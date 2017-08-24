package com.sshtools.rfb.encoding;

import com.sshtools.rfbcommon.RFBConstants;

public class JPEGQuality3Encoding extends JPEGQuality0Encoding {
	@Override
	public int getType() {
		return RFBConstants.ENC_JPEG_QUALITY_LEVEL0 + 3;
	}
}
