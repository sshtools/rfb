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

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.UpdateRectangle;

public class TightPNGEncoding extends AbstractTightEncoding {
	final static Logger LOG = LoggerFactory.getLogger(TightPNGEncoding.class);

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_TIGHT_PNG;
	}

	@Override
	protected void writeTightBasic(UpdateRectangle<?> update, ProtocolWriter dout, PixelFormat pixelFormat, ProtocolWriter writer,
			int[] tileBuf) throws IOException {
		dout.writeByte(OP_PNG << 4);
		ByteArrayOutputStream bout = new ByteArrayOutputStream(
				(int) update.getArea().getWidth() * (int) update.getArea().getHeight());
		ImageIO.write((BufferedImage) update.getData(), "PNG", bout);
		byte[] data = bout.toByteArray();
		dout.writeCompactLen(data.length);
		dout.write(data);
	}

	@Override
	protected void encodeImage(BufferedImage img, ByteArrayOutputStream bout, ProtocolWriter dout) throws IOException {
		if (tightLevel == -1) {
			dout.writeByte(OP_PNG << 4);
			if (!ImageIO.write(img, "PNG", bout))
				throw new IOException("Cannot encode as PNG");
		} else {
			dout.writeByte(OP_JPEG << 4);
			if (!ImageIO.write(img, "JPEG", bout))
				throw new IOException("Cannot encode as JPEG");
		}
	}
}
