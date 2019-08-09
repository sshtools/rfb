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

import java.io.DataInputStream;
import java.io.IOException;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBDisplayModel;
import com.sshtools.rfb.RFBToolkit;
import com.sshtools.rfb.RFBToolkit.RFBColor;
import com.sshtools.rfb.RFBToolkit.RFBGraphicsContext;
import com.sshtools.rfbcommon.ImageUtil;
import com.sshtools.rfbcommon.RFBConstants;

public class CORREEncoding extends AbstractRawEncoding {
	public CORREEncoding() {
	}

	@Override
	public int getType() {
		return RFBConstants.ENC_CORRE;
	}

	@Override
	public boolean isPseudoEncoding() {
		return false;
	}

	@Override
	public void processEncodedRect(RFBDisplay<?, ?> display, int x, int y, int width, int height, int encodingType)
			throws IOException {
		ProtocolEngine engine = display.getEngine();
		RFBDisplayModel model = display.getDisplayModel();
		DataInputStream in = engine.getInputStream();
		int subrects = in.readInt();
		int bytesPerPixel = model.getBitsPerPixel() / 8;
		byte[] buffer = new byte[bytesPerPixel];
		in.readFully(buffer);
		RFBColor pixel = RFBToolkit.get().newColor().setRGB(ImageUtil.decodeAndUntranslatePixel(buffer, 0, model));
		RFBGraphicsContext g = display.getDisplayModel().getGraphicBuffer();
		g.setColor(pixel);
		g.fillRect(x, y, width, height);
		byte[] buffer2 = new byte[subrects * (bytesPerPixel + 4)];
		in.readFully(buffer2);
		int sx;
		int sy;
		int sw;
		int sh;
		int i = 0;
		for (int j = 0; j < subrects; j++) {
			pixel.setRGB(ImageUtil.decodeAndUntranslatePixel(buffer2, i, model));
			i += bytesPerPixel;
			sx = x + (buffer2[i++] & 0xFF);
			sy = y + (buffer2[i++] & 0xFF);
			sw = buffer2[i++] & 0xFF;
			sh = buffer2[i++] & 0xFF;
			g.setColor(pixel);
			g.fillRect(sx, sy, sw, sh);
		}
		// Request a repaint
		display.requestRepaint(display.getContext().getScreenUpdateTimeout(), x, y, width, height);
	}

	@Override
	public String getName() {
		return "CORRE";
	}
}
