package com.sshtools.rfbserver.encodings;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.ImageUtil;
import com.sshtools.rfbcommon.PaletteAnalyser;
import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public class HextileEncoding extends AbstractRawEncoding<BufferedImage> {
	final static Logger LOG = LoggerFactory.getLogger(HextileEncoding.class);
	// Hextile sub encodings
	protected final int HEXTILE_RAW = 1 << 0;
	protected final int HEXTILE_BACKGROUND = 1 << 1;
	protected final int HEXTILE_FOREGROUND = 1 << 2;
	protected final int HEXTILE_SUBRECTS = 1 << 3;
	protected final int HEXTILE_COLORED = 1 << 4;
	protected PaletteAnalyser paletteAnalyser = new PaletteAnalyser(256, 256);

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_HEXTILE;
	}

	public boolean isPseudoEncoding() {
		return false;
	}

	public void encode(UpdateRectangle<BufferedImage> update, ProtocolWriter dout, PixelFormat pixelFormat, RFBClient client)
			throws IOException {
		BufferedImage img = update.getData();
		dout.writeInt(getType().getCode());
		int tileSize = 16;
		int tileWidth = tileSize;
		int tileHeight = tileSize;
		// The number of colours needed before a tile will be delivered as Raw
		int rawThreshold = tileWidth * tileHeight / 2;
		// int rawThreshold = Integer.MAX_VALUE;
		int[] tileBuf = new int[tileWidth * tileHeight];
		int fg = -1;
		int bg = -1;
		int subenc = 0;
		List<SubRect> subrects = null;
		for (int y = 0; y < update.getArea().height; y += tileSize) {
			tileWidth = tileSize;
			Rectangle area = update.getArea();
			if (y + tileHeight > area.height) {
				tileHeight = update.getArea().height - y;
			}
			for (int x = 0; x < update.getArea().width; x += tileSize) {
				if (x + tileWidth > update.getArea().width) {
					tileWidth = update.getArea().width - x;
				}
				// Grab pixels
				img.getRGB(x, y, tileWidth, tileHeight, tileBuf, 0, tileWidth);
				paletteAnalyser.reset();
				paletteAnalyser.analyse(tileBuf, tileWidth * tileHeight);
				subenc = 0;
				if (paletteAnalyser.getSize() > rawThreshold) {
					// Use raw (clear background and foreground)
					subenc = HEXTILE_RAW;
					fg = -1;
					bg = -1;
				} else {
					int bgPix = paletteAnalyser.getBackground();
					if (bg == -1 || bg != bgPix) {
						bg = bgPix;
						subenc = HEXTILE_BACKGROUND;
						if (LOG.isDebugEnabled()) {
							LOG.debug(String.format("Writing background 0x%06x", bg));
						}
					}
					subrects = extractSubRectangles(tileBuf, bg, tileWidth, tileHeight, 255, 255);
					if (subrects.size() > 0) {
						if (paletteAnalyser.getSize() == 2) {
							int rectfg = paletteAnalyser.getForeground();
							if (rectfg == -1) {
								fg = -1;
							} else if (fg == -1 || fg != rectfg) {
								subenc = subenc | HEXTILE_FOREGROUND;
								fg = rectfg;
								if (LOG.isDebugEnabled()) {
									LOG.debug(String.format("Writing foreground 0x%06x", fg));
								}
							}
						} else {
							subenc = subenc | HEXTILE_COLORED;
						}
						subenc = subenc | HEXTILE_SUBRECTS;
					}
				}
				//
				dout.write(subenc);
				// Raw data
				if ((subenc & HEXTILE_RAW) > 0) {
					dout.write(prepareEncode(ImageUtil.copyImage(img.getSubimage(x, y, tileWidth, tileHeight), pixelFormat),
							pixelFormat));
					dout.flush();
					// Background may not be carried over if previous tile was
					// raw
					bg = -1;
					fg = -1;
					if (LOG.isDebugEnabled()) {
						LOG.debug(String.format("Writing Raw", bg));
					}
				} else {
					if (LOG.isDebugEnabled()) {
						LOG.debug(String.format("Writing Tiled", bg));
					}
					if ((subenc & HEXTILE_BACKGROUND) > 0) {
						writePixel(dout, pixelFormat, bg);
					}
					if ((subenc & HEXTILE_FOREGROUND) > 0) {
						writePixel(dout, pixelFormat, fg);
					}
					if ((subenc & HEXTILE_SUBRECTS) > 0) {
						if (LOG.isDebugEnabled()) {
							if ((subenc & HEXTILE_COLORED) > 0) {
								LOG.debug("Writing colour subrects " + subrects.size());
							} else {
								LOG.debug("Writing subrects " + subrects.size());
							}
						}
						dout.write(subrects.size());
						for (SubRect s : subrects) {
							if ((subenc & HEXTILE_COLORED) > 0) {
								writePixel(dout, pixelFormat, s.pixel);
							}
							dout.write(s.x << 4 | s.y & 0x0F);
							dout.write((s.w - 1) << 4 | (s.h - 1) & 0x0F);
						}
					}
					// The foreground pixel value may not be carried over if the
					// previous tile
					// had the Raw or SubrectsColoured bits set.
					if ((subenc & HEXTILE_COLORED) > 0) {
						fg = -1;
					}
				}
			}
		}
	}

	protected List<SubRect> extractSubRectangles(int[] pixels, int background, int w, int h, int maxw, int maxh) {
		SubRect subrect;
		List<SubRect> subrects = new ArrayList<SubRect>();
		int currentPixel;
		int currentX, currentY;
		int runningX, runningY;
		int firstX = 0, firstY, firstW, firstH;
		int secondX = 0, secondY, secondW, secondH;
		boolean firstYflag;
		int segment;
		int line;
		for (currentY = 0; currentY < h; currentY++) {
			line = currentY * w;
			for (currentX = 0; currentX < w; currentX++) {
				if (pixels[line + currentX] != background) {
					currentPixel = pixels[line + currentX];
					firstY = currentY - 1;
					firstYflag = true;
					for (runningY = currentY; runningY < h; runningY++) {
						segment = runningY * w;
						if (runningY - currentY >= maxh) {
							break;
						}
						if (pixels[segment + currentX] != currentPixel)
							break;
						runningX = currentX;
						while ((runningX < w) && (runningX - currentX < maxw) && (pixels[segment + runningX] == currentPixel))
							runningX++;
						runningX--;
						if (runningY == currentY)
							secondX = firstX = runningX;
						if (runningX < secondX)
							secondX = runningX;
						if (firstYflag && (runningX >= firstX))
							firstY++;
						else
							firstYflag = false;
					}
					secondY = runningY - 1;
					firstW = firstX - currentX + 1;
					firstH = firstY - currentY + 1;
					secondW = secondX - currentX + 1;
					secondH = secondY - currentY + 1;
					subrect = new SubRect();
					subrects.add(subrect);
					subrect.pixel = currentPixel;
					subrect.x = currentX;
					subrect.y = currentY;
					if ((firstW * firstH) > (secondW * secondH)) {
						subrect.w = firstW;
						subrect.h = firstH;
					} else {
						subrect.w = secondW;
						subrect.h = secondH;
					}
					for (runningY = subrect.y; runningY < (subrect.y + subrect.h); runningY++) {
						for (runningX = subrect.x; runningX < (subrect.x + subrect.w); runningX++) {
							pixels[runningY * w + runningX] = background;
						}
					}
				}
			}
		}
		return subrects;
	}

	protected boolean isAllRectanglesSameColour(Collection<SubRect> r) {
		int c = -1;
		for (SubRect a : r) {
			if (c == -1) {
				c = a.pixel;
			} else if (c != a.pixel) {
				return false;
			}
		}
		return true;
	}
}
