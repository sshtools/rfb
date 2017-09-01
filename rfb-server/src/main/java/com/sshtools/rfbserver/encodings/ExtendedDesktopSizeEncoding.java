package com.sshtools.rfbserver.encodings;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.ScreenData;
import com.sshtools.rfbcommon.ScreenDetail;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public class ExtendedDesktopSizeEncoding extends AbstractEncoding<ScreenData> {
	final static Logger LOG = LoggerFactory.getLogger(ExtendedDesktopSizeEncoding.class);

	public ExtendedDesktopSizeEncoding() {
	}

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_EXTENDED_FB_SIZE;
	}

	public boolean isPseudoEncoding() {
		return true;
	}

	public void encode(UpdateRectangle<ScreenData> update, ProtocolWriter dout, PixelFormat pixelFormat, RFBClient client)
			throws IOException {
		ScreenData data = update.getData();
		dout.writeUInt32(getType().getCode());
		dout.writeByte(data.getDetails().size());
		dout.write(new byte[3]);
		for (ScreenDetail d : data.getDetails()) {
			dout.writeUInt32(d.getId());
			dout.writeShort((short) d.getX());
			dout.writeShort((short) d.getY());
			dout.writeShort((short) d.getDimension().getWidth());
			dout.writeShort((short) d.getDimension().getHeight());
			dout.writeUInt32(d.getFlags());
		}
		dout.flush();
	}

	@Override
	public void selected(RFBClient client) {
	}
}
