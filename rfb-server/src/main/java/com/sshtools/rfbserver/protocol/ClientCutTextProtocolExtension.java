package com.sshtools.rfbserver.protocol;

import java.io.IOException;

import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbserver.RFBClient;

public class ClientCutTextProtocolExtension implements ProtocolExtension {

    public boolean handle(int msg, RFBClient rfbClient) throws IOException {
        ProtocolReader din = rfbClient.getInput();
        din.read();
        din.read();
        din.read();
        rfbClient.getDisplayDriver().setClipboardText(din.readASCII());
        return true;
    }

}
