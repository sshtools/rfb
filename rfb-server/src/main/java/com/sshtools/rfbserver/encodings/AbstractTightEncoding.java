/**
 * RFB Server - Remote Frame Buffer (VNC Server) implementation. This is the base module if you want to create a VNC server. It takes a layered driver approach to add native specific features (which is recommened as the cross-platform default "Robot" driver is very slow).
 *
 * See the vncserver module for a concrete server implementation that has some native performance improvements for some platforms.
 * Copyright © 2006 SSHTOOLS Limited (support@sshtools.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.sshtools.rfbserver.encodings;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.PaletteAnalyser;
import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.TightConstants;
import com.sshtools.rfbcommon.TightUtil;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public abstract class AbstractTightEncoding extends AbstractZLIBEncoding implements TightConstants {
	final static Logger LOG = LoggerFactory.getLogger(AbstractTightEncoding.class);
	protected int tightLevel = -1;
	protected PaletteAnalyser pan;
	public final static int JPEG_THRESHOLD = 256 * 256;

	public AbstractTightEncoding() {
		pan = new PaletteAnalyser(256, JPEG_THRESHOLD);
	}

	public boolean isPseudoEncoding() {
		return false;
	}

	public void encode(UpdateRectangle<BufferedImage> update, ProtocolWriter dout, PixelFormat pixelFormat, RFBClient client)
			throws IOException {
		BufferedImage img = update.getData();
		dout.writeUInt32(getType().getCode());
		// https://wiki.qemu.org/Features/VNC_Tight_PNG
		// https://vncdotool.readthedocs.io/en/0.8.0/rfbproto.html#tight-encoding
		int area = (int) update.getArea().getWidth() * (int) update.getArea().getHeight();
		if (area > JPEG_THRESHOLD && tightLevel > -1) {
			if(LOG.isDebugEnabled())
				LOG.debug(String.format("Sending JPEG of %s", update.getArea()));
			ByteArrayOutputStream bout = new ByteArrayOutputStream(
					(int) update.getArea().getWidth() * (int) update.getArea().getHeight());
			encodeImage(img, bout, dout);
			byte[] data = bout.toByteArray();
			dout.writeCompactLen(data.length);
			dout.write(data);
			dout.flush();
		} else {
			pan.reset();
			int tileWidth = (int) update.getArea().getWidth();
			int tileHeight = (int) update.getArea().getHeight();
			int[] tileBuf = new int[tileWidth * tileHeight];
			img.getRGB(0, 0, tileWidth, tileHeight, tileBuf, 0, tileWidth);
			pan.analyse(tileBuf, tileWidth * tileHeight);
			if (pan.getPalette().length == 1) {
				/* Fill */
				if(LOG.isDebugEnabled())
					LOG.debug(String.format("Sending FILL of %d", pan.getPalette()[0]));
				dout.writeByte(OP_FILL << 4);
				TightUtil.writeTightColor(pan.getPalette()[0], pixelFormat, dout);
			} else {
				writeTightBasic(update, dout, pixelFormat, dout, tileBuf);
			}
		}
	}

	protected abstract void writeTightBasic(UpdateRectangle<?> update, ProtocolWriter dout, PixelFormat pixelFormat,
			ProtocolWriter writer, int[] tileBuf) throws IOException;

	protected abstract void encodeImage(BufferedImage img, ByteArrayOutputStream bout, ProtocolWriter dout) throws IOException;

	public void setTightLevel(int level) {
		this.tightLevel = level;
	}
}
