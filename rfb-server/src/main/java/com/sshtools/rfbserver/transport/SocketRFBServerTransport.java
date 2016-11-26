package com.sshtools.rfbserver.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class SocketRFBServerTransport implements RFBServerTransport {

	private Socket socket;

	public SocketRFBServerTransport(Socket socket) {
		this.socket = socket;
	}

	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}

	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}

	public void stop() {
		try {
			socket.close();
		} catch (IOException e) {
		}		
	}

	public boolean isDisconnect(IOException e) {
		return getCause(e) instanceof SocketException;
	}
	
	private Throwable getCause(Throwable t) {
		Throwable cause = t.getCause();
		if(cause != null) {
			return getCause(cause);
		}
		return t;
	}

}
