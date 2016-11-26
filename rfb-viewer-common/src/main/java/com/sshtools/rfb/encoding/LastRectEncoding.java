package com.sshtools.rfb.encoding;

import java.io.IOException;

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfbcommon.RFBConstants;

public class LastRectEncoding implements RFBEncoding {
	public LastRectEncoding() {
	}

	@Override
	public int getType() {
		return RFBConstants.ENC_LAST_RECT;
	}

	@Override
	public boolean isPseudoEncoding() {
		return true;
	}

	@Override
	public void processEncodedRect(RFBDisplay display, int x, int y, int width,
			int height, int encodingType) throws IOException {
		display.getDisplayModel().drawRectangle(x, y, width, height, null);
	}

	@Override
	public String getName() {
		return "Last Rectangle";
	}

}
