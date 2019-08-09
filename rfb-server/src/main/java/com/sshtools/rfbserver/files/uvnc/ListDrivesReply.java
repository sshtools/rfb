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
import java.util.ArrayList;
import java.util.List;

import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;

public class ListDrivesReply extends FileTransfer<List<RFBDrive>> {

    public ListDrivesReply() {
        super(RFBConstants.RFB_DIR_DRIVE_LIST, RFBConstants.RFB_RECV_DRIVE_LIST);
    }

    @Override
    protected void onWrite(ProtocolWriter dout) throws IOException {
        StringBuilder bui = new StringBuilder();
        dout.writeUInt32(0);
        for(RFBDrive d : getData()) {
            bui.append(String.format("%-2s", d.getName()));
            bui.append(d.toCode());
            bui.append('\0');
        }
        byte[] bytes = bui.toString().getBytes("UTF-8");
        dout.writeInt(bytes.length);
        dout.write(bytes);
    }

//    @Override
//    public Reply<List<RFBDrive>> copy() {
//        ListDrivesReply d = new ListDrivesReply();
//        populate(d);
//        return d;
//    }

    protected void populate(ListDrivesReply r) {
        super.populate(r);
        r.data = new ArrayList<RFBDrive>(getData());
    }

}
