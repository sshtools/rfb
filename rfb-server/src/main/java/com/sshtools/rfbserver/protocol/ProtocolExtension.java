package com.sshtools.rfbserver.protocol;

import java.io.IOException;

import com.sshtools.rfbserver.RFBClient;

public interface ProtocolExtension {

    boolean handle(int msg, RFBClient rfbClient) throws IOException;

}
