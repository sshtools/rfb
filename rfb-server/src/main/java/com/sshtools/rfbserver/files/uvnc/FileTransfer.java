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
import com.sshtools.rfbserver.protocol.Reply;

public abstract class FileTransfer<T> extends Reply<T> {

    protected int type;
    protected int contentParam;

    public FileTransfer(int type, int contentParam) {
        super(RFBConstants.SMSG_FILE_TRANSFER);
        this.type = type;
        this.contentParam = contentParam;
    }

    @Override
    public final void write(ProtocolWriter dout) throws IOException {
        dout.write(type);
        dout.write(contentParam & 0xff);
        dout.write(contentParam >> 8);
        onWrite(dout);
    }
    
    protected void populate(FileTransfer<T> t) {
        t.type = type;
        t.contentParam = contentParam;
        super.populate(t);
    }

    @Override
    public String toString() {
        return "FileTransfer [type=" + type + ", contentParam=" + contentParam + ", data=" + data + ", code=" + code + "]";
    }

    protected abstract void onWrite(ProtocolWriter dout) throws IOException;

}