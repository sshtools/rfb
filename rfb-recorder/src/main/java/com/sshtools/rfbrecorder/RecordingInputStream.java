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