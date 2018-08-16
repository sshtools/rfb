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

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class PixelFormat {
	public enum Type {
		DIRECT, NATIVE
	}

	public final static PixelFormat DEFAULT_PIXEL_FORMAT = new PixelFormat();
	private boolean bigEndian = true;
	private boolean trueColour = true;
	private int redMax = 255;
	private int redShift = 16;
	private int greenMax = 255;
	private int greenShift = 8;
	private int blueMax = 255;
	private int blueShift = 0;
	private int bpp = 32;
	private int depth = 24;
	private int imageType = BufferedImage.TYPE_INT_ARGB;
	private Map<Integer, Integer> colorMap = new HashMap<Integer, Integer>();
	private int redMaxBits;
	private int greenMaxBits;
	private int blueMaxBits;
	private boolean fitsInLSCPIXEL;
	private boolean fitsInMSCPIXEL;
	private boolean supportsCPIXEL;

	public PixelFormat() {
		recalcPop();
	}

	public PixelFormat(PixelFormat base) {
		bigEndian = base.bigEndian;
		trueColour = base.trueColour;
		redMax = base.redMax;
		redShift = base.redShift;
		redMax = base.redMax;
		redShift = base.redShift;
		blueMax = base.blueMax;
		blueShift = base.blueShift;
		greenMax = base.greenMax;
		greenShift = base.greenShift;
		bpp = base.bpp;
		depth = base.depth;
		redMaxBits = base.redMaxBits;
		greenMaxBits = base.greenMaxBits;
		blueMaxBits = base.blueMaxBits;
		imageType = base.imageType;
		colorMap.putAll(base.colorMap);
		// type = base.type;
		recalcPop();
	}

	public boolean isDefault() {
		return this == DEFAULT_PIXEL_FORMAT || this.equals(DEFAULT_PIXEL_FORMAT);
	}

	public Map<Integer, Integer> getColorMap() {
		return colorMap;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (bigEndian ? 1231 : 1237);
		result = prime * result + blueMax;
		result = prime * result + blueShift;
		result = prime * result + bpp;
		result = prime * result + depth;
		result = prime * result + greenMax;
		result = prime * result + greenShift;
		result = prime * result + redMax;
		result = prime * result + redShift;
		result = prime * result + (trueColour ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PixelFormat other = (PixelFormat) obj;
		if (bigEndian != other.bigEndian)
			return false;
		if (blueMax != other.blueMax)
			return false;
		if (blueShift != other.blueShift)
			return false;
		if (bpp != other.bpp)
			return false;
		if (depth != other.depth)
			return false;
		if (greenMax != other.greenMax)
			return false;
		if (greenShift != other.greenShift)
			return false;
		if (redMax != other.redMax)
			return false;
		if (redShift != other.redShift)
			return false;
		if (trueColour != other.trueColour)
			return false;
		return true;
	}

	public boolean isBigEndian() {
		return bigEndian;
	}

	public void setBigEndian(boolean bigEndian) {
		this.bigEndian = bigEndian;
		recalcPop();
	}

	public boolean isTrueColor() {
		return trueColour;
	}

	public void setTrueColor(boolean trueColour) {
		this.trueColour = trueColour;
		recalcPop();
		imageType = getImageTypeForFormat();
	}

	public int getRedMax() {
		return redMax;
	}

	public void setRedMax(int redMax) {
		this.redMax = redMax;
		recalcPop();
	}

	public int getRedShift() {
		return redShift;
	}

	public void setRedShift(int redShift) {
		this.redShift = redShift;
		recalcPop();
	}

	public int getGreenMax() {
		return greenMax;
	}

	public void setGreenMax(int greenMax) {
		this.greenMax = greenMax;
		recalcPop();
	}

	public int getGreenShift() {
		return greenShift;
	}

	public void setGreenShift(int greenShift) {
		this.greenShift = greenShift;
		recalcPop();
	}

	public int getBlueMax() {
		return blueMax;
	}

	public void setBlueMax(int blueMax) {
		this.blueMax = blueMax;
		recalcPop();
	}

	public int getBlueShift() {
		return blueShift;
	}

	public void setBlueShift(int blueShift) {
		this.blueShift = blueShift;
		recalcPop();
	}

	public int getBitsPerPixel() {
		return bpp;
	}

	public void setBitsPerPixel(int bpp) {
		this.bpp = bpp;
		recalcPop();
		imageType = getImageTypeForFormat();
	}

	public int getColorDepth() {
		return depth;
	}

	public void setColorDepth(int depth) {
		this.depth = depth;
		recalcPop();
	}

	public void write(OutputStream out) throws IOException {
		DataOutputStream dout = new DataOutputStream(out);
		dout.write(bpp);
		dout.write(depth);
		dout.write(bigEndian ? 1 : 0);
		dout.write(trueColour ? 1 : 0);
		dout.writeShort(redMax); // Red max
		dout.writeShort(greenMax); // Green max
		dout.writeShort(blueMax); // Blue max
		dout.write(redShift); // Red shift
		dout.write(greenShift); // Green shift
		dout.write(blueShift); // Blue shift
		dout.write(new byte[3]); // Padding
	}

	public void read(InputStream in) throws IOException {
		DataInputStream din = new DataInputStream(in);
		bpp = din.read();
		depth = din.read();
		bigEndian = din.read() > 0;
		trueColour = din.read() > 0;
		redMax = din.readUnsignedShort();
		greenMax = din.readUnsignedShort();
		blueMax = din.readUnsignedShort();
		redShift = din.read();
		greenShift = din.read();
		blueShift = din.read();
		imageType = getImageTypeForFormat();
		din.readFully(new byte[3]);
		recalcPop();
	}

	public int getBytesPerPixel() {
		return bpp / 8;
	}

	public boolean isCompatibleFormat(int type) {
		return type == imageType || (type == BufferedImage.TYPE_INT_ARGB && imageType == BufferedImage.TYPE_INT_RGB)
				|| (type == BufferedImage.TYPE_INT_RGB && imageType == BufferedImage.TYPE_INT_ARGB);
	}

	// public Type getType() {
	// return type;
	// }
	//
	// public void setType(Type type) {
	// this.type = type;
	// }
	public int getImageTypeForFormat() {
		if (bpp == 32) {
			return BufferedImage.TYPE_INT_RGB;
		} else if (bpp == 24) {
			return BufferedImage.TYPE_3BYTE_BGR;
		} else if (bpp == 16) {
			return BufferedImage.TYPE_USHORT_565_RGB;
		} else if (bpp == 15) {
			return BufferedImage.TYPE_USHORT_555_RGB;
		} else if (bpp == 8) {
			return BufferedImage.TYPE_BYTE_INDEXED;
		}
		return 0;
	}

	// public void setImageType(int imageType) {
	// this.imageType = imageType;
	// type = Type.D;
	// switch (imageType) {
	// case BufferedImage.TYPE_INT_ARGB:
	// case BufferedImage.TYPE_INT_RGB:
	// if (depth == 24 && !bigEndian && bpp == 32 && redShift == 8 && greenShift
	// == 16 && blueShift == 24
	// && redMax == 0xff && greenMax == 0xff && blueMax == 0xff) {
	// type = Type.NATIVE;
	// }
	// ;
	// break;
	// case BufferedImage.TYPE_3BYTE_BGR:
	// if (depth == 24 && !bigEndian && bpp == 24 && redShift == 8 && greenShift
	// == 16 && blueShift == 0 && redMax == 0xff
	// && greenMax == 0xff && blueMax == 0xff) {
	// type = Type.NATIVE;
	// }
	// break;
	// case BufferedImage.TYPE_USHORT_565_RGB:
	// if (depth == 16 && !bigEndian && bpp == 16 && redShift == 0 && greenShift
	// == 5 && blueShift == 11 && redMax == 0x1f
	// && greenMax == 0x3f && blueMax == 0x1f) {
	// type = Type.NATIVE;
	// }
	// break;
	// case BufferedImage.TYPE_USHORT_555_RGB:
	// if (depth == 15 && !bigEndian && bpp == 16 && redShift == 0 && greenShift
	// == 5 && blueShift == 10 && redMax == 0x1f
	// && greenMax == 0x1f && blueMax == 0x1f) {
	// type = Type.NATIVE;
	// }
	// break;
	// case BufferedImage.TYPE_BYTE_INDEXED:
	// if (depth == 8 && bpp == 8 && !trueColour) {
	// type = Type.NATIVE;
	// }
	// break;
	// }
	// }
	public boolean setFromImageType(int imageType) {
		try {
			switch (imageType) {
			case BufferedImage.TYPE_INT_RGB:
			case BufferedImage.TYPE_INT_ARGB:
				bpp = 32;
				bigEndian = true;
				depth = 24;
				trueColour = true;
				redShift = 8;
				greenShift = 16;
				blueShift = 24;
				redMax = 0xff;
				greenMax = 0xff;
				blueMax = 0xff;
				break;
			case BufferedImage.TYPE_3BYTE_BGR:
				bpp = 24;
				bigEndian = true;
				depth = 24;
				trueColour = true;
				redShift = 0;
				greenShift = 8;
				blueShift = 16;
				redMax = 0xff;
				greenMax = 0xff;
				blueMax = 0xff;
				break;
			case BufferedImage.TYPE_USHORT_565_RGB:
				bigEndian = true;
				depth = 16;
				bpp = 16;
				trueColour = true;
				redShift = 11;
				greenShift = 5;
				blueShift = 0;
				redMax = 0x1f;
				greenMax = 0x3f;
				blueMax = 0x1f;
				break;
			case BufferedImage.TYPE_USHORT_555_RGB:
				bigEndian = true;
				depth = 15;
				bpp = 16;
				trueColour = true;
				redShift = 0;
				greenShift = 5;
				blueShift = 10;
				redMax = 0x1f;
				greenMax = 0x1f;
				blueMax = 0x1f;
				break;
			case BufferedImage.TYPE_BYTE_INDEXED:
				bigEndian = true;
				depth = 8;
				bpp = 8;
				trueColour = false;
				redShift = 0;
				greenShift = 0;
				blueShift = 0;
				redMax = 0;
				greenMax = 0;
				blueMax = 0;
				break;
			default:
				return false;
			}
			return true;
		} finally {
			recalcPop();
		}
	}

	public int getImageType() {
		return imageType;
	}

	@Override
	public String toString() {
		return "PixelFormat [bigEndian=" + bigEndian + ", trueColour=" + trueColour + ", redMax=" + redMax + ", redShift="
				+ redShift + ", greenMax=" + greenMax + ", greenShift=" + greenShift + ", blueMax=" + blueMax + ", blueShift="
				+ blueShift + ", bpp=" + bpp + ", depth=" + depth + ", imageType=" + imageType
				+ ", redMaxBits=" + redMaxBits + ", greenMaxBits=" + greenMaxBits + ", blueMaxBits=" + blueMaxBits + "]";
	}

	public int getRedMaxBits() {
		return redMaxBits;
	}

	public int getGreenMaxBits() {
		return greenMaxBits;
	}

	public int getBlueMaxBits() {
		return blueMaxBits;
	}

	public static int bitCount(int n) {
		int count = 0;
		for (int i = n; i != 0; i = i >> 1) {
			count += i & 1;
		}
		return count;
	}

	private void recalcPop() {
		redMaxBits = bitCount(redMax);
		greenMaxBits = bitCount(greenMax);
		blueMaxBits = bitCount(blueMax);
		fitsInLSCPIXEL = (redMax << redShift) < (1 << 24) && (greenMax << greenShift) < (1 << 24)
				&& (blueMax << blueShift) < (1 << 24);
		fitsInMSCPIXEL = redShift > 7 && greenShift > 7 && blueShift > 7;
		supportsCPIXEL = trueColour && bpp == 32 && depth <= 24 && (isFitsInLSCPIXEL() || isFitsInMSCPIXEL());
	}

	/**
	 * Get if this pixel format will fit in the least significant 3 bytes as a
	 * CPIXEL
	 * 
	 * @return fits in least significant 3 bytes as CPIXEL
	 */
	public boolean isFitsInLSCPIXEL() {
		return fitsInLSCPIXEL;
	}

	/**
	 * Get if this pixel format will fit in the most significant 3 bytes as a
	 * CPIXEL
	 * 
	 * @return fits in least significant 3 bytes as CPIXEL
	 */
	public boolean isFitsInMSCPIXEL() {
		return fitsInMSCPIXEL;
	}

	/**
	 * Get if this pixel format is compatible with the CPIXEL format. This is
	 * the same as a PIXEL for the agreed pixel format, except where
	 * true-colour-flag is non-zero, bits- per-pixel is 32, depth is 24 or less
	 * and all of the bits making up the red, green and blue intensities fit in
	 * either the least significant 3 bytes or the most significant 3 bytes. In
	 * this case a CPIXEL is only 3 bytes long, and contains the least
	 * significant or the most significant 3 bytes as appropriate.
	 * bytesPerCPixel is the number of bytes in a CPIXEL.
	 * 
	 * @return supports CPIXEL
	 */
	public boolean isSupportsCPIXEL() {
		return supportsCPIXEL;
	}

	/**
	 * Get the number of bytes a pixel will take, taking into account whether
	 * CPIXEL format can be used.
	 * 
	 * @return bytes per CPIXEL
	 */
	public int getBytesPerCPIXEL() {
		return isSupportsCPIXEL() ? 3 : (getBitsPerPixel() / 8);
	}
}
