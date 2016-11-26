package com.sshtools.rfbserver.files.uvnc;

import java.io.DataOutputStream;
import java.io.IOException;

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
    public final void write(DataOutputStream dout) throws IOException {
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

    protected abstract void onWrite(DataOutputStream dout) throws IOException;

}