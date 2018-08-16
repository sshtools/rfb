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
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PixelFormatImageFactory {
	final static Logger LOG = Logger.getLogger(PixelFormatImageFactory.class.getName());
	private PixelFormat pixelFormat;

	public PixelFormatImageFactory(PixelFormat pixelFormat) {
		this.pixelFormat = pixelFormat;
	}

	public BufferedImage create(int width, int height) {
		ColorModel colorModel = null;
		int rmask = 0, gmask = 0, bmask = 0;
		if (!pixelFormat.isTrueColor()) {
			// TODO 16 bit indexed
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Creating Indexed 8 bit colour model");
			}
			Map<Integer, Integer> colors = pixelFormat.getColorMap();
			byte[] r = new byte[colors.size()];
			byte[] g = new byte[colors.size()];
			byte[] b = new byte[colors.size()];
			for (Map.Entry<Integer, Integer> en : colors.entrySet()) {
				r[en.getKey()] = (byte) (en.getValue() >> 16);
				g[en.getKey()] = (byte) (en.getValue() >> 8);
				b[en.getKey()] = (byte) (en.getValue() & 0xff);
			}
			colorModel = new IndexColorModel(8, colors.size(), r, g, b);
		} else {
			if (pixelFormat.getBitsPerPixel() == 8) {
				rmask = (0xff & pixelFormat.getRedMax()) << pixelFormat.getRedShift();
				gmask = (0xff & pixelFormat.getGreenMax()) << pixelFormat.getGreenShift();
				bmask = (0xff & pixelFormat.getBlueMax()) << pixelFormat.getBlueShift();
			} else {
				if (pixelFormat.getColorDepth() == 32) {
					rmask = 0xff << pixelFormat.getRedShift();
					gmask = 0xff << pixelFormat.getGreenShift();
					bmask = 0xff << pixelFormat.getBlueShift();
				} else {
					if (pixelFormat.getBitsPerPixel() == 16) {
						if (pixelFormat.getColorDepth() == 15) {
							rmask = (0xff & pixelFormat.getRedMax()) << pixelFormat.getRedShift();
							gmask = (0xff & pixelFormat.getGreenMax()) << pixelFormat.getGreenShift();
							bmask = (0xff & pixelFormat.getBlueMax()) << pixelFormat.getBlueShift();
						} else {
							rmask = (0xff & pixelFormat.getRedMax()) << pixelFormat.getRedShift();
							gmask = (0xff & pixelFormat.getGreenMax()) << pixelFormat.getGreenShift();
							bmask = (0xff & pixelFormat.getBlueMax()) << pixelFormat.getBlueShift();
						}
					} else {
						rmask = 0xff << pixelFormat.getRedShift();
						gmask = 0xff << pixelFormat.getGreenShift();
						bmask = 0xff << pixelFormat.getBlueShift();
					}
				}
			}
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Creating Direct " + pixelFormat.getColorDepth() + " bit colour model");
				LOG.fine("RED  : " + toBinaryString(pixelFormat.getColorDepth(), rmask));
				LOG.fine("GREEN: " + toBinaryString(pixelFormat.getColorDepth(), gmask));
				LOG.fine("BLUE : " + toBinaryString(pixelFormat.getColorDepth(), bmask));
			}
			colorModel = new DirectColorModel(pixelFormat.getColorDepth(), rmask, gmask, bmask);
		}
		switch (pixelFormat.getBitsPerPixel()) {
		case 8:
			if (pixelFormat.isTrueColor()) {
				byte[] pixels = new byte[width * height];
				DataBuffer dataBuffer = new DataBufferByte(pixels, width * height, 0);
				SampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_BYTE, width, height,
						new int[] { rmask, gmask, bmask });
				WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, null);
				return new BufferedImage(colorModel, raster, false, null);
			} else {
				return new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED);
			}
		case 15:
			return new BufferedImage(width, height, BufferedImage.TYPE_USHORT_555_RGB);
		case 16:
			short[] pixels = new short[width * height];
			DataBuffer dataBuffer = new DataBufferUShort(pixels, width * height, 0);
			SampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_USHORT, width, height,
					new int[] { rmask, gmask, bmask });
			WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, null);
			return new BufferedImage(colorModel, raster, false, null);
		case 24:
		case 32:
			int[] intPixels = new int[width * height];
			dataBuffer = new DataBufferInt(intPixels, width * height, 0);
			sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, width, height, new int[] { rmask, gmask, bmask });
			raster = Raster.createWritableRaster(sampleModel, dataBuffer, null);
			return new BufferedImage(colorModel, raster, false, null);
		default:
			throw new UnsupportedOperationException("Bits per pixel of " + pixelFormat.getBitsPerPixel() + " not supported.");
		}
	}

	private String toBinaryString(int s, int i) {
		return String.format("%" + s + "s", Integer.toBinaryString(i)).replace(' ', '0');
	}
}
