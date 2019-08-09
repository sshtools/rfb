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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.ImageUtil;
import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.PixelFormatImageFactory;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.DisplayDriver.PointerShape;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public class CursorEncoding extends AbstractRawEncoding<PointerShape> {
	final static Logger LOG = LoggerFactory.getLogger(CursorEncoding.class);

	public CursorEncoding() {
	}

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_RICH_CURSOR;
	}

	public boolean isPseudoEncoding() {
		return true;
	}

	public void encode(UpdateRectangle<PointerShape> update, ProtocolWriter dout, PixelFormat pixelFormat, RFBClient client)
			throws IOException {
		if(LOG.isDebugEnabled())
			LOG.debug("Sending default cursor shape update in " + pixelFormat);
		PointerShape pc = (PointerShape) update.getData();
		BufferedImage img = pc.getData();
		int height = update.getArea().height;
		int width = update.getArea().width;
		/*
		 * Get the cursor mask. This is obtained from the original cursor image,
		 * not the formatted one
		 */
		int bytesPerRow = (width + 7) / 8;
		int bytesMaskData = bytesPerRow * height;
		ByteBuffer maskBuf = ByteBuffer.allocate(bytesMaskData);
		int bufSize = bytesPerRow * 8;
		BitSet maskRow = new BitSet(bufSize);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++)
				maskRow.set(x, (img.getRGB(width - x - 1, y) & 0xff000000) != 0);
			maskBuf.put(ImageUtil.toByteArray(maskRow, bytesPerRow));
		}
		byte[] array2 = maskBuf.array();
		/*
		 * The cursor will not be in the correct format, so create a new image
		 * in the required format and extract the raw image data to send
		 */
		BufferedImage compatImg = new PixelFormatImageFactory(pixelFormat).create(img.getWidth(), img.getHeight());
		compatImg.getGraphics().drawImage(img, 0, 0, null);
		UpdateRectangle<BufferedImage> u = new UpdateRectangle<BufferedImage>(update.getDriver(),
				new Rectangle(0, 0, width, height), getType().getCode());
		u.setData(compatImg);
		byte[] pixelData = prepareEncode(u, pixelFormat);
		/* Write out update */
		dout.writeUInt32(getType().getCode());
		dout.write(pixelData);
		// And now write the mast
		dout.write(array2);
	}

	@Override
	public void selected(RFBClient client) {
	}
}
