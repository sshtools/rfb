package com.sshtools.rfb.encoding;

import java.io.IOException;

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfbcommon.RFBConstants;

public class CursorPositionEncoding implements RFBEncoding {
	public CursorPositionEncoding() {
	}

	@Override
	public int getType() {
		return RFBConstants.ENC_POINTER_POS;
	}

	@Override
	public boolean isPseudoEncoding() {
		return true;
	}

	@Override
	public void processEncodedRect(RFBDisplay<?, ?> display, int x, int y, int width, int height, int encodingType)
			throws IOException {
		display.getDisplayModel().softCursorMove(x, y);
	}

	@Override
	public String getName() {
		return "Cursor Position";
	}
}
