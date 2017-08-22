package com.sshtools.rfbcommon;

import java.io.DataOutput;
import java.io.IOException;

public class TightUtil {
	public static int getTightPixSize(PixelFormat rfbModel) {
		return (rfbModel.getColorDepth() == 24 && rfbModel.getBitsPerPixel() == 32) ? 3 : rfbModel.getBytesPerPixel();
	}

	public static boolean isTightNative(PixelFormat rfbModel) {
		return rfbModel.getBytesPerPixel() == 4 && getTightPixSize(rfbModel) == 3 && rfbModel.getRedMax() == 0xff
				&& rfbModel.getGreenMax() == 0xff && rfbModel.getBlueMax() == 0xff;
	}

	public static void writeTightColor(int pixel, PixelFormat rfbModel, DataOutput dout) throws IOException {
		if (isTightNative(rfbModel)) {
			dout.writeByte(pixel >> 16 & 0xff);
			dout.writeByte(pixel >> 8 & 0xff);
			dout.writeByte(pixel & 0xff);
		} else {
			dout.write(ImageUtil.translateAndEncodePixel(rfbModel, pixel));
		}
	}
}
