package com.sshtools.rfbserver.protocol;

import java.io.IOException;

import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbcommon.ScreenData;
import com.sshtools.rfbcommon.ScreenDetail;
import com.sshtools.rfbcommon.ScreenDimension;
import com.sshtools.rfbserver.RFBClient;

public class SetDesktopSizeExtension implements ProtocolExtension {

	public boolean handle(int msg, RFBClient rfbClient) throws IOException {
		ProtocolReader din = rfbClient.getInput();
		ScreenData screenData = new ScreenData(new ScreenDimension(din.readUnsignedShort(), din.readUnsignedShort()));
		int noScreens = din.readUnsignedByte();
		din.read();
		for (int i = 0; i < noScreens; i++) {
			screenData.getDetails()
					.add(new ScreenDetail(din.readUInt32(), din.readUnsignedShort(), din.readUnsignedShort(),
							new ScreenDimension(din.readUnsignedShort(), din.readUnsignedShort()), din.readUInt32()));
		}
		rfbClient.getDisplayDriver().resize(screenData);
		return false;
	}

}
