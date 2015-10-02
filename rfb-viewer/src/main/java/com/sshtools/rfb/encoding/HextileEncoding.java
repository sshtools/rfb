package com.sshtools.rfb.encoding;

import java.awt.Color;
import java.awt.Graphics;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBDisplayModel;
import com.sshtools.rfbcommon.ImageUtil;

public class HextileEncoding extends AbstractRawEncoding {

	private Color hextile_bg, hextile_fg;

	// Hextile sub encodings
	final int HEXTILE_RAW = 1 << 0;
	final int HEXTILE_BACKGROUND = 1 << 1;
	final int HEXTILE_FOREGROUND = 1 << 2;
	final int HEXTILE_SUBRECTS = 1 << 3;
	final int HEXTILE_COLORED = 1 << 4;

	public HextileEncoding() {
	}

	@Override
	public int getType() {
		return 5;
	}

	@Override
	public boolean isPseudoEncoding() {
		return false;
	}

	@Override
	public void processEncodedRect(RFBDisplay display, int x, int y, int width, int height, int encodingType) throws IOException {
		ProtocolEngine engine = display.getEngine();
		RFBDisplayModel model = display.getDisplayModel();
		DataInputStream in = engine.getInputStream();
		hextile_bg = new Color(0);
		hextile_fg = new Color(0);
		for (int ty = y; ty < y + height; ty += 16) {
			int th = 16;
			if (y + height - ty < 16) {
				th = y + height - ty;
			}
			for (int tx = x; tx < x + width; tx += 16) {
				int tw = 16;
				if (x + width - tx < 16) {
					tw = x + width - tx;
				}
				int subencoding = in.readUnsignedByte();
				Graphics g = model.getGraphicBuffer();
				Map<Integer, Integer> colors  = model.getColorMap();
				if ((subencoding & HEXTILE_RAW) != 0) {
					engine.getContext().selectEncoding(RFBContext.ENCODING_RAW).processEncodedRect(display, tx, ty, tw, th, 0);
					continue;
				}
				byte[] cbuf = new byte[model.getBitsPerPixel() / 8];
				if ((subencoding & HEXTILE_BACKGROUND) != 0) {
					in.readFully(cbuf);
					hextile_bg = ImageUtil.decodeAndUntranslatePixelToColour(cbuf, 0, model);
				}
				g.setColor(hextile_bg);
				g.fillRect(tx, ty, tw, th);
				if ((subencoding & HEXTILE_FOREGROUND) != 0) {
					in.readFully(cbuf);
					hextile_fg = ImageUtil.decodeAndUntranslatePixelToColour(cbuf, 0, model);
				}
				if ((subencoding & HEXTILE_SUBRECTS) == 0) {
					continue;
				}
				int nSubrects = in.readUnsignedByte();
				int bufsize = nSubrects * 2;
				if ((subencoding & HEXTILE_COLORED) != 0) {
					bufsize += nSubrects * (model.getBitsPerPixel() / 8);
				}
				byte[] buf = new byte[bufsize];
				in.readFully(buf);
				int b1, b2, sx, sy, sw, sh;
				int i = 0;
				if ((subencoding & HEXTILE_COLORED) == 0) {
					g.setColor(hextile_fg);
					for (int j = 0; j < nSubrects; j++) {
						b1 = buf[i++] & 0xFF;
						b2 = buf[i++] & 0xFF;
						sx = tx + (b1 >> 4);
						sy = ty + (b1 & 0xf);
						sw = (b2 >> 4) + 1;
						sh = (b2 & 0xf) + 1;
						g.fillRect(sx, sy, sw, sh);
					}
				} else if (model.getBitsPerPixel() == RFBDisplay.COLOR_8BIT) {
					for (int j = 0; j < nSubrects; j++) {
						hextile_fg = new Color(colors.get(buf[i++] & 0xFF));
						b1 = buf[i++] & 0xFF;
						b2 = buf[i++] & 0xFF;
						sx = tx + (b1 >> 4);
						sy = ty + (b1 & 0xf);
						sw = (b2 >> 4) + 1;
						sh = (b2 & 0xf) + 1;
						g.setColor(hextile_fg);
						g.fillRect(sx, sy, sw, sh);
					}
				} else {
					for (int j = 0; j < nSubrects; j++) {
						int r1 = buf[i + 2] & 0xFF;
						int r2 = buf[i + 1] & 0xFF;
						int r3 = buf[i + 0] & 0xFF;
						Color subrectFg = ImageUtil.decodeAndUntranslatePixelToColour(buf, i, model);
						hextile_fg = new Color(r1, r2, r3);
						i += 4;
						b1 = buf[i++] & 0xFF;
						b2 = buf[i++] & 0xFF;
						sx = tx + (b1 >> 4);
						sy = ty + (b1 & 0xf);
						sw = (b2 >> 4) + 1;
						sh = (b2 & 0xf) + 1;
						g.setColor(subrectFg);
						g.fillRect(sx, sy, sw, sh);
					}
				}
			}
		}
		display.requestRepaint(display.getContext().getScreenUpdateTimeout(), x, y, width, height);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.rfb.RFBEncoding#getName()
	 */
	@Override
	public String getName() {
		return "Hextile";
	}

}
