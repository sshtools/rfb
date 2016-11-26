package com.sshtools.rfb.swing;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import com.sshtools.rfb.RFBToolkit;
import com.sshtools.rfb.RFBToolkit.RFBImage.Type;
import com.sshtools.rfbcommon.ImageUtil;
import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.PixelFormatImageFactory;

public class SwingRFBToolkit extends RFBToolkit implements RFBToolkit.RFBClipboard, ClipboardOwner {

	public SwingRFBToolkit() {
		super();
	}

	@Override
	public RFBImage createImage(Type type, int w, int h) {
		return new RFBBufferedImage(new BufferedImage(w, h, typeToNativeType(type)));
	}

	@Override
	public void run(Runnable r) {
		SwingUtilities.invokeLater(r);
	}

	@Override
	public RFBColor newColor() {
		return new RFBAWTColor();
	}

	@Override
	public RFBClipboard getClipboard() {
		return this;
	}

	@Override
	public RFBCursor createCursor(RFBImage img, int hotX, int hotY) {
		Point hotspot = new Point(hotX, hotY);
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		return new RFBAWTCursor(toolkit.createCustomCursor(((RFBBufferedImage) img).backing, hotspot, "none"));
	}

	@Override
	public RFBImage createImage(PixelFormat fmt, int w, int h) {
		PixelFormatImageFactory pfim = new PixelFormatImageFactory(fmt);
		return new RFBBufferedImage(pfim.create(w, h));
	}

	@Override
	public RFBImage ensureType(RFBImage bim, Type type) {
		BufferedImage newType = ImageUtil.ensureType(((RFBBufferedImage) bim).backing, typeToNativeType(type));
		if (newType == ((RFBBufferedImage) bim).backing) {
			return bim;
		}
		return new RFBBufferedImage(newType);
	}

	@Override
	public RFBCursor getDefaultCursor() {
		return new RFBAWTCursor(Cursor.getDefaultCursor());
	}

	@Override
	public RFBImage loadImage(String string) {
		try {
			return new RFBBufferedImage(ImageIO.read(getClass().getResource(string)));
		} catch (IOException ioe) {
			throw new IllegalArgumentException("Invalid image resource.", ioe);
		}
	}

	@Override
	public void beep() {
		Toolkit.getDefaultToolkit().beep();
	}

	@Override
	public RFBImage createImage(byte[] imageData) {
		try {
			return new RFBBufferedImage(ImageIO.read(new ByteArrayInputStream(imageData)));
		} catch (IOException ioe) {
			throw new IllegalArgumentException("Invalid image data.", ioe);
		}
	}

	@Override
	public RFBImage createTightCompatibleImage(int width, int height) {
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
		int[] nBits = { 8, 8, 8 };
		int[] bOffs = { 0, 1, 2 };
		ColorModel colorModel = new ComponentColorModel(cs, nBits, false, false, Transparency.OPAQUE,
				DataBuffer.TYPE_BYTE);
		Raster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, width * 3, 3, bOffs, null);
		BufferedImage img = new BufferedImage(colorModel, (WritableRaster) raster, false, null);
		return new RFBBufferedImage(img);
	}

	@Override
	public void setData(String data) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(data), this);
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}

	static int typeToNativeType(Type type) {
		switch (type) {
		case ARGB:
			return BufferedImage.TYPE_INT_ARGB;
		default:
			throw new IllegalArgumentException(String.format("Unknown type %s", type));
		}
	}

	final class RFBAWTColor implements RFBColor {
		Color nativeColor;

		@Override
		public RFBColor setRGB(int r, int g, int b) {
			nativeColor = new Color(r, g, b);
			return this;
		}

		@Override
		public RFBColor setRGB(int rgb) {
			nativeColor = new Color(rgb);
			return this;
		}
	}

	class RFBAWTCursor implements RFBCursor {
		Cursor cursor;

		public RFBAWTCursor(Cursor cursor) {
			this.cursor = cursor;
		}
	}

	static class RFBBufferedImage implements RFBImage {
		BufferedImage backing;
		RFBGraphicsContext ctx;

		RFBBufferedImage(BufferedImage backing) {
			this.backing = backing;
			ctx = new RFBGraphics2D((Graphics2D) backing.getGraphics());
		}

		@Override
		public int getRGB(int x, int y) {
			return backing.getRGB(x, y);
		}

		@Override
		public void setRGB(int x, int y, int rgb) {
			backing.setRGB(x, y, rgb);
		}

		@Override
		public RFBGraphicsContext getGraphicsContext() {
			return ctx;
		}

		@Override
		public int getWidth() {
			return backing.getWidth();
		}

		@Override
		public int getHeight() {
			return backing.getHeight();
		}

		@Override
		public Object getData() {
			switch (backing.getRaster().getDataBuffer().getDataType()) {
			case DataBuffer.TYPE_INT:
				return ((DataBufferInt) backing.getRaster().getDataBuffer()).getData();
			case DataBuffer.TYPE_SHORT:
				return ((DataBufferUShort) backing.getRaster().getDataBuffer()).getData();
			case DataBuffer.TYPE_BYTE:
				return ((DataBufferByte) backing.getRaster().getDataBuffer()).getData();
			default:
				throw new IllegalStateException("Unknown data type");
			}

		}

		@Override
		public Type getType() {
			switch (backing.getType()) {
			case BufferedImage.TYPE_INT_ARGB:
				return Type.ARGB;
			default:
				return Type.UNKNOWN;
			}
		}

	}

}
