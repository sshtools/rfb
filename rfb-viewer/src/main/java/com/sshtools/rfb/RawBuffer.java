package com.sshtools.rfb;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;

import com.sshtools.rfbcommon.ImageUtil;
import com.sshtools.rfbcommon.PixelFormat;

public class RawBuffer {
	private int[] paletteInt;
	private short[] paletteShort;
	private byte[] paletteByte;

	private int[] dataInt;
	private short[] dataShort;
	private byte[] dataByte;

	private PixelFormat format;
	private int w;
	private int h;
	private int bytesPerPixel;
	private int len;

	private BufferedImage image;

	public RawBuffer(BufferedImage image, int bytesPerPixel, PixelFormat format) {
		this.format = format;
		this.image = image;
		this.w = image.getWidth();
		this.h = image.getHeight();
		this.bytesPerPixel = bytesPerPixel;
		this.len = w * h;
		switch (bytesPerPixel) {
		case 4:
		case 3:
			dataInt = ((DataBufferInt) image.getRaster().getDataBuffer())
					.getData();
			paletteInt = new int[256];
			break;
		case 2:
			dataShort = ((DataBufferUShort) image.getRaster().getDataBuffer())
					.getData();
			paletteShort = new short[256];
			break;
		case 1:
			dataByte = ((DataBufferByte) image.getRaster().getDataBuffer())
					.getData();
			paletteByte = new byte[256];
			break;
		}
	}

	public BufferedImage getImage() {
		return image;
	}

	public int readPalette(int paletteSize, byte[] bytes, int offset) {
		for (int i = 0; i < paletteSize; ++i) {
			if (dataInt != null) {
				paletteInt[i] = decode(bytes, offset + (i * bytesPerPixel));
			} else if (dataShort != null) {
				paletteShort[i] = (short) decode(bytes, offset
						+ (i * bytesPerPixel));
			} else {
				paletteByte[i] = (byte) decode(bytes, offset
						+ (i * bytesPerPixel));
			}
		}
		return paletteSize * bytesPerPixel;
	}

	public boolean isShort() {
		return dataShort != null;
	}

	public boolean isInt() {
		return dataInt != null;
	}

	public boolean isByte() {
		return dataInt != null;
	}

	public int fill(int offset, int len, int color) {
		int olen = len;
		if (dataInt != null) {
			while (len-- > 0) {
				dataInt[offset++] = color;
			}
		} else if (dataShort != null) {
			while (len-- > 0) {
				dataShort[offset++] = (short) color;
			}
		} else {
			while (len-- > 0) {
				dataByte[offset++] = (byte) color;
			}
		}
		return olen;
	}

	public void fillPalette(int index) {
		fillPalette(0, len, index);
	}

	public int fillPalette(int offset, int len, int index) {
		int olen = len;
		if (dataInt != null) {
			while (len-- > 0) {
				dataInt[offset++] = paletteInt[index];
			}
		} else if (dataShort != null) {
			while (len-- > 0) {
				dataShort[offset++] = paletteShort[index];
			}
		} else if (dataByte != null) {
			while (len-- > 0) {
				dataByte[offset++] = paletteByte[index];
			}
		} else {
			throw new IllegalStateException();
		}
		return olen;
	}

	public int draw(byte[] bytes, int offset, int width, int height) {
		int i = offset;
		for (int ly = 0; ly < height; ++ly) {
			int end = ly * this.w + width;
			if (dataInt != null) {
				for (int pixelsOffset = ly * this.w; pixelsOffset < end; ++pixelsOffset) {
					dataInt[pixelsOffset] = ImageUtil.decodePixel(bytes, i,
							format, bytesPerPixel);
					i += bytesPerPixel;
				}
			} else if (dataShort != null) {
				for (int pixelsOffset = ly * this.w; pixelsOffset < end; ++pixelsOffset) {
					dataShort[pixelsOffset] = ImageUtil.decodePixelShort(bytes,
							i, format);
					i += bytesPerPixel;
				}
			} else {
				for (int pixelsOffset = ly * this.w; pixelsOffset < end; ++pixelsOffset) {
					dataByte[pixelsOffset] = bytes[i++];
				}
			}
		}
		return i - offset;
	}

	public int[] getDataInt() {
		return dataInt;
	}

	public short[] getDataShort() {
		return dataShort;
	}

	public byte[] getDataByte() {
		return dataByte;
	}

	public int getWidth() {
		return w;
	}

	public int getHeight() {
		return h;
	}

	public int getBytesPerPixel() {
		return bytesPerPixel;
	}

	public int decode(byte[] bytes, int index) {
		if (dataInt != null) {
			return ImageUtil.decodePixel(bytes, index, format, bytesPerPixel);
		} else if (dataShort != null) {
			return ImageUtil.decodePixelShort(bytes, index, format);
		} else {
			return bytes[index];
		}
	}
}