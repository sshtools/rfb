package com.sshtools.rfbserver;

import java.io.DataOutputStream;
import java.io.IOException;

import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbserver.protocol.Reply;

public class Beep extends Reply<Void> {

    public Beep() {
        super(RFBConstants.SMSG_BELL);
    }

    @Override
    public void write(DataOutputStream dout) throws IOException {
    }

}
