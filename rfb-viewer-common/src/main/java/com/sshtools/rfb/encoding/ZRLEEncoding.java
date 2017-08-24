package com.sshtools.rfb.encoding;

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBToolkit;
import com.sshtools.rfb.RFBToolkit.RFBImage;
import com.sshtools.rfb.RawBuffer;
import com.sshtools.rfbcommon.RFBConstants;

public class ZRLEEncoding extends ZLIBEncoding {
	private static final int MAX_TILE_SIZE = 64;
	private RawBuffer rawBuffer;

	@Override
	public int getType() {
		return RFBConstants.ENC_ZRLE;
	}

	@Override
	public boolean isPseudoEncoding() {
		return false;
	}

	@Override
	protected int doProcessRaw(RFBDisplay<?, ?> display, int x, int y, int width, int height, byte[] bytes) {
		int offset = 0;
		int maxX = x + width;
		int maxY = y + height;
		for (int tileY = y; tileY < maxY; tileY += MAX_TILE_SIZE) {
			int tileHeight = Math.min(maxY - tileY, MAX_TILE_SIZE);
			for (int tileX = x; tileX < maxX; tileX += MAX_TILE_SIZE) {
				int tileWidth = Math.min(maxX - tileX, MAX_TILE_SIZE);
				RFBImage img = RFBToolkit.get().createImage(display.getDisplayModel(), tileWidth, tileHeight);
				rawBuffer = new RawBuffer(img, display.getDisplayModel().getBytesPerCPIXEL(), display.getDisplayModel());
				int subencoding = bytes[offset++] & 0x0ff;
				int paletteSize = subencoding & 127;
				offset += rawBuffer.readPalette(paletteSize, bytes, offset);
				if (subencoding == RFBConstants.ZRLE_SOLID) {
					rawBuffer.fillPalette(0);
				} else if ((subencoding & 128) != 0) {
					if (0 == paletteSize) {
						offset += rle(bytes, offset, display, tileX, tileY, tileWidth, tileHeight);
					} else {
						offset += paletteRle(bytes, offset, tileX, tileY, tileWidth, tileHeight);
					}
				} else {
					if (0 == paletteSize) {
						offset += rawBuffer.draw(bytes, offset, tileWidth, tileHeight);
					} else {
						offset += packed(bytes, offset, paletteSize, tileX, tileY, tileWidth, tileHeight);
					}
				}
				display.getDisplayModel().drawRectangle(tileX, tileY, tileWidth, tileHeight, rawBuffer.getImage());
			}
		}
		return 0;
	}

	private int rle(byte[] bytes, int offset, RFBDisplay<?, ?> display, int tx, int ty, int tw, int th) {
		int cp = display.getDisplayModel().getBytesPerCPIXEL();
		int dataOffset = 0;
		int end = tw * th;
		int index = offset;
		while (dataOffset < end) {
			int color = rawBuffer.decode(bytes, index);
			index += cp;
			int run = 1;
			do {
				run += bytes[index] & 0x0ff;
			} while ((bytes[index++] & 0x0ff) == 255);
			dataOffset += rawBuffer.fill(dataOffset, run, color);
		}
		return index - offset;
	}

	private int paletteRle(byte[] bytes, int offset, int tx, int ty, int tw, int th) {
		int dataOffset = 0;
		int end = tw * th;
		int index = offset;
		while (dataOffset < end) {
			int colorIndex = bytes[index++];
			int run = 1;
			if ((colorIndex & 128) != 0) {
				do {
					run += bytes[index] & 0x0ff;
				} while (bytes[index++] == (byte) 255);
			}
			dataOffset += rawBuffer.fillPalette(dataOffset, run, colorIndex & 127);
		}
		return index - offset;
	}

	private int packed(byte[] buf, int offset, int paletteSize, int tx, int ty, int tw, int th) {
		int bitsPerPalletedPixel = paletteSize > 16 ? 8 : paletteSize > 4 ? 4 : paletteSize > 2 ? 2 : 1;
		int index = offset;
		int dataOffset = 0;
		for (int i = 0; i < th; ++i) {
			int decodedRowEnd = dataOffset + tw;
			int byteProcessed = 0;
			int bitsRemain = 0;
			while (dataOffset < decodedRowEnd) {
				if (bitsRemain == 0) {
					byteProcessed = buf[index++];
					bitsRemain = 8;
				}
				bitsRemain -= bitsPerPalletedPixel;
				int colorIndex = byteProcessed >> bitsRemain & (1 << bitsPerPalletedPixel) - 1 & 127;
				rawBuffer.fillPalette(dataOffset, 1, colorIndex);
				++dataOffset;
			}
		}
		return index - offset;
	}
}
