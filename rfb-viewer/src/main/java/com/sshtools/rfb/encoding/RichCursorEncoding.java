package com.sshtools.rfb.encoding;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfbcommon.ImageUtil;
import com.sshtools.rfbcommon.RFBConstants;

public class RichCursorEncoding extends AbstractRawEncoding {
	public RichCursorEncoding() {
	}

	@Override
	public int getType() {
		return RFBConstants.ENC_RICH_CURSOR;
	}

	@Override
	public boolean isPseudoEncoding() {
		return true;
	}

	@Override
	public void processEncodedRect(RFBDisplay display, int hotx, int hoty,
			int width, int height, int encodingType) throws IOException {
		int bytesPerRow = (width + 7) / 8;
		int bytesMaskData = bytesPerRow * height;
		DataInputStream in = display.getEngine().getInputStream();

		if (width * height == 0) {
			return;
		}

		if (display.getContext().isCursorUpdateIgnored()) {
			in.skipBytes(width * height + bytesMaskData);
		} else {
			// An image to work with in the pixel format
			BufferedImage bim = display.getDisplayModel().getFactory().create(
					width, height);

			int bytesPixel = display.getDisplayModel().getBytesPerPixel();

			// Read pixel and mask data.
			byte[] pixBuf = new byte[width * height * bytesPixel];
			in.readFully(pixBuf);
			byte[] maskBuf = new byte[bytesMaskData];
			in.readFully(maskBuf);

			// Decode as if it's a raw image
			decodeIntoImage(pixBuf, display.getDisplayModel(), bim, 0);

			// Now make it ARGB so we can add some alpha
			bim = ImageUtil.ensureType(bim, BufferedImage.TYPE_INT_ARGB);

			int c = 0, n = 0, x = 0, ax = 0;
			for (int y = 0; y < height; y++) {
				ax = 0;
				for (x = 0; x < width / 8; x++) {
					c = maskBuf[y * bytesPerRow + x];
					for (n = 7; n >= 0; n--) {
						if ((c >> n & 1) == 0) {
							setAlpha(bim, (byte) 0x00, ax, y);
						}
						ax++;
					}
				}

				for (n = 7; n >= 8 - width % 8; n--) {
					if ((maskBuf[y * bytesPerRow + x] >> n & 1) == 0) {
						setAlpha(bim, (byte) 0x00, ax, y);
					}
					ax++;
				}
			}

			// Now update the display
			display.getDisplayModel().updateCursor(bim, hotx, hoty, width,
					height);
		}
	}

	public void setAlpha(BufferedImage obj_img, byte alpha, int cx, int cy) {
		alpha %= 0xff;
		int color = obj_img.getRGB(cx, cy);

		int mc = (alpha << 24) | 0x00ffffff;
		int newcolor = color & mc;
		obj_img.setRGB(cx, cy, newcolor);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.rfb.RFBEncoding#getName()
	 */
	@Override
	public String getName() {
		return "Rich Cursor";
	}
}
