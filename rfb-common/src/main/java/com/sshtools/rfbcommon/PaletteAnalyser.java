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

public class PaletteAnalyser {

    private int[] palette;
    private byte[] index;
    private int[] count;
    private int[] key;
    private int size;

    private int singlePixels;
    private int runs;
    private int tileSize;
    private int maxSize;

    public PaletteAnalyser() {
        this(127, 4096);
    }

    public PaletteAnalyser(int maxSize, int tileSize) {
        this.tileSize = tileSize;
        this.maxSize = maxSize;
        reset();
    }

    public int getSize() {
        return size;
    }

    public int getForeground() {
        int bg = getBackground();
        int max = -1;
        int maxIndex = -1;
        for (int i = 0; i < count.length; i++) {
            if (count[i] > max && palette[i] != bg) {
                maxIndex = i;
                max = count[i];
            }
        }
        return maxIndex == -1 ? bg : palette[maxIndex];
    }

    public int getBackground() {
        int max = -1;
        int maxIndex = -1;
        for (int i = 0; i < count.length; i++) {
            if (count[i] > max) {
                maxIndex = i;
                max = count[i];
            }
        }
        return maxIndex == -1 ? -1 : palette[maxIndex];
    }

    public void reset() {
        palette = new int[maxSize];
        count = new int[maxSize];
        key = new int[maxSize + tileSize];
        index = new byte[maxSize + tileSize];
        for (int i = 0; i < index.length; i++) {
            index[i] = (byte) 0xff;
        }
        size = 0;
    }

    public void insert(int pix) {
        if (size < maxSize) {
            int i = hashPix(pix);
            while (index[i] != (byte) 0xff && key[i] != pix)
                i++;
            if (index[i] != (byte) 0xff)
                return;
            index[i] = (byte) size;
            key[i] = pix;
            palette[size] = pix;
            count[size] = count[size++];
        }
        size++;
    }

    public int lookup(int pix) {
        int i = hashPix(pix);
        if (size > maxSize) {
            throw new IllegalStateException();
        }
        while (index[i] != 255 && key[i] != pix)
            i++;
        if (index[i] != 255) {
            return index[i];
        }
        return -1;
    }

    private int hashPix(int pix) {
        return ((pix) ^ ((pix) >> 17)) & (tileSize - 1);
    }

    public int[] getPalette() {
        return palette;
    }

    public int getSinglePixels() {
        return singlePixels;
    }

    public int getRuns() {
        return runs;
    }

    public PaletteAnalyser analyse(int[] tileBuf, int len) {
        try {
            singlePixels = 0;
            runs = 0;
            int ptr = 0;
            while (ptr < len) {
                int pix = tileBuf[ptr];
                if (ptr < ( len - 1 ) && tileBuf[++ptr] != pix) {
                    singlePixels++;
                } else {
                    while (++ptr < len && tileBuf[ptr] == pix)
                        ;
                    runs++;
                }
                insert(pix);
            }
        } catch (ArrayIndexOutOfBoundsException aioube) {
            throw new RuntimeException("OOB for len " + len + " tileBuf.length=" + tileBuf.length, aioube);
        }
        return this;

    }
}
