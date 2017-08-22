package com.sshtools.rfbcommon;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class ProtocolWriter extends DataOutputStream {
	public ProtocolWriter(OutputStream out) {
		super(out);
	}

	public void writeTerminatedString(String str) throws UnsupportedEncodingException, IOException {
		writeUTF8String(str + '\0');
	}

	public void writeUTF8String(String message) throws UnsupportedEncodingException, IOException {
		byte[] buf = message == null ? null : message.getBytes("UTF-8");
		writeInt(buf == null ? 0 : buf.length);
		write(buf);
		flush();
	}

	public void writeCompactLen(int len) throws IOException {
		byte[] buf = new byte[3];
		int bytes = 0;
		buf[bytes++] = (byte) (len & 0x7F);
		if (len > 0x7F) {
			buf[bytes - 1] |= 0x80;
			buf[bytes++] = (byte) (len >> 7 & 0x7F);
			if (len > 0x3FFF) {
				buf[bytes - 1] |= 0x80;
				buf[bytes++] = (byte) (len >> 14 & 0xFF);
			}
		}
		write(buf, 0, bytes);
	}

	public void writeString(String message) throws UnsupportedEncodingException, IOException {
		byte[] buf = message == null ? null : message.getBytes("ASCII");
		writeInt(buf == null ? 0 : buf.length);
		write(buf);
		flush();
	}

	public void writeUInt32(long uint32) throws IOException {
		writeInt((int) uint32);
	}
}
