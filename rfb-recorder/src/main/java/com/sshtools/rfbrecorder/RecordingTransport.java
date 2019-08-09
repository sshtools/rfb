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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBTransport;

public class RecordingTransport implements RFBTransport {

	private RFBTransport underlyingTransport;
	private RecordingInputStream in;
	private DataOutputStream recordStream;
	private RecordingOutputStream out;

	public RecordingTransport(RFBContext context, File outputFile, RFBTransport underlyingTransport)
			throws IOException {
		this.underlyingTransport = underlyingTransport;

		
		recordStream = new DataOutputStream(
				new FileOutputStream(outputFile));

		recordStream.writeUTF(getHostname());
		recordStream.writeInt(getPort());
		
		ObjectOutputStream oos = new ObjectOutputStream(recordStream);
		oos.writeObject(context);
		oos.flush();
	}

	public String getHostname() {
		return underlyingTransport.getHostname();
	}

	public InputStream getInputStream() throws IOException {
		if(in == null) {
			in = new RecordingInputStream(underlyingTransport.getInputStream(),
					recordStream);
		}
		return in;
	}

	public OutputStream getOutputStream() throws IOException {
		if(out == null) {
			out = new RecordingOutputStream(
					underlyingTransport.getOutputStream(), recordStream);
		}
		return out;
	}

	public void close() throws IOException {
		underlyingTransport.close();
	}

	public int getPort() {
		return underlyingTransport.getPort();
	}
}