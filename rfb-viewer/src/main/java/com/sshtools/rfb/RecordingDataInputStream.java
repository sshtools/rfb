/* HEADER */
package com.sshtools.rfb;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


class RecordingDataInputStream extends InputStream {

    private InputStream underlying;
    private boolean monitoring;
    private long idle;
    private long kbits;
    private DataOutputStream recordingOutputStream;

    /**
     * @param in
     */
    public RecordingDataInputStream(InputStream underlying) {
        this.underlying = underlying;
        idle = 5;
        kbits = 0;
    }

    public int read() throws IOException {
        int i = underlying.read();
        if(i != -1) {
            writeResponse(i);
        }
        return i;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int i = underlying.read();
        if(i != -1) {
            writeResponse(b, off, i);
        }
        return i;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int available() throws IOException {
        return underlying.available();
    }

    public void close() throws IOException {
        underlying.close();
    }

    public synchronized void mark(int readlimit) {
        underlying.mark(readlimit);
    }

    public boolean markSupported() {
        return underlying.markSupported();
    }

    public synchronized void reset() throws IOException {
        underlying.reset();
    }

    public long skip(long n) throws IOException {
        return underlying.skip(n);
    }
    
    public synchronized void setRecordingStream(OutputStream out) {
        recordingOutputStream = new DataOutputStream(new BufferedOutputStream(out));
    }
    
    synchronized void writeResponse(int i) throws IOException {
        if(recordingOutputStream != null) {
            writeHeader(1);
            recordingOutputStream.write(i);
        }
    }
    
    
    synchronized void writeResponse(byte[] b, int off, int len) throws IOException {
        if(recordingOutputStream != null) {
            writeHeader(len);
            recordingOutputStream.write(b, off, len);
        }
        
    }
    
    void writeHeader(int bytes) throws IOException {
        recordingOutputStream.writeLong(System.currentTimeMillis());
        recordingOutputStream.writeInt(bytes);
    }
}