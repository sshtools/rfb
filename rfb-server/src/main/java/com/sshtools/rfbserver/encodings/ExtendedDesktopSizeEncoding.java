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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.ScreenData;
import com.sshtools.rfbcommon.ScreenDetail;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public class ExtendedDesktopSizeEncoding extends AbstractEncoding<ScreenData> {
	final static Logger LOG = LoggerFactory.getLogger(ExtendedDesktopSizeEncoding.class);

	public ExtendedDesktopSizeEncoding() {
	}

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_EXTENDED_FB_SIZE;
	}

	public boolean isPseudoEncoding() {
		return true;
	}

	public void encode(UpdateRectangle<ScreenData> update, ProtocolWriter dout, PixelFormat pixelFormat, RFBClient client)
			throws IOException {
		ScreenData data = update.getData();
		dout.writeUInt32(getType().getCode());
		dout.writeByte(data.getDetails().size());
		dout.write(new byte[3]);
		for (ScreenDetail d : data.getDetails()) {
			dout.writeUInt32(d.getId());
			dout.writeShort((short) d.getX());
			dout.writeShort((short) d.getY());
			dout.writeShort((short) d.getDimension().getWidth());
			dout.writeShort((short) d.getDimension().getHeight());
			dout.writeUInt32(d.getFlags());
		}
		dout.flush();
	}

	@Override
	public void selected(RFBClient client) {
	}
}
