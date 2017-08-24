package com.sshtools.rfb.encoding;

import com.sshtools.rfbcommon.RFBConstants;

public class JPEGQuality1Encoding extends JPEGQuality0Encoding {
	public JPEGQuality1Encoding() {
	}

	@Override
	public int getType() {
		return RFBConstants.ENC_JPEG_QUALITY_LEVEL0 + 1;
	}

}
