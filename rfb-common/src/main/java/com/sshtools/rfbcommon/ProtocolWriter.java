package com.sshtools.rfbcommon;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class ProtocolWriter extends DataOutputStream {

	public ProtocolWriter(OutputStream out) {
		super(out);
	}

	public void writeTerminatedString(String str)
			throws UnsupportedEncodingException, IOException {
		writeUTF8String(str + '\0');
	}

	public void writeUTF8String(String message)
			throws UnsupportedEncodingException, IOException {
		byte[] buf = message == null ? null : message.getBytes("UTF-8");
		writeInt(buf == null ? 0 : buf.length);
		write(buf);
		flush();
	}

	public void writeString(String message)
			throws UnsupportedEncodingException, IOException {
		byte[] buf = message == null ? null : message.getBytes("ASCII");
		writeInt(buf == null ? 0 : buf.length);
		write(buf);
		flush();
	}

	public void writeUInt32(long uint32)
			throws IOException {
		writeInt((int) uint32);
	}
}
