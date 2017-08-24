package com.sshtools.rfb.encoding;

import java.io.IOException;

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfbcommon.RFBConstants;

public class CompressLevel0Encoding implements RFBEncoding {
	public CompressLevel0Encoding() {
	}

	@Override
	public int getType() {
		return RFBConstants.ENC_COMPRESS_LEVEL0;
	}

	@Override
	public boolean isPseudoEncoding() {
		return true;
	}

	@Override
	public void processEncodedRect(RFBDisplay<?, ?> display, int x, int y, int width, int height, int encodingType)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		return "Compress Level " + ( getType() - RFBConstants.ENC_COMPRESS_LEVEL0 );
	}
}
