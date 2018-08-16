/**
 * RFB Server - Remote Frame Buffer (VNC Server) implementation. This is the base module if you want to create a VNC server. It takes a layered driver approach to add native specific features (which is recommened as the cross-platform default "Robot" driver is very slow).
 *
 * See the vncserver module for a concrete server implementation that has some native performance improvements for some platforms.
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
package com.sshtools.rfbserver.encodings;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.ImageUtil;
import com.sshtools.rfbcommon.PaletteAnalyser;
import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.UpdateRectangle;

public class ZRLEEncoding extends AbstractZLIBEncoding {
    final static Logger LOG = LoggerFactory.getLogger(ZRLEEncoding.class);

    private final static int[] bitsPerPackedPixel = { 0, 1, 2, 2, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4 };

    private PaletteAnalyser paletteAnalyser;

    public ZRLEEncoding() {
        paletteAnalyser = new PaletteAnalyser();
    }

    public TightCapability getType() {
        return RFBConstants.CAP_ENC_ZRLE;
    }

    @Override
    protected byte[] prepareEncode(UpdateRectangle<BufferedImage> update, PixelFormat pixelFormat) throws IOException {
        BufferedImage img = ((UpdateRectangle<BufferedImage>) update).getData();
        int tileSize = 64;
        int tw = tileSize;
        int th = tileSize;
        int bpp = pixelFormat.getBytesPerCPIXEL();
        int[] tileBuf = new int[tw * th * 100];
        Rectangle r = update.getArea();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(tileBuf.length);
        DataOutputStream tdos = new DataOutputStream(baos);

        for (int ty = 0; ty < r.height; ty += tileSize) {
            tw = tileSize;
            th = tileSize;
            if (ty + th > r.height) {
                th = r.height - ty;
            }
            for (int tx = 0; tx < r.width; tx += tileSize) {
                if (tx + tw > r.width) {
                    tw = r.width - tx;
                }
                int ts = tw * th;

                img.getRGB(tx, ty, tw, th, tileBuf, 0, tw);

                paletteAnalyser.reset();
                paletteAnalyser.analyse(tileBuf, ts);

                /* Solid tile is a special case */
                int palSize = paletteAnalyser.getSize();
                if (palSize == 1) {
                    tdos.write(RFBConstants.ZRLE_SOLID);
                    writePixelOrCPIXEL(tdos, pixelFormat, paletteAnalyser.getPalette()[0]);
                } else {

                    boolean useRle = false;
                    boolean usePalette = false;

                    int estimatedBytes = ts * bpp;
                    if (pixelFormat.getBitsPerPixel() != 8 && getLevel() > 0 && (getLevel() & 0x80) == 0) {
                        estimatedBytes >>= getLevel();
                    }

                    int plainRleBytes = (bpp + 1) * (paletteAnalyser.getRuns() + paletteAnalyser.getSinglePixels());

                    if (plainRleBytes < estimatedBytes) {
                        useRle = true;
                        estimatedBytes = plainRleBytes;
                    }

                    if (palSize < 128) {
                        int paletteRleBytes = bpp * palSize + 2 * paletteAnalyser.getRuns() + paletteAnalyser.getSinglePixels();
                        if (paletteRleBytes < estimatedBytes && pixelFormat.getBitsPerPixel() != 8) {
                            useRle = true;
                            usePalette = true;
                            estimatedBytes = paletteRleBytes;
                        }

                        if (palSize < 17) {
                            int packedBytes = bpp * palSize + ts + bitsPerPackedPixel[palSize - 1] / 8;
                            if (packedBytes < estimatedBytes && pixelFormat.getBitsPerPixel() != 8) {
                                useRle = false;
                                usePalette = true;
                                estimatedBytes = packedBytes;
                            }
                        }
                    }
                    
                    if (!usePalette) {
                        palSize = 0;
                    }

                    tdos.write((useRle ? 128 : 0) | palSize);
                    for (int i = 0; i < palSize; i++) {
                        writePixelOrCPIXEL(tdos, pixelFormat, paletteAnalyser.getPalette()[i]);
                    }

                    if (useRle) {
                        int ptr = 0;
                        int pix;
                        int runStart;
                        while (ptr < ts) {
                            int len;
                            runStart = ptr;
                            pix = tileBuf[ptr++];
                            while (tileBuf[ptr] == pix && ptr < ts)
                                ptr++;
                            len = ptr - runStart;
                            if (len <= 2 && usePalette) {
                                int index = paletteAnalyser.lookup(pix);
                                if (len == 2) {
                                    tdos.write(index);
                                }
                                tdos.write(index);
                                continue;
                            }
                            if (usePalette) {
                                int index = paletteAnalyser.lookup(pix);
                                tdos.write(index | 128);
                            } else {
                                writePixelOrCPIXEL(tdos, pixelFormat, pix);
                            }
                            len -= 1;
                            while (len >= 255) {
                                tdos.write(255);
                                len -= 255;
                            }
                            tdos.write(len);
                        }
                    } else {
                        if (usePalette) {

                            // Work out how many bits to pack the palette into
                            int bits = bitsPerPackedPixel[palSize - 1];

                            int bitsRemaining = 8;
                            int idx = 0;
                            for (int ay = 0; ay < th; ay++) {
                                int val = 0;
                                for (int ax = 0; ax < tw; ax++) {
                                    int rgb = tileBuf[idx++];
                                    int index = paletteAnalyser.lookup(rgb);
                                    val = val | index;
                                    bitsRemaining -= bits;
                                    if (bitsRemaining > 0) {
                                        // Shift it for the next value to be
                                        // added
                                        val = val << bits;
                                    } else {
                                        tdos.write(val);
                                        bitsRemaining = 8;
                                    }
                                }
                                if (bitsRemaining != 8) {
                                    val = val << bitsRemaining;
                                    tdos.write(val);
                                }
                            }
                        } else {
                            writeRawTile(pixelFormat, img, tx, ty, tw, th, tdos);
                        }
                    }
                }
            }
        }
        tdos.flush();
        return baos.toByteArray();
    }

    private void writePixelOrCPIXEL(DataOutputStream tdos, PixelFormat pixelFormat, int pix) throws IOException {
        if (pixelFormat.isSupportsCPIXEL()) {
            writeCPixel(tdos, pixelFormat, pix);
        } else {
            writePixel(tdos, pixelFormat, pix);
        }
    }

    private void writeRawTile(PixelFormat pixelFormat, BufferedImage img, int x, int y, int tileWidth, int tileHeight,
                              DataOutputStream tdos) throws IOException {
        boolean supportsCPIXEL = pixelFormat.isSupportsCPIXEL();
        boolean cpixelLS = pixelFormat.isFitsInLSCPIXEL();
        byte[] imgData = prepareEncode(ImageUtil.copyImage(img.getSubimage(x, y, tileWidth, tileHeight), pixelFormat), pixelFormat);
        if (supportsCPIXEL) {
            for (int i = 0; i < imgData.length; i += 4) {
                if ((cpixelLS && pixelFormat.isBigEndian()) || (!cpixelLS && !pixelFormat.isBigEndian())) {
                    tdos.write(imgData[i + 1]);
                    tdos.write(imgData[i + 2]);
                    tdos.write(imgData[i + 3]);
                } else {
                    tdos.write(imgData[i]);
                    tdos.write(imgData[i + 1]);
                    tdos.write(imgData[i + 2]);
                }
            }
        } else {
            tdos.write(imgData);
        }
    }

}
