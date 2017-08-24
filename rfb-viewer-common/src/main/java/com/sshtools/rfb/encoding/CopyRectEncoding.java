package com.sshtools.rfb.encoding;

import java.io.IOException;

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfbcommon.RFBConstants;

public class CopyRectEncoding implements RFBEncoding {
	public CopyRectEncoding() {
	}

	@Override
	public boolean isPseudoEncoding() {
		return false;
	}

	@Override
	public int getType() {
		return RFBConstants.ENC_COPYRECT;
	}

	@Override
	public void processEncodedRect(RFBDisplay<?, ?> display, int x, int y, int width, int height, int encodingType)
			throws IOException {
		int posx = display.getEngine().getInputStream().readUnsignedShort();
		int posy = display.getEngine().getInputStream().readUnsignedShort();
		display.getDisplayModel().getGraphicBuffer().copyArea(posx, posy, width, height, x - posx, y - posy);
		display.requestRepaint(display.getContext().getScreenUpdateTimeout(), x, y, width, height);
	}

	@Override
	public String getName() {
		return "CopyRect";
	}
}