package com.sshtools.rfbserver.protocol;

import java.io.IOException;

import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbserver.RFBClient;

public class PointerEventProtocolExtension implements ProtocolExtension {

    public boolean handle(int msg, RFBClient rfbClient) throws IOException {
        ProtocolReader din = rfbClient.getInput();
        int buttonMask = din.read();
        int x = din.readUnsignedShort();
        int y = din.readUnsignedShort();
        rfbClient.getDisplayDriver().mouseEvent(rfbClient, buttonMask, x, y);
        return false;
    }

}
