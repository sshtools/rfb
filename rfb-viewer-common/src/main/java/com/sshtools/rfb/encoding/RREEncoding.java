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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBDisplayModel;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfb.RFBToolkit;
import com.sshtools.rfb.RFBToolkit.RFBColor;
import com.sshtools.rfb.RFBToolkit.RFBGraphicsContext;

public class RREEncoding implements RFBEncoding {
	public RREEncoding() {
	}

	@Override
	public int getType() {
		return 2;
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
		int nSubrects = in.readInt();
		byte[] bg_buf = new byte[model.getBitsPerPixel() / 8];
		in.readFully(bg_buf);
		RFBColor pixel = RFBToolkit.get().newColor();
		if (model.getBitsPerPixel() == RFBDisplay.COLOR_8BIT) {
			pixel.setRGB(model.getColorMap().get(bg_buf[0] & 0xFF));
		} else {
			pixel.setRGB(bg_buf[2] & 0xFF, bg_buf[1] & 0xFF, bg_buf[0] & 0xFF);
		}
		RFBGraphicsContext g = model.getGraphicBuffer();
		if (g == null) {
			return;
		}
		g.setColor(pixel);
		g.fillRect(x, y, width, height);
		byte[] buf = new byte[nSubrects * ((model.getBitsPerPixel() / 8) + 8)];
		in.readFully(buf);
		DataInputStream ds = new DataInputStream(new ByteArrayInputStream(buf));
		int sx, sy, sw, sh;
		for (int j = 0; j < nSubrects; j++) {
			if (model.getBitsPerPixel() == RFBDisplay.COLOR_8BIT) {
				pixel.setRGB(model.getColorMap().get(ds.readUnsignedByte()));
			} else {
				ds.skip(4);
				pixel.setRGB(buf[j * 12 + 2] & 0xFF, buf[j * 12 + 1] & 0xFF, buf[j * 12] & 0xFF);
			}
			sx = x + ds.readUnsignedShort();
			sy = y + ds.readUnsignedShort();
			sw = ds.readUnsignedShort();
			sh = ds.readUnsignedShort();
			g.setColor(pixel);
			g.fillRect(sx, sy, sw, sh);
		}
		display.requestRepaint(display.getContext().getScreenUpdateTimeout(), x, y, width, height);
	}

	@Override
	public String getName() {
		return "RRE";
	}
}
