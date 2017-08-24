package com.sshtools.rfb.encoding;

import com.sshtools.rfbcommon.RFBConstants;

public class CompressLevel8Encoding extends CompressLevel0Encoding {
	@Override
	public int getType() {
		return RFBConstants.ENC_COMPRESS_LEVEL0 + 8;
	}
}
