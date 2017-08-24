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

public class CompressLevel0 implements RFBServerEncoding<Void> {
	final static Logger LOG = LoggerFactory.getLogger(CompressLevel0.class);

	public int getCode() {
		return getType().getCode() + 0;
	}

	public boolean isPseudoEncoding() {
		return true;
	}

	public void selected(RFBClient client) {
		RFBServerEncoding<?> enabledEncoding = client.getEncoder().getEnabledEncoding(RFBConstants.ENC_ZLIB);
		ZLIBEncoding enc = (ZLIBEncoding) enabledEncoding;
		if (enc != null) {
			int level = getLevel();
			LOG.info("Setting compress level to " + level);
			enc.setZLibCompressLevel(level);
		}
	}

	private int getLevel() {
		int level = getCode() - RFBConstants.ENC_COMPRESS_LEVEL0;
		return level;
	}

	public void encode(UpdateRectangle<Void> update, ProtocolWriter dout, PixelFormat pixelFormat, RFBClient client)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_COMPRESS;
	}
}
