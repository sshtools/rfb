package com.sshtools.rfb.encoding;

import java.io.IOException;

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfbcommon.RFBConstants;

public class JPEGQuality0Encoding implements RFBEncoding {
	public JPEGQuality0Encoding() {
	}

	@Override
	public int getType() {
		return RFBConstants.ENC_JPEG_QUALITY_LEVEL0;
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
		return "JPEG Quality " + ( getType() - RFBConstants.ENC_JPEG_QUALITY_LEVEL0 );
	}
}
