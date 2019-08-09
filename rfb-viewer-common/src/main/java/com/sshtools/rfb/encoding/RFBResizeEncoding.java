/**
 * RFB - Remote Frame Buffer (VNC) implementation.
 * Copyright © 2006 SSHTOOLS Limited (support@sshtools.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
