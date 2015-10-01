package com.sshtools.rfb.encoding;

import java.awt.Color;
import java.awt.Graphics;
import java.io.DataInputStream;
import java.io.IOException;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBDisplayModel;
import com.sshtools.rfbcommon.ImageUtil;

public class CORREEncoding extends AbstractRawEncoding {
	public CORREEncoding() {
	}

	public int getType() {
		return 4;
	}

	public boolean isPseudoEncoding() {
		return false;
	}

	public void processEncodedRect(RFBDisplay display, int x, int y, int width, int height, int encodingType) throws IOException {

		ProtocolEngine engine = display.getEngine();
		RFBDisplayModel model = display.getDisplayModel();
		DataInputStream in = engine.getInputStream();

		int subrects = in.readInt();
		int bytesPerPixel = model.getBitsPerPixel() / 8;
		byte[] buffer = new byte[bytesPerPixel];
		in.readFully(buffer);
		Color pixel = ImageUtil.decodeAndUntranslatePixelToColour(buffer, 0, model);
		Graphics g = display.getDisplayModel().getGraphicBuffer();
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
			pixel = ImageUtil.decodeAndUntranslatePixelToColour(buffer2, i, model);
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

	public String getName() {
		return "CORRE";
	}

}
