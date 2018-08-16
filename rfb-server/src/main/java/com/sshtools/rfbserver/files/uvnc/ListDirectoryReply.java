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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.RFBFile;

public class ListDirectoryReply extends FileTransfer<RFBFile> {

    public ListDirectoryReply(RFBFile file) {
        super(RFBConstants.RFB_DIR_DRIVE_LIST, RFBConstants.RFB_RECV_DIRECTORY);
        setData(file);
    }

    @Override
    protected void onWrite(ProtocolWriter dout) throws IOException {
        dout.writeUInt32(0); // Dunno
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        
        int sizeHigh = (int)( data.getSize() >> 32 );
        int sizeLow = (int)( data.getSize() & 0xffffffff );
        int fileAttributes = data.getFileAttributes();
        
        if(data.isFolder()) {
            fileAttributes = fileAttributes | 0x10000000;
        }
        
        dos.writeInt(fileAttributes);
        dos.writeLong(data.getCreationTime());
        dos.writeLong(data.getLastAccessTime());
        dos.writeLong(data.getLastWriteTime());
        dos.writeInt(sizeHigh);
        dos.writeInt(sizeLow);        
        dos.writeLong(0); //reserved
        dos.write((data.getName() + "\0").getBytes());
        dos.write((data.getAlternateName() + "\0").getBytes());
        dos.flush();
        
        byte[] out = baos.toByteArray();
        dout.writeInt(out.length);
        dout.write(out);
    }

//    @Override
//    public Reply<RFBFile> copy() {
//        ListDirectoryReply d = new ListDirectoryReply(data);
//        populate(d);
//        return d;
//    }

    protected void populate(ListDirectoryReply r) {
        super.populate(r);
    }

}
