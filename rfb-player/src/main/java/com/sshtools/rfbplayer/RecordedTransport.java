/**
 * RFB Player - Remote Frame Buffer Player. Plays files recorded by rfb-recorder
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
package com.sshtools.rfbplayer;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBTransport;

public class RecordedTransport implements RFBTransport {

	private RFBContext context;
	private String hostname;
	private int port;
	private PipedOutputStream inputsPipe;
	private PipedInputStream input;
	private PipedOutputStream output;
	private DataInputStream in;
	private PipedInputStream outputsPipe;
	private Thread pipeThread;
	private boolean open;

	public RecordedTransport(File file) throws IOException {
		in = new DataInputStream(new FileInputStream(file));
		hostname = in.readUTF();
		port = in.readInt();
		System.out.println("Host " + hostname + ":" + port);
		ObjectInputStream oin = new ObjectInputStream(in);
		try {
			context = (RFBContext) oin.readObject();
		} catch (ClassNotFoundException cnfe) {
			throw new IOException("Failed to read context.", cnfe);
		}
		System.out.println("Context: " + context);
		open = true;

	}

	public RFBContext getContext() {
		return context;
	}

	public String getHostname() {
		return hostname;
	}

	public InputStream getInputStream() throws IOException {
		if (input == null) {
			// Input pipe
			input = new PipedInputStream();
			inputsPipe = new PipedOutputStream(input);
			if (!isPiping()) {
				startPipe();
			}
		}
		return input;
	}

	public OutputStream getOutputStream() throws IOException {
		if (output == null) {
			output = new PipedOutputStream();
			outputsPipe = new PipedInputStream(output);
			if (!isPiping()) {
				startPipe();
			}
		}
		return output;
	}

	private boolean isPiping() {
		return pipeThread != null;
	}

	public void close() throws IOException {
		IOException ioe = null;
		open = false;
		try {
			input.close();
		} catch (IOException e) {
			ioe = e;
		}
		try {
			output.close();
		} catch (IOException e) {
			if (ioe == null) {
				ioe = e;
			}
		}
		try {
			in.close();
		} catch (IOException e) {
			if (ioe == null) {
				ioe = e;
			}
		}
		if (ioe != null) {
			throw ioe;
		}
	}

	public int getPort() {
		return port;
	}

	private void startPipe() {
		pipeThread = new Thread(new Runnable() {
			public void run() {
				try {
					long last = -1;
					long sleepTime = 0;
					long start = 0;
					while (open) {
						boolean input = in.readBoolean();
						long time = in.readLong();
						if (start == 0) {
							start = time;
						}
						if (last != -1) {
							sleepTime = time - last;
							Thread.sleep(sleepTime);
						}
						int len = in.readInt();
						byte[] b = new byte[len];
						in.read(b, 0, len);
						if (input) {
							inputsPipe.write(b, 0, len);
						} else {
							outputsPipe.read(b);
						}
						last = time;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		pipeThread.start();
	}
}