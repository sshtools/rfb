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
