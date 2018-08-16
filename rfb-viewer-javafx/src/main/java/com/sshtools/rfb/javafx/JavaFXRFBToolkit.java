/**
 * RFB - Remote Frame Buffer (VNC) implementation for JavaFX.
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
package com.sshtools.rfb.javafx;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.sshtools.rfb.RFBToolkit;
import com.sshtools.rfb.RFBToolkit.RFBImage.Type;
import com.sshtools.rfbcommon.PixelFormat;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;

public class JavaFXRFBToolkit extends RFBToolkit implements RFBToolkit.RFBClipboard {
	private static AudioClip beep = new AudioClip(JavaFXRFBToolkit.class.getResource("/beep.wav").toString());

	public JavaFXRFBToolkit() {
		super();
	}

	@Override
	public RFBImage createImage(Type type, int w, int h) {
		return new RFBJavaFXImage(type, w, h);
	}

	@Override
	public void run(Runnable r) {
		Platform.runLater(r);
	}

	@Override
	public RFBColor newColor() {
		return new RFBJavaFXColor();
	}

	@Override
	public RFBClipboard getClipboard() {
		return this;
	}

	@Override
	public RFBCursor createCursor(RFBImage img, int hotX, int hotY) {
		return new RFBJavaFXCursor(new ImageCursor(((RFBJavaFXImage) img).backing, hotX, hotY));
	}

	@Override
	public RFBImage createImage(PixelFormat fmt, int w, int h) {
		return new RFBJavaFXImage(fmt, w, h);
	}

	@Override
	public RFBImage ensureType(RFBImage bim, Type type) {
		if (bim.getType() != type) {
		}
		return bim;
	}

	@Override
	public RFBCursor getDefaultCursor() {
		return new RFBJavaFXCursor(Cursor.DEFAULT);
	}

	@Override
	public RFBImage loadImage(String resource) {
		InputStream resourceAsStream = getClass().getResourceAsStream(resource);
		if (resourceAsStream == null) {
			throw new IllegalArgumentException("Image not found " + resource);
		}
		return new RFBJavaFXImage(new Image(resourceAsStream));
	}

	@Override
	public void beep() {
		Platform.runLater(new Runnable() {
			public void run() {
				beep.play();
			}
		});
	}

	@Override
	public RFBImage createImage(byte[] imageData) {
		InputStream resourceAsStream = new ByteArrayInputStream(imageData);
		return new RFBJavaFXImage(new Image(resourceAsStream));
	}

	@Override
	public RFBImage createTightCompatibleImage(int width, int height) {
		// ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
		// int[] nBits = { 8, 8, 8 };
		// int[] bOffs = { 0, 1, 2 };
		// ColorModel colorModel = new ComponentColorModel(cs, nBits, false,
		// false, Transparency.OPAQUE,
		// DataBuffer.TYPE_BYTE);
		// Raster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
		// width, height, width * 3, 3, bOffs, null);
		// BufferedImage img = new BufferedImage(colorModel, (WritableRaster)
		// raster, false, null);
		// return new RFBJavaFXImage(img);
		throw new UnsupportedOperationException();
	}

	@Override
	public void setData(String data) {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final ClipboardContent content = new ClipboardContent();
		content.putString(data);
		clipboard.setContent(content);
	}

	static javafx.scene.image.PixelFormat.Type typeToNativeType(Type type) {
		switch (type) {
		case ARGB:
			return javafx.scene.image.PixelFormat.Type.INT_ARGB;
		case ARGB_PRE:
			return javafx.scene.image.PixelFormat.Type.INT_ARGB_PRE;
		case BYTE_BGRA:
			return javafx.scene.image.PixelFormat.Type.BYTE_BGRA;
		case BYTE_BGRA_PRE:
			return javafx.scene.image.PixelFormat.Type.BYTE_BGRA_PRE;
		case BYTE_INDEXED:
			return javafx.scene.image.PixelFormat.Type.BYTE_INDEXED;
		case BYTE_RGB:
			return javafx.scene.image.PixelFormat.Type.BYTE_RGB;
		default:
			throw new IllegalArgumentException(String.format("Unknown type %s", type));
		}
	}

	private final class RFBJavaFXColor implements RFBColor {
		Color nativeColor;

		@Override
		public RFBColor setRGB(int r, int g, int b) {
			nativeColor = Color.rgb(r, g, b);
			return this;
		}

		@Override
		public RFBColor setRGB(int rgb) {
			int r = (rgb >> 24) & 0xff;
			int g = (rgb >> 16) & 0xff;
			int b = (rgb >> 8) & 0xff;
			nativeColor = Color.rgb(r, g, b);
			return this;
		}
	}

	static class RFBGraphics2D implements RFBGraphicsContext {
		PixelReader pr;
		PixelWriter pw;
		RFBColor color;
		Type type;

		RFBGraphics2D(PixelReader pr, PixelWriter pw, Type type) {
			this.pr = pr;
			this.pw = pw;
			this.type = type;
		}

		@Override
		public void setColor(RFBColor color) {
			this.color = color;
		}

		@Override
		public void fillRect(int x, int y, int width, int height) {
			// TODO optimise
			int ty;
			int th = y + height;
			int tw = x + width;
			for (int tx = x; tx < tw; tx++) {
				for (ty = y; ty < th; ty++) {
					pw.setColor(tx, ty, ((RFBJavaFXColor) color).nativeColor);
				}
			}
			// switch (pr.getPixelFormat().getType()) {
			// case BYTE_BGRA:
			// case BYTE_BGRA_PRE:
			// case BYTE_INDEXED:
			// case BYTE_RGB:
			// byte[] buf = new byte[width * height];
			// pw.setPixels(x, y, width, height,
			// javafx.scene.image.PixelFormat.ByIntArgbInstance(), buf, 0,
			// width);
			//
			// PixelFormat.getIntArgbInstance()
			//
			//
			//
			// backing.getPixelReader().getPixels(0, 0, width, height,
			// (WritablePixelFormat<ByteBuffer>)
			// backing.getPixelReader().getPixelFormat(), buf, 0, width);
			// javafx.scene.image.PixelFormat<ByteBuffer> w =
			// backing.getPixelReader().getPixelFormat()
			// .getByteRgbInstance();
			// return buf;
			// case INT_ARGB:
			// case INT_ARGB_PRE:
			// int[] ibuf = new int[width * height];
			// backing.getPixelReader().getPixels(0, 0, width, height,
			// (WritablePixelFormat<IntBuffer>)
			// backing.getPixelReader().getPixelFormat(), ibuf, 0, width);
			// return ibuf;
			// default:
			// throw new IllegalStateException("Unknown data type");
			// }
		}

		@Override
		public void copyArea(int x, int y, int width, int height, int dx, int dy) {
			WritableImage copyRect = new WritableImage(width, height);
			copyRect.getPixelWriter().setPixels(0, 0, width, height, pr, x, y);
			pw.setPixels(dx, dy, width, height, copyRect.getPixelReader(), 0, 0);
		}

		@Override
		public void drawImage(RFBImage imageBuffer, int x, int y, int width, int height, int scaleMode) {
			pw.setPixels(x, y, width, height, ((RFBJavaFXImage) imageBuffer).backing.getPixelReader(), 0, 0);
		}

		@Override
		public int[] getClipBounds() {
			// TODO Does this matter? Only usages is in paintCursor and im not
			// convinced it's actually needed. Test this before removing from
			// interface
			return new int[] { 0, 0, 0, 0 };
		}

		@Override
		public void setClip(int[] clip) {
			// if (clip == null)
			// g2d.setClip(null);
			// else
			// g2d.setClip(clip[0], clip[1], clip[2], clip[3]);
			// TODO Does this matter? Only usages is in paintCursor and im not
			// convinced it's actually needed. Test this before removing from
			// interface
		}

		@Override
		public void drawImage(RFBImage img, int x, int y) {
			drawImage(img, x, y, img.getWidth(), img.getHeight(), -1);
		}
	}

	class RFBJavaFXCursor implements RFBCursor {
		Cursor cursor;

		public RFBJavaFXCursor(Cursor cursor) {
			this.cursor = cursor;
		}
	}

	static class RFBJavaFXImage implements RFBImage {
		Image backing;
		RFBGraphicsContext ctx;
		PixelReader reader;
		PixelWriter writer;
		Type type;

		RFBJavaFXImage(Image backing) {
			this.backing = backing;
			if (backing instanceof WritableImage) {
				writer = ((WritableImage) backing).getPixelWriter();
			}
			reader = backing.getPixelReader();
			ctx = new RFBGraphics2D(reader, writer, getType());
		}

		public RFBJavaFXImage(Type type, int w, int h) {
			this.type = type;
			backing = new WritableImage(w, h);
			writer = ((WritableImage) backing).getPixelWriter();
			reader = backing.getPixelReader();
			ctx = new RFBGraphics2D(reader, writer, getType());
		}

		public RFBJavaFXImage(PixelFormat fmt, int w, int h) {
			// if (!pixelFormat.isTrueColor()) {
			// // TODO 16 bit indexed
			// if (LOG.isLoggable(Level.FINE)) {
			// LOG.fine("Creating Indexed 8 bit colour model");
			// }
			// Map<Integer, Integer> colors = pixelFormat.getColorMap();
			// byte[] r = new byte[colors.size()];
			// byte[] g = new byte[colors.size()];
			// byte[] b = new byte[colors.size()];
			// for (Map.Entry<Integer, Integer> en : colors.entrySet()) {
			// r[en.getKey()] = (byte) (en.getValue() >> 16);
			// g[en.getKey()] = (byte) (en.getValue() >> 8);
			// b[en.getKey()] = (byte) (en.getValue() & 0xff);
			// }
			//
			// WritableImage img = new WritableImage(width, height);
			// PixelWriter pw;
			//
			//
			// colorModel = new IndexColorModel(8, colors.size(), r, g, b);
			// }
			backing = new WritableImage(w, h);
			writer = ((WritableImage) backing).getPixelWriter();
			reader = backing.getPixelReader();
			ctx = new RFBGraphics2D(reader, writer, getType());
		}

		@Override
		public int getRGB(int x, int y) {
			return reader.getArgb(x, y);
		}

		@Override
		public void setRGB(int x, int y, int rgb) {
			if (writer == null)
				throw new UnsupportedOperationException("Not a writable image.");
			writer.setArgb(x, y, rgb);
		}

		@Override
		public RFBGraphicsContext getGraphicsContext() {
			return ctx;
		}

		@Override
		public int getWidth() {
			return (int) backing.getWidth();
		}

		@Override
		public int getHeight() {
			return (int) backing.getHeight();
		}

		@Override
		public Object getData() {
			int width = (int) backing.getWidth();
			int height = (int) backing.getHeight();
			switch (backing.getPixelReader().getPixelFormat().getType()) {
			case BYTE_BGRA:
			case BYTE_BGRA_PRE:
			case BYTE_INDEXED:
			case BYTE_RGB:
				byte[] buf = new byte[width * height];
				backing.getPixelReader().getPixels(0, 0, width, height,
						(WritablePixelFormat<ByteBuffer>) backing.getPixelReader().getPixelFormat(), buf, 0, width);
				javafx.scene.image.PixelFormat<ByteBuffer> w = backing.getPixelReader().getPixelFormat().getByteRgbInstance();
				return buf;
			case INT_ARGB:
			case INT_ARGB_PRE:
				int[] ibuf = new int[width * height];
				backing.getPixelReader().getPixels(0, 0, width, height,
						(WritablePixelFormat<IntBuffer>) backing.getPixelReader().getPixelFormat(), ibuf, 0, width);
				return ibuf;
			default:
				throw new IllegalStateException("Unknown data type");
			}
		}

		@Override
		public Type getType() {
			if (type == null) {
				switch (backing.getPixelReader().getPixelFormat().getType()) {
				case INT_ARGB:
					return Type.ARGB;
				case INT_ARGB_PRE:
					return Type.ARGB_PRE;
				case BYTE_BGRA:
					return Type.BYTE_BGRA;
				case BYTE_BGRA_PRE:
					return Type.BYTE_BGRA_PRE;
				case BYTE_INDEXED:
					return Type.BYTE_INDEXED;
				case BYTE_RGB:
					return Type.BYTE_RGB;
				default:
					return Type.UNKNOWN;
				}
			} else {
				return type;
			}
		}
	}
}
