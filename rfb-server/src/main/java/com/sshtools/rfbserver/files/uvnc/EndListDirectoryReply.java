package com.sshtools.rfbserver.files.uvnc;

import java.io.DataOutputStream;
import java.io.IOException;

import com.sshtools.rfbcommon.RFBConstants;

public class EndListDirectoryReply extends FileTransfer<Integer> {

    public EndListDirectoryReply(int size) {
        super(RFBConstants.RFB_DIR_DRIVE_LIST, RFBConstants.RFB_RECV_NONE);
        setData(size);
    }

    @Override
    protected void onWrite(DataOutputStream dout) throws IOException {
        dout.writeInt(0);
        dout.writeInt(data);
    }

//    @Override
//    public EndListDirectoryReply copy() {
//        EndListDirectoryReply d = new EndListDirectoryReply(data);
//        populate(d);
//        return d;
//    }

}
