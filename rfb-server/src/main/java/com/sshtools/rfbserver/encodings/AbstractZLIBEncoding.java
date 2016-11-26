package com.sshtools.rfbserver.encodings;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZStream;
import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public abstract class AbstractZLIBEncoding extends AbstractRawEncoding {

    private int level = 5;

    private ZStream deflater;

    public AbstractZLIBEncoding() {
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isPseudoEncoding() {
        return false;
    }

    public synchronized void encode(UpdateRectangle<?> update, DataOutputStream dout, PixelFormat pixelFormat, RFBClient client)
                    throws IOException {

        boolean useJZLib = true;
        if (useJZLib) {
            jzlibEncode(update, dout, pixelFormat, client);
            return;
        }
        // VVV Doesn't work.. What is different between this an jzlib?

        @SuppressWarnings("unchecked")
        byte[] uncompressed = prepareEncode((UpdateRectangle<BufferedImage>) update, pixelFormat);
        int uncompressedLen = uncompressed.length;

        Deflater deflater = new Deflater(level, false);
        deflater.setInput(uncompressed);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(uncompressedLen);

        deflater.finish();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer); // returns the generated
            outputStream.write(buffer, 0, count);
        }
        outputStream.flush();
        outputStream.close();
        deflater.end();

        byte[] data = outputStream.toByteArray();
        dout.writeInt(getType().getCode());
        dout.writeInt(data.length);
        dout.write(data);
        dout.flush();
    }

    public synchronized void jzlibEncode(UpdateRectangle<?> update, DataOutputStream dout, PixelFormat pixelFormat, RFBClient client)
                    throws IOException {

        @SuppressWarnings("unchecked")
        long pstarted = System.currentTimeMillis();
        byte[] uncompressed = prepareEncode((UpdateRectangle<BufferedImage>) update, pixelFormat);
        int uncompressedLen = uncompressed.length;
        int maxCompressed = uncompressed.length + ((uncompressed.length + 99) / 100) + 12;
        byte[] tmpbuf = new byte[maxCompressed];

        if (deflater == null) {
            deflater = new ZStream();
            deflater.deflateInit(level);
        }

        ByteArrayOutputStream compressOut = new ByteArrayOutputStream(maxCompressed);
        deflater.next_in = uncompressed;
        deflater.next_in_index = 0;
        deflater.avail_in = uncompressedLen;
        int status;

        do {
            deflater.next_out = tmpbuf;
            deflater.next_out_index = 0;
            deflater.avail_out = maxCompressed;
            status = deflater.deflate(JZlib.Z_PARTIAL_FLUSH);
            switch (status) {
                case JZlib.Z_OK:
                    compressOut.write(tmpbuf, 0, maxCompressed - deflater.avail_out);
                    break;
                default:
                    throw new IOException("compress: deflate returnd " + status);
            }
        } while (deflater.avail_out == 0);

        // Write the actual message
        dout.writeInt(getType().getCode());
        byte[] out = compressOut.toByteArray();
        int length = out.length;
        dout.writeInt(length);
        dout.write(out);
    }

}
