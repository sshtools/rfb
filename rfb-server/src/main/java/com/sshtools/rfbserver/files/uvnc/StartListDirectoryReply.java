package com.sshtools.rfbserver.files.uvnc;

import java.io.DataOutputStream;
import java.io.IOException;

import com.sshtools.rfbcommon.RFBConstants;

public class StartListDirectoryReply extends FileTransfer<String> {

    public StartListDirectoryReply(String path) {
        super(RFBConstants.RFB_DIR_DRIVE_LIST, RFBConstants.RFB_RECV_DIRECTORY);
        setData(path);
    }

    @Override
    protected void onWrite(DataOutputStream dout) throws IOException {
        dout.writeInt(0);
        if (getData() == null || getData().length() == 0) {
            dout.writeInt(0);
        } else {
            byte[] buf = (getData() + "\0").getBytes();
            dout.writeInt(buf.length);
            dout.write(buf);
        }
    }

//    @Override
//    public Reply<String> copy() {
//        StartListDirectoryReply d = new StartListDirectoryReply(data);
//        populate(d);
//        return d;
//    }

}
