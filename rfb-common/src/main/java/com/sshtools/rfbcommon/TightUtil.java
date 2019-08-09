/**
 * RFB Common - Remote Frame Buffer common code used both in client and server.
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
