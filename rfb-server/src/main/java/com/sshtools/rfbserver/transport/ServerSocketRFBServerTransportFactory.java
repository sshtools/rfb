package com.sshtools.rfbserver.transport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.RFBServerConfiguration;

public class ServerSocketRFBServerTransportFactory implements RFBServerTransportFactory {
	final static Logger LOG = LoggerFactory.getLogger(RFBClient.class);

	protected ServerSocket serverSocket;
	protected RFBServerConfiguration configuration;

	public void start() throws IOException {
		if (isStarted()) {
			throw new IllegalStateException("Already started");
		}
		if(configuration.getAddress() == null) {
            LOG.info("Listening on all addresses port  " + configuration.getPort());
            serverSocket = new ServerSocket(configuration.getPort(), configuration.getListenBacklog());
		}
		else {
    		InetAddress byName = InetAddress.getByName(configuration.getAddress());
    		LOG.info("Listening on " + byName.getHostAddress() + ":" + configuration.getPort());
    		serverSocket = new ServerSocket(configuration.getPort(), configuration.getListenBacklog(),
    			byName);
		}
	}

	public void stop() {
		if (!isStarted()) {
			throw new IllegalStateException("Not started");
		}
		try {
			serverSocket.close();
		} catch (IOException e) {
		} finally {
			serverSocket = null;
		}
	}

	public void init(RFBServerConfiguration configuration) {
		this.configuration = configuration;
	}

	public RFBServerTransport nextTransport() throws IOException {
		if(serverSocket == null)
			return null;
		LOG.info("Waiting for connection");		
		return new SocketRFBServerTransport(serverSocket.accept());
	}

	public boolean isStarted() {
		return serverSocket != null;
	}

}
