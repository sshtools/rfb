package com.sshtools.rfbserver.encodings;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.sshtools.rfbcommon.ImageUtil;
import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public abstract class AbstractRawEncoding<D> extends AbstractEncoding<D> {

    public static class SubRect {
        public int pixel;
        public int x;
        public int y;
        public int w;
        public int h;
    }

    protected RFBClient client;
    protected ByteBuffer buf;

    public void rawEncode(UpdateRectangle<?> update, DataOutputStream dout, PixelFormat pixelFormat) throws IOException {
        @SuppressWarnings("unchecked")
        byte[] array = prepareEncode((UpdateRectangle<BufferedImage>) update, pixelFormat);
        dout.writeInt(getType().getCode());
        dout.write(array);
    }

    public String getVendor() {
        return null;
    }

    public void selected(RFBClient client) {
        this.client = client;
    }

    protected byte[] prepareEncode(UpdateRectangle<BufferedImage> update, PixelFormat pixelFormat) throws IOException {
        return prepareEncode(update.getData(), pixelFormat);
    }

    protected byte[] prepareEncode(BufferedImage img, PixelFormat pixelFormat) {
        DataBuffer dataBuffer = img.getData().getDataBuffer();
        if (dataBuffer instanceof DataBufferInt) {
            return prepareEncode(((DataBufferInt) dataBuffer).getData(), img.getWidth(), img.getHeight(), pixelFormat);
        } else if (dataBuffer instanceof DataBufferUShort) {
            return prepareEncode(((DataBufferUShort) dataBuffer).getData(), img.getWidth(), img.getHeight(), pixelFormat);
        } else if (dataBuffer instanceof DataBufferByte) {
            return prepareEncode(((DataBufferByte) dataBuffer).getData(), img.getWidth(), img.getHeight(), pixelFormat);
        } else {
            throw new UnsupportedOperationException("Unknown data buffer type.");
        }
    }

    protected byte[] prepareEncode(byte[] imageData, int width, int height, PixelFormat pixelFormat) {
        return imageData;
    }

    protected byte[] prepareEncode(short[] imageData, int width, int height, PixelFormat pixelFormat) {
        int len = width * height;
        int alloc = len * (pixelFormat.getBitsPerPixel() / 8);
        ByteBuffer buf = getByteBuffer(alloc);
        if (pixelFormat.isBigEndian()) {
            ShortBuffer ibuf = buf.asShortBuffer();
            ibuf.put(imageData);
        } else {
            buf.order(ByteOrder.LITTLE_ENDIAN);
            for (short s : imageData) {
                buf.putShort(s);
            }
        }
        return buf.array();
    }

    protected byte[] prepareEncode(int[] imageData, int width, int height, PixelFormat pixelFormat) {
        int len = width * height;
        int alloc = len * (pixelFormat.getBitsPerPixel() / 8);
        ByteBuffer buf = getByteBuffer(alloc);
        if (pixelFormat.isBigEndian()) {
            IntBuffer ibuf = buf.asIntBuffer();
            ibuf.put(imageData);
        } else {
            buf.order(ByteOrder.LITTLE_ENDIAN);
            for (int i : imageData) {
                buf.putInt(i);
            }
        }
        return buf.array();
    }

    protected void writeCPixel(DataOutput output, PixelFormat pixelFormat, int pixel) throws IOException {
        byte[] imgData = ImageUtil.translateAndEncodePixel(pixelFormat, pixel);
        boolean cpixelLS = pixelFormat.isFitsInLSCPIXEL();
        if ((cpixelLS && pixelFormat.isBigEndian()) || (!cpixelLS && !pixelFormat.isBigEndian())) {
            output.write(imgData[1]);
            output.write(imgData[2]);
            output.write(imgData[3]);
        } else {
            output.write(imgData[0]);
            output.write(imgData[1]);
            output.write(imgData[2]);
        }
    }

    protected void writePixel(DataOutput output, PixelFormat pixelFormat, int pixel) throws IOException {
        output.write(ImageUtil.translateAndEncodePixel(pixelFormat, pixel));
    }

    protected ByteBuffer getByteBuffer(int len) {
        if (buf == null || len != buf.capacity()) {
            buf = ByteBuffer.allocate(len);
        } else {
            buf.rewind();
        }
        // buf.rewind();
        // if (buf == null || len > buf.capacity()) {
        // buf = ByteBuffer.allocate(len);
        // }
        // else if(buf != null && len < buf.capacity()) {
        // buf.position(buf.capacity() - len);
        // buf = buf.slice();
        // buf.rewind();
        // }
        return buf;
    }
}
