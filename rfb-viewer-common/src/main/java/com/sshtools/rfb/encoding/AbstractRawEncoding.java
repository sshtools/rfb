/**
 * RFB - Remote Frame Buffer (VNC) implementation.
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
package com.sshtools.rfb.encoding;

import java.awt.Color;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBDisplayModel;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfb.RFBToolkit;
import com.sshtools.rfb.RFBToolkit.RFBImage;
import com.sshtools.rfbcommon.ImageUtil;
import com.sshtools.rfbcommon.PixelFormat;

public abstract class AbstractRawEncoding implements RFBEncoding {

	public AbstractRawEncoding() {
	}

	public static Color decodeAndUntranslatePixelToColour(byte[] b, int off,
			PixelFormat model) throws IOException {
		return new Color(ImageUtil.decodeAndUntranslatePixel(b, off, model));
	}

	protected int doProcessRaw(RFBDisplay<?,?> display, int x, int y, int width,
			int height, byte[] buf) {
		return doProcessRaw(display, x, y, width, height, buf, 0);
	}

	protected int doProcessRaw(RFBDisplay<?,?> display, int x, int y, int width,
			int height, byte[] buf, int offset) {
		RFBDisplayModel model = display.getDisplayModel();
		// Create a smaller compatible image to draw the rectangle on. This
		// might be used again last for 'last rect'
		RFBImage bim = RFBToolkit.get().createImage(model, width, height);
		try {
			synchronized (model.getLock()) {
				return decodeIntoImage(buf, model, bim, offset);
			}
		} catch (BufferUnderflowException bufe) {
			int expected = width * height * display.getDisplayModel().getBytesPerPixel();
			throw new RuntimeException(
					"Server didn't supply enough data for a raw block. Size is "
							+ width + "x" + height + ", position is " + x + ","
							+ y + ". The data buffer is " + buf.length + " bytes in length starting " + 
							"at the offset of " + offset + " and the expected length is " + expected + ". This would make a final index of " + ( expected + offset ), bufe);
		} finally {
			model.drawRectangle(x, y, width, height, bim);
		}
	}

	protected int decodeIntoImage(byte[] buf, RFBDisplayModel model,
			RFBImage bim, int offset) {
		Object dataBuffer = bim.getData();
		if (dataBuffer instanceof int[]) {
			return prepareDecode(buf, (int[])dataBuffer,
					model, offset);
		} else if (dataBuffer instanceof short[]) {
			return prepareDecode(buf,
					(short[])dataBuffer, model, offset);
		} else if (dataBuffer instanceof byte[]) {
			return prepareDecode(buf, (byte[])dataBuffer,
					model, offset);
		} else {
			throw new UnsupportedOperationException("Unknown data buffer type.");
		}
	}

	private int prepareDecode(byte[] buf, int[] data, RFBDisplayModel model,
			int offset) {
		if (model.getBitsPerPixel() == 24) {
			int j = offset;
			for (int i = 0; i < data.length; i++) {
				data[i] = ((buf[j++] << 16) & 0xff0000)
						| ((buf[j++] << 8) & 0x00ff00) | (buf[j++] & 0x0000ff);
			}
		} else {
			ByteBuffer bb = ByteBuffer.wrap(buf);
			bb.rewind();
			bb.position(offset);
			bb.order(model.isBigEndian() ? ByteOrder.BIG_ENDIAN
					: ByteOrder.LITTLE_ENDIAN);
			for (int i = 0; i < data.length; i++) {
				data[i] = bb.getInt();
			}
		}
		return data.length * 4;
	}

	private int prepareDecode(byte[] buf, short[] data, RFBDisplayModel model,
			int offset) {
		ByteBuffer bb = ByteBuffer.wrap(buf);
		bb.rewind();
		bb.position(offset);
		bb.order(model.isBigEndian() ? ByteOrder.BIG_ENDIAN
				: ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < data.length; i++) {
			data[i] = bb.getShort();
		}

		return data.length * 2;
	}

	private int prepareDecode(byte[] buf, byte[] data, RFBDisplayModel model,
			int offset) {
		// In the case of Zlib data, buf may be longer than is needed, so we
		// copy using data.length
		System.arraycopy(buf, offset, data, 0, data.length);
		return data.length;
	}
}
