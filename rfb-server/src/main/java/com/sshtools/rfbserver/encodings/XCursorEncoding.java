package com.sshtools.rfbserver.encodings;

import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.ImageUtil;
import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.DisplayDriver.PointerShape;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public class XCursorEncoding extends AbstractEncoding {
	final static Logger LOG = LoggerFactory.getLogger(XCursorEncoding.class);
	

	public final static byte[] FOREGROUND = { (byte) 0xff, (byte) 0xff, (byte) 0xff };
	public final static byte[] BACKGROUND = { (byte) 0x00, (byte) 0x00, (byte) 0x00 };

	public XCursorEncoding() {
	}

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_X11_CURSOR;
	}

	public boolean isPseudoEncoding() {
		return true;
	}

	public void encode(UpdateRectangle<?> update, DataOutputStream dout, PixelFormat pixelFormat, RFBClient client)
			throws IOException {
		LOG.info("Sending X cursor shape update");
		
		PointerShape pc = (PointerShape) update.getData();
		BufferedImage img = ImageUtil.ensureType(pc.getData(), client.getPixelFormat().getImageType());

		int height = update.getArea().height;
		int width = update.getArea().width;

		int bytesPerRow = (width + 7) / 8;
		int bytesMaskData = bytesPerRow * height;
		ByteBuffer pixBuf = ByteBuffer.allocate(bytesMaskData);
		ByteBuffer maskBuf = ByteBuffer.allocate(bytesMaskData);

		int minRed = 255;
		int minGreen = 255;
		int minBlue = 255;
		int maxRed = 255;
		int maxGreen = 255;
		int maxBlue = 255;
		int totRed = 0;
		int totGreen = 0;
		int totBlue = 0;

		int bufSize = bytesPerRow * 8;
		BitSet pixRow = new BitSet(bufSize);
		BitSet maskRow = new BitSet(bufSize);

		int[] pix = new int[4];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				img.getRaster().getPixel(width - x - 1, y, pix);
				totRed += pix[3];
				totGreen += pix[2];
				totBlue += pix[1];
				minRed = Math.min(minRed, pix[3]);
				minGreen = Math.min(minGreen, pix[2]);
				minBlue = Math.min(minBlue, pix[1]);
				maxRed = Math.max(maxRed, pix[3]);
				maxGreen = Math.max(maxGreen, pix[2]);
				maxBlue = Math.max(maxBlue, pix[1]);
				maskRow.set(x, pix[0] >= 1);
				pixRow.set(x, pix[1] > 128 | pix[2] > 128 | pix[3] > 128);
			}
			pixBuf.put(ImageUtil.toByteArray(pixRow, bytesPerRow));
			maskBuf.put(ImageUtil.toByteArray(maskRow, bytesPerRow));
		}

		int pixels = width * height;
		int avgRed = totRed / pixels;
		int avgGreen = totGreen / pixels;
		int avgBlue = totBlue / pixels;

		dout.writeInt(getType().getCode());
		dout.write(avgRed);
		dout.write(avgGreen);
		dout.write(avgBlue);
		dout.write(maxRed);
		dout.write(maxGreen);
		dout.write(maxBlue);
		byte[] array = pixBuf.array();
		dout.write(array);
		byte[] array2 = maskBuf.array();
		dout.write(array2);
	}

	public void selected(RFBClient client) {
	}

}
