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
import java.io.IOException;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public class ZLIBEncoding extends AbstractZLIBEncoding {
	/**
	 * Minimum ZLIB update size (smaller than this there is no point in
	 * compressing due to compression overhead)
	 */
	private final static int VNC_ENCODE_ZLIB_MIN_COMP_SIZE = 17;

	public ZLIBEncoding() {
	}

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_ZLIB;
	}

	public synchronized void encode(UpdateRectangle<BufferedImage> update, ProtocolWriter dout, PixelFormat pixelFormat,
			RFBClient client) throws IOException {
		int width = update.getArea().width;
		int height = update.getArea().height;
		int bytesPerPixel = pixelFormat.getBitsPerPixel() / 8;
		if (width * height * bytesPerPixel < VNC_ENCODE_ZLIB_MIN_COMP_SIZE) {
			rawEncode(update, dout, pixelFormat);
			return;
		}
		super.encode(update, dout, pixelFormat, client);
	}

	public String getName() {
		return "ZLIB";
	}
}
