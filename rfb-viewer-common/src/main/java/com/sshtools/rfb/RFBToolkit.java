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
package com.sshtools.rfb;

import com.sshtools.rfb.RFBToolkit.RFBImage.Type;
import com.sshtools.rfbcommon.PixelFormat;

public abstract class RFBToolkit {

	private static RFBToolkit instance;

	static {

	}

	public interface RFBColor {
		RFBColor setRGB(int rgb);

		RFBColor setRGB(int r, int g, int b);

	}

	public interface RFBGraphicsContext {

		void setColor(RFBColor pixel);

		void fillRect(int x, int y, int width, int height);

		void copyArea(int posx, int posy, int width, int height, int i, int j);

		void drawImage(RFBImage imageBuffer, int imagex, int imagey, int width, int height, int scaleMode);

		int[] getClipBounds();

		void setClip(int[] clip);

		void drawImage(RFBImage prev, int x, int y);

	}

	public interface RFBCursor {

	}

	public interface RFBImage {
		public enum Type {
			ARGB, ARGB_PRE, RGB, UNKNOWN, BYTE_BGRA, BYTE_BGRA_PRE, BYTE_INDEXED, BYTE_RGB
		}

		int getRGB(int x, int y);

		void setRGB(int x, int y, int rgb);

		RFBGraphicsContext getGraphicsContext();

		int getWidth();

		int getHeight();

		Object getData();

		Type getType();
	}

	public interface RFBClipboard {
		void setData(String data);
	}

	public final static RFBToolkit get() {
		if (instance == null) {
			throw new IllegalStateException("No toolkit initialized.");
		}
		return instance;
	}

	protected RFBToolkit() {
		instance = this;
	}

	public abstract RFBImage createImage(RFBImage.Type type, int w, int h);

	public abstract void run(Runnable r);

	public abstract RFBColor newColor();

	public abstract RFBClipboard getClipboard();

	public abstract RFBCursor createCursor(RFBImage img, int hotX, int hotY);

	public abstract RFBImage createImage(PixelFormat fmt, int w, int h);

	public abstract RFBImage ensureType(RFBImage bim, Type argb);

	public abstract RFBCursor getDefaultCursor();

	public abstract RFBImage loadImage(String string);

	public abstract void beep();

	public abstract RFBImage createImage(byte[] imageData);

	public abstract  RFBImage createTightCompatibleImage(int width, int height);
}
