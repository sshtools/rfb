package com.sshtools.rfbserver.encodings;

import java.io.DataOutputStream;
import java.io.IOException;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;

public class RREEncoding extends AbstractRREEncoding {

    public RREEncoding() {
		super();
	}

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_RRE;
	}

	public boolean isPseudoEncoding() {
		return false;
	}

	@Override
	protected void writeSubrect(DataOutputStream dout, PixelFormat pixelFormat, SubRect s) throws IOException {
		writePixel(dout, pixelFormat, s.pixel);
		dout.writeShort(s.x);
		dout.writeShort(s.y);
		dout.writeShort(s.w);
		dout.writeShort(s.h);
	}

}
