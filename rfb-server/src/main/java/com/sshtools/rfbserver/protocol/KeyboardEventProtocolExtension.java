package com.sshtools.rfbserver.protocol;

import java.io.IOException;

import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbserver.RFBClient;

public class KeyboardEventProtocolExtension implements ProtocolExtension {

    public boolean handle(int msg, RFBClient rfbClient) throws IOException {
        ProtocolReader din = rfbClient.getInput();
        boolean down = din.read() > 0;
        din.readUnsignedShort(); // padding
        rfbClient.getDisplayDriver().keyEvent(rfbClient, down, din.readInt());
        return true;
    }

}
