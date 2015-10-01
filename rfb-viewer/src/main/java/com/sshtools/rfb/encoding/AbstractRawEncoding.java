package com.sshtools.rfb.encoding;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBDisplayModel;
import com.sshtools.rfb.RFBEncoding;

public abstract class AbstractRawEncoding implements RFBEncoding {

	public AbstractRawEncoding() {
	}

	protected int doProcessRaw(RFBDisplay display, int x, int y, int width,
			int height, byte[] buf) {
		return doProcessRaw(display, x, y, width, height, buf, 0);
	}

	protected int doProcessRaw(RFBDisplay display, int x, int y, int width,
			int height, byte[] buf, int offset) {
		RFBDisplayModel model = display.getDisplayModel();
		// Create a smaller compatible image to draw the rectangle on. This
		// might be used again last for 'last rect'
		BufferedImage bim = model.getFactory().create(width, height);
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
			BufferedImage bim, int offset) {
		DataBuffer dataBuffer = bim.getRaster().getDataBuffer();
		if (dataBuffer instanceof DataBufferInt) {
			return prepareDecode(buf, ((DataBufferInt) dataBuffer).getData(),
					model, offset);
		} else if (dataBuffer instanceof DataBufferUShort) {
			return prepareDecode(buf,
					((DataBufferUShort) dataBuffer).getData(), model, offset);
		} else if (dataBuffer instanceof DataBufferByte) {
			return prepareDecode(buf, ((DataBufferByte) dataBuffer).getData(),
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
