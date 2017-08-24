package com.sshtools.rfb.encoding;

import java.io.DataInputStream;
import java.io.IOException;

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfb.RFBToolkit;
import com.sshtools.rfb.RFBToolkit.RFBImage;
import com.sshtools.rfb.RFBToolkit.RFBImage.Type;
import com.sshtools.rfbcommon.RFBConstants;

public class XCursorEncoding implements RFBEncoding {
	public XCursorEncoding() {
	}

	@Override
	public int getType() {
		return RFBConstants.ENC_X11_CURSOR;
	}

	@Override
	public boolean isPseudoEncoding() {
		return true;
	}

	@Override
	public void processEncodedRect(RFBDisplay<?, ?> display, int x2, int y2, int width, int height, int encodingType)
			throws IOException {
		int bytesPerRow = (width + 7) / 8;
		int bytesMaskData = bytesPerRow * height;
		DataInputStream in = display.getEngine().getInputStream();
		if (width * height == 0) {
			return;
		}
		if (display.getContext().isCursorUpdateIgnored()) {
			in.skipBytes(6 + bytesMaskData * 2);
		} else {
			// An image with alpha to draw the cursor on
			RFBImage bim = RFBToolkit.get().createImage(Type.ARGB, width, height);
			// Read foreground and background colors of the cursor.
			byte[] rgb = new byte[6];
			in.readFully(rgb);
			int[] colors = { (0xFF000000 | (rgb[3] & 0xFF) << 16 | (rgb[4] & 0xFF) << 8 | (rgb[5] & 0xFF)),
					(0xFF000000 | (rgb[0] & 0xFF) << 16 | (rgb[1] & 0xFF) << 8 | (rgb[2] & 0xFF)) };
			// Read pixel and mask data.
			byte[] pixBuf = new byte[bytesMaskData];
			in.readFully(pixBuf);
			byte[] maskBuf = new byte[bytesMaskData];
			in.readFully(maskBuf);
			// Decode pixel data into image
			byte pixByte, maskByte;
			int dx, dy, n;
			int i;
			for (dy = 0; dy < height; dy++) {
				i = 0;
				for (dx = 0; dx < width / 8; dx++) {
					pixByte = pixBuf[dy * bytesPerRow + dx];
					maskByte = maskBuf[dy * bytesPerRow + dx];
					for (n = 7; n >= 0; n--) {
						if ((maskByte >> n & 1) != 0) {
							bim.setRGB(i, dy, colors[pixByte >> n & 1]);
						} else {
							bim.setRGB(i, dy, 0x00ffffff);
						}
						i++;
					}
				}
				for (n = 7; n >= 8 - width % 8; n--) {
					if ((maskBuf[dy * bytesPerRow + dx] >> n & 1) != 0) {
						bim.setRGB(i, dy, colors[pixBuf[dy * bytesPerRow + dx] >> n & 1]);
					} else {
						bim.setRGB(i, dy, 0x00ffffff);
					}
				}
			}
			display.getDisplayModel().updateCursor(bim, x2, y2, width, height);
		}
	}

	@Override
	public String getName() {
		return "X Cursor";
	}
}
