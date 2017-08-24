package com.sshtools.rfb.encoding;

import java.io.IOException;

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.ScreenData;
import com.sshtools.rfbcommon.ScreenDetail;
import com.sshtools.rfbcommon.ScreenDimension;

public class RFBResizeEncoding implements RFBEncoding {
	public RFBResizeEncoding() {
	}

	@Override
	public int getType() {
		return RFBConstants.ENC_NEW_FB_SIZE;
	}

	@Override
	public boolean isPseudoEncoding() {
		return true;
	}

	@Override
	public void processEncodedRect(RFBDisplay<?, ?> display, int x, int y, int width, int height, int encodingType)
			throws IOException {
		ScreenDimension dim = new ScreenDimension(width, height);
		ScreenData sd = new ScreenData(dim);
		sd.getDetails().add(new ScreenDetail(0, 0, 0, dim, 0));
		display.getDisplayModel().changeFramebufferSize(ExtendedDesktopSizeEncoding.SERVER_SIDE_CHANGE, sd);
	}

	@Override
	public String getName() {
		return "Resize";
	}
}
