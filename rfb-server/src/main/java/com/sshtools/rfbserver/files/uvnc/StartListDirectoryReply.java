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
package com.sshtools.rfbserver.files.uvnc;

import java.io.IOException;

import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;

public class StartListDirectoryReply extends FileTransfer<String> {
	public StartListDirectoryReply(String path) {
		super(RFBConstants.RFB_DIR_DRIVE_LIST, RFBConstants.RFB_RECV_DIRECTORY);
		setData(path);
	}

	@Override
	protected void onWrite(ProtocolWriter dout) throws IOException {
		dout.writeUInt32(0);
		if (getData() == null || getData().length() == 0) {
			dout.writeUInt32(0);
		} else {
			byte[] buf = (getData() + "\0").getBytes();
			dout.writeUInt32(buf.length);
			dout.write(buf);
		}
	}
	// @Override
	// public Reply<String> copy() {
	// StartListDirectoryReply d = new StartListDirectoryReply(data);
	// populate(d);
	// return d;
	// }
}
