package com.sshtools.rfbserver.encodings;

import java.io.IOException;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public interface RFBServerEncoding<D> {
	void selected(RFBClient client);
	
	int getCode();
	
	TightCapability getType();

	boolean isPseudoEncoding();

	void encode(UpdateRectangle<D> update, ProtocolWriter dout, PixelFormat pixelFormat, RFBClient client) throws IOException;

}
