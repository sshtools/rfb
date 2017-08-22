package com.sshtools.rfb.encoding;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfbcommon.RFBConstants;

public class TightPNGEncoding extends TightEncoding {
	final static Logger LOG = LoggerFactory.getLogger(ProtocolEngine.class);

	public TightPNGEncoding() {
	}

	@Override
	public int getType() {
		return RFBConstants.ENC_TIGHT_PNG;
	}

	@Override
	public String getName() {
		return "Tight PNG";
	}

	@Override
	public boolean isPseudoEncoding() {
		return false;
	}

	protected void doTight(int x, int y, int width, int height, int op) throws IOException {
		int type = op >> 4 & 0x0F;
		if (type == OP_PNG) {
			doGenericImage(x, y);
		} else {
			super.doTight(x, type, width, height, op);
		}
	}
}
