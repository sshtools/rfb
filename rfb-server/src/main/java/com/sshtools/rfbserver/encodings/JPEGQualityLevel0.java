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

public class JPEGQualityLevel0 implements RFBServerEncoding<Void> {
	final static Logger LOG = LoggerFactory.getLogger(JPEGQualityLevel0.class);

	public int getCode() {
		return getType().getCode() + 0;
	}

	public boolean isPseudoEncoding() {
		return true;
	}

	public void selected(RFBClient client) {
		setForEncoding(client, RFBConstants.ENC_TIGHT);
		setForEncoding(client, RFBConstants.ENC_TIGHT_PNG);
	}

	private void setForEncoding(RFBClient client, int enc) {
		AbstractTightEncoding enabledEncoding = (AbstractTightEncoding) client.getEncoder().getEnabledEncoding(enc);
		if (enabledEncoding != null) {
			if (client.getPixelFormat().getColorDepth() >= 16) {
				int level = getLevel();
				LOG.info("Setting JPEG quality level to " + level);
				enabledEncoding.setTightLevel(level);
			} else {
				LOG.warn(String.format("Cannot set compression level with color depth < 16 for ",
						enabledEncoding.getType().getSignature()));
			}
		}
	}

	private int getLevel() {
		int level = getCode() - RFBConstants.ENC_JPEG_QUALITY_LEVEL0;
		return level;
	}

	public void encode(UpdateRectangle<Void> update, ProtocolWriter dout, PixelFormat pixelFormat, RFBClient client)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_JPEG_QUALITY;
	}
}
