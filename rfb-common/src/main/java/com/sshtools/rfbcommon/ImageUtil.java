/**
 * RFB Common - Remote Frame Buffer common code used both in client and server.
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
package com.sshtools.rfbcommon;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class ImageUtil {

	public static class ColourData {
		public Integer unique;
		public Integer background;
		public Integer foreground;
		public Set<Integer> palette;
	}

	public static int unsignedToBytes(byte b) {
		return b & 0xFF;
	}

	public static BufferedImage ensureType(BufferedImage img, int type) {
		if (type != BufferedImage.TYPE_CUSTOM && img.getType() != type) {
			BufferedImage bimg = new BufferedImage(img.getWidth(),
					img.getHeight(), type);
			bimg.getGraphics().drawImage(img, 0, 0, null);
			return bimg;
		}
		return img;
	}

	public static String debugBits(BitSet bs, int bytesPerRow) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < bytesPerRow * 64; i++) {
			b.append(bs.get(i) ? "1" : "0");
		}
		return b.toString();
	}

	public static byte[] toByteArray(BitSet bits, int bytesPerRow) {
		byte[] bytes = new byte[bytesPerRow];
		for (int i = 0; i < bytesPerRow * 8; i++) {
			if (bits.get(i)) {
				bytes[bytes.length - i / 8 - 1] |= 1 << (i % 8);
			}
		}
		return bytes;
	}

	public static ColourData count(int[] pixels) {
		Map<Integer, Integer> counter = new TreeMap<Integer, Integer>();
		for (int a : pixels) {
			a = a | 0xff000000;
			Integer c = counter.get(a);
			if (c == null) {
				c = 0;
			}
			c++;
			counter.put(a, c);
		}
		List<Map.Entry<Integer, Integer>> l = new ArrayList<Map.Entry<Integer, Integer>>(
				counter.entrySet());
		Collections.sort(l, new Comparator<Map.Entry<Integer, Integer>>() {
			public int compare(Entry<Integer, Integer> o1,
					Entry<Integer, Integer> o2) {
				return o1.getValue().compareTo(o2.getValue()) * -1;
			}
		});
		ColourData cd = new ColourData();
		Iterator<Map.Entry<Integer, Integer>> iterator = l.iterator();
		cd.background = iterator.next().getKey();
		if (iterator.hasNext()) {
			cd.foreground = iterator.next().getKey();
		} else {
			cd.foreground = -1;
		}
		cd.palette = counter.keySet();
		cd.unique = counter.size();
		return cd;
	}

	public static ColourData XXcount(int[] pixels) {
		Map<Integer, Integer> counter = new HashMap<Integer, Integer>();
		for (int a : pixels) {
			a = a | 0xff000000;
			Integer c = counter.get(a);
			if (c == null) {
				c = 0;
			}
			c++;
			counter.put(a, c);
		}
		int maxC = 0;
		int maxV = 0;
		for (Map.Entry<Integer, Integer> a : counter.entrySet()) {
			if (a.getValue() > maxC) {
				maxC = a.getValue();
				maxV = a.getKey();
			}
		}
		ColourData cd = new ColourData();
		if (maxC == counter.size()) {
			cd.foreground = maxV;
		}
		cd.background = maxV;
		cd.unique = counter.size();
		return cd;
	}

	public static BufferedImage copyImage(BufferedImage img, PixelFormat fmt) {
		PixelFormatImageFactory pim = new PixelFormatImageFactory(fmt);
		BufferedImage bim = pim.create(img.getWidth(), img.getHeight());
		Graphics g = bim.getGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
		return bim;
	}

	public static BufferedImage deepCopy(BufferedImage bi) {
		// Doesn't work with subimages
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	public static String debug(int[] rgb) {
		return "RGB:" + rgb[0] + "," + rgb[1] + "," + rgb[2];
	}

	public static int[] from555toRGB(short argb) {
		return new int[] { (argb >> 10) & 0x1F, (argb >> 5) & 0x1F, argb & 0x1F };
	}

	public static int[] from565toRGB(short argb) {
		return new int[] { (argb >> 11) & 0x1F, (argb >> 5) & 0x3F, argb & 0x1F };
	}

	public static int[] toRGB(int argb) {
		return new int[] { (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF };
	}

	public static byte[] translateAndEncodePixel16Bit(PixelFormat pixelFormat,
			short pixel) {
		pixel = translate16BitToFormattedPixel(pixelFormat, pixel);
		byte[] out = new byte[pixelFormat.getBytesPerPixel()];
		if (pixelFormat.isBigEndian()) {
			out[1] = (byte) (pixel);
			out[0] = (byte) (pixel >> 8);
			return out;
		} else {
			out[0] = (byte) (pixel);
			out[1] = (byte) (pixel >> 8);
			return out;
		}
	}

	public static byte[] translateAndEncodePixel(PixelFormat pixelFormat,
			int pixel) {
		pixel = translateARGBToFormattedPixel(pixelFormat, pixel);
		byte[] out = new byte[pixelFormat.getBitsPerPixel() / 8];
		if (pixelFormat.isBigEndian()) {
			for (int i = 0; i < out.length; i++) {
				out[out.length - i - 1] = (byte) ((pixel >> (i * 8)) & 0xff);
			}
		} else {
			for (int i = 0; i < out.length; i++) {
				out[i] = (byte) ((pixel >> (i * 8)) & 0xff);
			}
		}
		return out;
	}

	public static int translate(int rawColor, PixelFormat rfbModel) {
		return (255	* (rawColor >> rfbModel.getRedShift() & rfbModel.getRedMax()) / rfbModel.getRedMax() << 16)	| 
			   (255 * (rawColor >> rfbModel.getGreenShift() & rfbModel.getGreenMax()) / rfbModel.getGreenMax() << 8) |
			   (255 * (rawColor >> rfbModel.getBlueShift() & rfbModel.getBlueMax()) / rfbModel.getBlueMax());
	}

	public static int decodeAndUntranslatePixel(byte[] b, int off,
			PixelFormat model) {
		int bytesPerPixel = model.getBytesPerPixel();
		return decodePixel(b, off, model, bytesPerPixel);

	}

	public static short decodePixelShort(byte[] b, int off,
			PixelFormat model) {
		return (short)decodePixel(b, off, model, 2);
	}

	public static int decodePixel(byte[] b, int off,
			PixelFormat model, int bytesPerPixel) {
		int translated = 0;
		if (model.isBigEndian()) {
			for (int i = 0; i < bytesPerPixel; i++) {
				translated = translated
						| (b[off + bytesPerPixel - i - 1] & 0xff) << (i * 8);
			}
		} else {
			for (int i = 0; i < bytesPerPixel; i++) {
				translated = translated | (b[i + off] & 0xff) << (i * 8);
			}
		}
		return translated;
	}

	public static byte[] decodeCPIXELsTo4ByteBuffer(byte[] bytes, int offset,
			int size, PixelFormat fmt) {
		ByteBuffer buf = ByteBuffer.allocate(size * 4);
		boolean fitsInLSCPIXEL2 = fmt.isFitsInLSCPIXEL();
		boolean fitsInMSCPIXEL2 = fmt.isFitsInMSCPIXEL();
		for (int i = 0; i < size; i++) {
			if ((fitsInLSCPIXEL2 && fmt.isBigEndian())
					|| (fitsInMSCPIXEL2 && !fmt.isBigEndian())) {
				buf.put((byte) 0);
				buf.put(bytes[offset++]);
				buf.put(bytes[offset++]);
				buf.put(bytes[offset++]);
			} else {
				buf.put(bytes[offset++]);
				buf.put(bytes[offset++]);
				buf.put(bytes[offset++]);
				buf.put((byte) 0);
			}
		}
		return buf.array();
	}

	public static Color decodeAndUntranslatePixelToColour(byte[] b, int off,
			PixelFormat model) throws IOException {
		return new Color(decodeAndUntranslatePixel(b, off, model));
	}

	//
	// Private
	//
	private static short translate16BitToFormattedPixel(
			PixelFormat pixelFormat, short pixel) {
		int[] rgb = pixelFormat.getColorDepth() == 15 ? from555toRGB(pixel)
				: from565toRGB(pixel);
		return (short) ((rgb[0] & pixelFormat.getRedMax()) << pixelFormat
				.getRedShift()
				| (rgb[1] & pixelFormat.getGreenMax()) << pixelFormat
						.getGreenShift() | (rgb[2] & pixelFormat.getBlueMax()) << pixelFormat
				.getBlueShift());
	}

	private static int translateARGBToFormattedPixel(PixelFormat pixelFormat,
			int pixel) {
		int[] rgb = toRGB(pixel);
		return ((rgb[0] >> (8 - pixelFormat.getRedMaxBits())) & pixelFormat
				.getRedMax()) << pixelFormat.getRedShift()
				| ((rgb[1] >> (8 - pixelFormat.getGreenMaxBits())) & pixelFormat
						.getGreenMax()) << pixelFormat.getGreenShift()
				| ((rgb[2] >> (8 - pixelFormat.getBlueMaxBits())) & pixelFormat
						.getBlueMax()) << pixelFormat.getBlueShift();
	}

	private static int translateFormattedPixelToARGB(PixelFormat model,
			int pixel) {
		return (pixel >> model.getRedShift() & 0xff & model.getRedMax()) << 16
				| (pixel >> model.getGreenShift() & 0xff & model.getGreenMax()) << 8
				| (pixel >> model.getBlueShift() & 0xff & model.getBlueMax());
	}

	//
	// Testing
	//

	public static void test(int val, PixelFormat px) {
		String b = Integer.toBinaryString(val);
		String bh = Integer.toHexString(val);
		int tr = translateARGBToFormattedPixel(px, val);
		int trBack = translateFormattedPixelToARGB(px, tr);
		if (trBack != val) {
			throw new RuntimeException("Translated back wasn't the same");
		}

		String a = Integer.toBinaryString(tr);
		String ah = Integer.toHexString(tr);
		byte[] n = translateAndEncodePixel(px, val);
		int nv = decodeAndUntranslatePixel(n, 0, px);
		if (nv != val) {
			throw new RuntimeException("Decoded wasn't the same. Got " + nv
					+ " (" + Integer.toBinaryString(nv) + ") for " + val + " (" +Integer.toBinaryString(val) +")");
		}

		String pv = toHex(n);
		System.out.println(b + " (0x" + bh + ") = " + a + " (0x" + ah
				+ ") :: 0x" + pv);
	}

	private static String toHex(byte[] n) {
		String pv = "";
		for (byte z : n) {
			String h = Integer.toHexString((byte) z & 0x000000ff);
			if (h.length() == 1) {
				h = "0" + h;
			}
			pv += h;
		}
		return pv;
	}

	public static void main(String[] args) {

		System.out.println("-1 = " + (-1 & 0x00ffffff));

		PixelFormat pf = new PixelFormat();
		pf.setBigEndian(false);
		pf.setColorDepth(24);
		pf.setBitsPerPixel(32);
		pf.setRedShift(8);
		pf.setGreenShift(16);
		pf.setBlueShift(24);
		pf.setRedMax(255);
		pf.setGreenMax(255);
		pf.setBlueMax(255);
		ImageUtil.test(0x00AABBCC, pf);
		pf.setBigEndian(true);
		ImageUtil.test(0x00AABBCC, pf);

		pf = new PixelFormat();
		pf.setBigEndian(false);
		pf.setColorDepth(24);
		pf.setBitsPerPixel(32);
		pf.setRedShift(16);
		pf.setGreenShift(8);
		pf.setBlueShift(0);
		pf.setRedMax(255);
		pf.setGreenMax(255);
		pf.setBlueMax(255);
		ImageUtil.test(0x00010203, pf);
		ImageUtil.test(0x00FFFFFF, pf);
		ImageUtil.test(0x00000000, pf);
		ImageUtil.test(0x00FF0000, pf);
		ImageUtil.test(0x0000FF00, pf);
		ImageUtil.test(0x000000FF, pf);
		pf.setBigEndian(true);
		ImageUtil.test(0x00010203, pf);
		ImageUtil.test(0x00FFFFFF, pf);
		ImageUtil.test(0x00000000, pf);
		ImageUtil.test(0x00FF0000, pf);
		ImageUtil.test(0x0000FF00, pf);
		ImageUtil.test(0x000000FF, pf);

		pf = new PixelFormat();
		pf.setBigEndian(false);
		pf.setColorDepth(8);
		pf.setBitsPerPixel(8);
		pf.setRedShift(0);
		pf.setGreenShift(3);
		pf.setBlueShift(6);
		pf.setRedMax(7);
		pf.setGreenMax(7);
		pf.setBlueMax(3);

		ImageUtil.test(0x00010203, pf);
		pf.setBigEndian(true);
		ImageUtil.test(0x00010203, pf);
	}
}
