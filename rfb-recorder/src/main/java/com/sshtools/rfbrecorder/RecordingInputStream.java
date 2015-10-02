package com.sshtools.rfbrecorder;

import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RecordingInputStream extends FilterInputStream {

	private DataOutputStream rec;

	public RecordingInputStream(InputStream in, DataOutputStream rec) {
		super(in);
		this.rec = rec;
	}

	@Override
	public int read() throws IOException {
		int r = super.read();
		if (r != -1) {
			writeBlock(new byte[] { (byte)r } , 0,  1);
		}
		return r;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int r = super.read(b);
		if (r != -1) {
			writeBlock(b, 0,  r);
		}
		return r;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int r = super.read(b, off, len);
		if (r != -1) {
			writeBlock(b , off,  r);
		}
		return r;
	}


	private void writeBlock(byte[] b, int off, int len) throws IOException {
		rec.writeBoolean(true);
		rec.writeLong(System.currentTimeMillis());
		rec.writeInt(len);
		rec.write(b, off, len);
	}

}