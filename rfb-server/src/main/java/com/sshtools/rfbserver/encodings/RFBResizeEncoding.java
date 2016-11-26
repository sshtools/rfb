package com.sshtools.rfbserver.encodings;

import java.io.DataOutputStream;
import java.io.IOException;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public class RFBResizeEncoding extends AbstractEncoding {

	public RFBResizeEncoding() {
	}

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_NEW_FB_SIZE;
	}

	public boolean isPseudoEncoding() {
		return true;
	}

	public void encode(UpdateRectangle<?> update, DataOutputStream dout, PixelFormat pixelFormat, RFBClient client) throws IOException {
		dout.writeInt(getType().getCode());
	}

	public void selected(RFBClient client) {
		
	}

}
