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