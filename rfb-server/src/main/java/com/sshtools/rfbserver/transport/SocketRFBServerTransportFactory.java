package com.sshtools.rfbserver.transport;

import java.io.IOException;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.RFBServerConfiguration;

public class SocketRFBServerTransportFactory implements RFBServerTransportFactory {
	final static Logger LOG = LoggerFactory.getLogger(RFBClient.class);

	private RFBServerConfiguration configuration;
	private Socket socket;
	private boolean started;

	public void start() throws IOException {
		if (isStarted()) {
			throw new IllegalStateException("Already started");
		}
		socket = new Socket(configuration.getAddress(), configuration.getPort());
		started = true;
	}

	public void stop() {
		if (!isStarted()) {
			throw new IllegalStateException("Not started");
		}
		try {
			socket.close();
		} catch (IOException e) {
		} finally {
			started = false;
		}
	}

	public void init(RFBServerConfiguration configuration) {
		this.configuration = configuration;
	}

	public RFBServerTransport nextTransport() throws IOException {
		if (socket == null) {
			throw new IOException("No more connections");
		}
		try {
			return new SocketRFBServerTransport(socket);
		} finally {
			socket = null;
		}
	}

	public boolean isStarted() {
		return started;
	}

}
