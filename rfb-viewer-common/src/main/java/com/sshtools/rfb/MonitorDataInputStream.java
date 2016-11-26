/* HEADER */
package com.sshtools.rfb;

import java.io.IOException;
import java.io.InputStream;


class MonitorDataInputStream extends InputStream {

    private InputStream underlying;
    private boolean monitoring;
    private long idle;
    private long kbits;

    /**
     * @param in
     */
    public MonitorDataInputStream(InputStream underlying) {
        this.underlying = underlying;
        idle = 5;
        kbits = 0;
    }

    @Override
	public int read() throws IOException {
        long start = 0;
        if (monitoring)
            start = System.currentTimeMillis();
        int i = underlying.read();
        if (monitoring) {
            long end = System.currentTimeMillis();
            long waited = (end - start) * 10;
            int newKbits = i * 8 / 1000;
            if (waited > newKbits * 1000) {
                waited = newKbits * 1000;
            }
            if (waited < newKbits / 4) {
                waited = newKbits / 4;
            }
            idle += waited;
            kbits += newKbits;
        }
        return i;
    }

    @Override
	public int read(byte[] b, int off, int len) throws IOException {
        long start = 0;
        if (monitoring) {
            start = System.currentTimeMillis();
        }
        int i = underlying.read(b, off, len);
        if (monitoring) {
            long end = System.currentTimeMillis();
            long waited = (end - start) * 10;
            int newKbits = i * 8 / 1000;
            if (waited > newKbits * 1000) {
                waited = newKbits * 1000;
            }
            if (waited < newKbits / 4) {
                waited = newKbits / 4;
            }
            idle += waited;
            kbits += newKbits;
        }
        return i;
    }

    @Override
	public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
	public int available() throws IOException {
        return underlying.available();
    }

    @Override
	public void close() throws IOException {
        underlying.close();
    }

    @Override
	public synchronized void mark(int readlimit) {
        underlying.mark(readlimit);
    }

    @Override
	public boolean markSupported() {
        return underlying.markSupported();
    }

    @Override
	public synchronized void reset() throws IOException {
        underlying.reset();
    }

    @Override
	public long skip(long n) throws IOException {
        return underlying.skip(n);
    }

    public void setMonitoring(boolean monitoring) {
        this.monitoring = monitoring;
        if(monitoring) {
            if (idle > 10000) {
                kbits = kbits * 10000 / idle;
                idle = 10000;
            }
        }
        else {
            if (idle < kbits / 2) {
                idle = kbits / 2;
            }
        }
    }

    public long getSpeed() {
        return kbits * 10000 / idle;
    }

    public long timeWaited() {
        return idle;
    }
}