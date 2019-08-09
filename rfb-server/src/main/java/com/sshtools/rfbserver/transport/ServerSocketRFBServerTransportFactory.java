/**
 * RFB Server - Remote Frame Buffer (VNC Server) implementation. This is the base module if you want to create a VNC server. It takes a layered driver approach to add native specific features (which is recommened as the cross-platform default "Robot" driver is very slow).
 *
 * See the vncserver module for a concrete server implementation that has some native performance improvements for some platforms.
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
