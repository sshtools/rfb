package com.sshtools.rfbserver.files.uvnc;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.rfbcommon.RFBConstants;

public class ListDrivesReply extends FileTransfer<List<RFBDrive>> {

    public ListDrivesReply() {
        super(RFBConstants.RFB_DIR_DRIVE_LIST, RFBConstants.RFB_RECV_DRIVE_LIST);
    }

    @Override
    protected void onWrite(DataOutputStream dout) throws IOException {
        StringBuilder bui = new StringBuilder();
        dout.writeInt(0);
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
