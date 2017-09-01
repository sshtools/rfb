package com.sshtools.rfbserver;

import java.io.IOException;

import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbserver.protocol.Reply;

public class ServerCut extends Reply<Void> {

    private String text;

    public ServerCut(String text) {
        super(RFBConstants.SMSG_SERVER_CUT_TEXT);
        this.text = text;
    }

    @Override
    public void write(ProtocolWriter dout) throws IOException {
        dout.write(new byte[3]);
        dout.writeUInt32(text.length());
        dout.write(text.getBytes());
    }

}
