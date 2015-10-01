package com.sshtools.rfbrecorder;

import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RecordingOutputStream extends FilterOutputStream {
	private OutputStream out;
	private DataOutputStream rec;

	public RecordingOutputStream(OutputStream out, DataOutputStream rec) {
		super(out);
		this.out = out;
		this.rec = rec;
	}

	@Override
	public void write(byte[] b) throws IOException {
		out.write(b);
		writeBlock(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
		writeBlock(b, off, len);
	}

	@Override
	public void write(int b) throws IOException {
		out.write(b);
		writeBlock(new byte[] { (byte)b }, 0, 1);
	}

	private void writeBlock(byte[] b, int off, int len) throws IOException {
		rec.writeBoolean(false);
		rec.writeLong(System.currentTimeMillis());
		rec.writeInt(len);
		rec.write(b, off, len);
	}
}