package com.sshtools.rfbserver.encodings;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.ImageUtil;
import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.DisplayDriver.PointerShape;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public class XCursorEncoding extends AbstractEncoding<PointerShape> {
	final static Logger LOG = LoggerFactory.getLogger(XCursorEncoding.class);

	public XCursorEncoding() {
	}

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_X11_CURSOR;
	}

	public boolean isPseudoEncoding() {
		return true;
	}

	public void encode(UpdateRectangle<PointerShape> update, ProtocolWriter dout, PixelFormat pixelFormat, RFBClient client)
			throws IOException {
		if(LOG.isDebugEnabled())
			LOG.debug("Sending X cursor shape update");
		PointerShape pc = update.getData();
		BufferedImage pointerImg = pc.getData();
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
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int rgb = pointerImg.getRGB(width - x - 1, y);
				int alpha = rgb >> 24 & 0xff;
				int red = rgb >> 16 & 0xff;
				int green = rgb >> 8 & 0xff;
				int blue = rgb & 0xff;
				totRed += red;
				totGreen += green;
				totBlue += blue;
				minRed = Math.min(minRed, red);
				minGreen = Math.min(minGreen, green);
				minBlue = Math.min(minBlue, blue);
				maxRed = Math.max(maxRed, red);
				maxGreen = Math.max(maxGreen, green);
				maxBlue = Math.max(maxBlue, blue);
				maskRow.set(x, alpha > 128);
				pixRow.set(x, !(red > 128 | green > 128 | blue > 128));
			}
			pixBuf.put(ImageUtil.toByteArray(pixRow, bytesPerRow));
			maskBuf.put(ImageUtil.toByteArray(maskRow, bytesPerRow));
		}
		int pixels = width * height;
		int avgRed = totRed / pixels;
		int avgGreen = totGreen / pixels;
		int avgBlue = totBlue / pixels;
		dout.writeUInt32(getType().getCode());
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
