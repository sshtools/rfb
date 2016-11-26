package com.sshtools.rfb.encoding;

import java.io.IOException;

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEncoding;

public class RFBResizeEncoding implements RFBEncoding {
	public RFBResizeEncoding() {
	}

	@Override
	public int getType() {
		return 0xFFFFFF21;
	}

	@Override
	public boolean isPseudoEncoding() {
		return true;
	}

	@Override
	public void processEncodedRect(RFBDisplay display, int x, int y, int width,
			int height, int encodingType) throws IOException {
		display.getDisplayModel().changeFramebufferSize(width, height);
	}

	@Override
	public String getName() {
		return "Resize";
	}

}
