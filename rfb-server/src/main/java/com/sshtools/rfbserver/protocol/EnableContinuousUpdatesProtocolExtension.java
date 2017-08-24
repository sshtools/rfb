package com.sshtools.rfbserver.protocol;

import java.awt.Rectangle;
import java.io.IOException;

import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbserver.RFBClient;

public class EnableContinuousUpdatesProtocolExtension implements ProtocolExtension {
	public boolean handle(int msg, RFBClient rfbClient) throws IOException {
		ProtocolReader din = rfbClient.getInput();
		boolean enable = din.read() > 0;
		int x = din.readUnsignedShort();
		int y = din.readUnsignedShort();
		int w = din.readUnsignedShort();
		int h = din.readUnsignedShort();
		rfbClient.setContinuousUpdates(enable);
		rfbClient.setRequestedArea(new Rectangle(x, y, w, h));
		return false;
	}
}
