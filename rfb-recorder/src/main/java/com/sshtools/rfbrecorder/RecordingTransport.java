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