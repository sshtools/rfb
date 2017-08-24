package com.sshtools.rfbserver.encodings;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public class CursorPositionEncoding extends AbstractEncoding<Void> {
	final static Logger LOG = LoggerFactory.getLogger(CursorPositionEncoding.class);

	public CursorPositionEncoding() {
	}

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_POINTER_POS;
	}

	public boolean isPseudoEncoding() {
		return true;
	}

	public void encode(UpdateRectangle<Void> update, ProtocolWriter dout, PixelFormat pixelFormat, RFBClient client)
			throws IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Send cursor position " + update.getArea().x + "," + update.getArea().y);
		}
		dout.writeInt(getType().getCode());
	}

	public void selected(RFBClient client) {
	}
}
