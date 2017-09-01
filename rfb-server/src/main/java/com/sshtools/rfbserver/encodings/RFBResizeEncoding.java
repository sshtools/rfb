package com.sshtools.rfbserver.encodings;

import java.io.IOException;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public class RFBResizeEncoding extends AbstractEncoding<Void> {
	public RFBResizeEncoding() {
	}

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_NEW_FB_SIZE;
	}

	public boolean isPseudoEncoding() {
		return true;
	}

	public void encode(UpdateRectangle<Void> update, ProtocolWriter dout, PixelFormat pixelFormat, RFBClient client)
			throws IOException {
		dout.writeUInt32(getType().getCode());
	}

	public void selected(RFBClient client) {
	}
}
