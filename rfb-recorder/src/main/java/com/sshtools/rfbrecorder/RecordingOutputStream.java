/**
 * RFB Recorder - Remote Frame Buffer Recorder.
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