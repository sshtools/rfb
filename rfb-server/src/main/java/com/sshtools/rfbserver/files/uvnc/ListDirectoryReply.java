package com.sshtools.rfbserver.files.uvnc;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.RFBFile;
import com.sshtools.rfbserver.protocol.Reply;

public class ListDirectoryReply extends FileTransfer<RFBFile> {

    public ListDirectoryReply(RFBFile file) {
        super(RFBConstants.RFB_DIR_DRIVE_LIST, RFBConstants.RFB_RECV_DIRECTORY);
        setData(file);
    }

    @Override
    protected void onWrite(DataOutputStream dout) throws IOException {
        dout.writeInt(0); // Dunno
        
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
