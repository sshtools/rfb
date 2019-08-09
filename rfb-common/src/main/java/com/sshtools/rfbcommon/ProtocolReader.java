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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class ProtocolReader extends DataInputStream {

	public ProtocolReader(InputStream in) {
		super(in);
	}

	public long readUInt32() throws IOException {
		long retVal = readInt();
		return retVal & 0x00000000FFFFFFFFL;
	}

	public String readTerminatedString() throws IOException {
		int len = readInt();
		String filename = "";
		if (len > 0) {
			byte[] b = new byte[len];
			readFully(b);
			filename = new String(b, 0, b.length - 1, "UTF-8");
		}
		return filename;
	}

	public byte[] readSizedArray() throws IOException {
		byte[] b = new byte[readInt()];
		readFully(b);
		return b;
	}

	public String readSizedUTF8() throws UnsupportedEncodingException,
			IOException {
		return new String(readSizedArray(), "UTF-8");
	}

	public String readASCII() throws UnsupportedEncodingException, IOException {
		return new String(readSizedArray(), "ASCII");
	}
	

	public int readCompactLen() throws IOException {
		int b = readUnsignedByte();
		int size = b & 0x7F;
		if ((b & 0x80) != 0) {
			b = readUnsignedByte();
			size += (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				size += readUnsignedByte() << 14;
			}
		}
		return size;
	}
	
	public int readCompactLen2() throws IOException {
		int[] portion = new int[3];
		portion[0] = readUnsignedByte();
		int len = portion[0] & 0x7F;
		if ((portion[0] & 0x80) != 0) {
			portion[1] = readUnsignedByte();
			len |= (portion[1] & 0x7F) << 7;
			if ((portion[1] & 0x80) != 0) {
				portion[2] = readUnsignedByte();
				len |= (portion[2] & 0xFF) << 14;
			}
		}
		return len;
	}
}
