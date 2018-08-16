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
package com.sshtools.rfbserver.protocol;

import java.io.IOException;

import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbcommon.ScreenData;
import com.sshtools.rfbcommon.ScreenDetail;
import com.sshtools.rfbcommon.ScreenDimension;
import com.sshtools.rfbserver.RFBClient;

public class SetDesktopSizeExtension implements ProtocolExtension {

	public boolean handle(int msg, RFBClient rfbClient) throws IOException {
		ProtocolReader din = rfbClient.getInput();
		ScreenData screenData = new ScreenData(new ScreenDimension(din.readUnsignedShort(), din.readUnsignedShort()));
		int noScreens = din.readUnsignedByte();
		din.read();
		for (int i = 0; i < noScreens; i++) 
			screenData.getDetails()
					.add(new ScreenDetail(din.readUInt32(), din.readUnsignedShort(), din.readUnsignedShort(),
							new ScreenDimension(din.readUnsignedShort(), din.readUnsignedShort()), din.readUInt32()));
		rfbClient.getDisplayDriver().resize(screenData);
		return true;
	}

}
