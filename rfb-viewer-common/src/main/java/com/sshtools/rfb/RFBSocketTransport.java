/* HEADER */
package com.sshtools.rfb;

import java.io.IOException;

/**
 * A simple Socket <a href="RFBTransport.html">RFBTransport</a> for insecure
 * links.
 *
 * @author Lee David Painter
 */
public class RFBSocketTransport extends java.net.Socket implements RFBTransport {
	/**
	 * Connect the socket.
	 * 
	 * @param hostname
	 * @param port
	 * @throws IOException
	 */
	public RFBSocketTransport(String hostname, int port) throws IOException {
		super(hostname, port);
	}

	@Override
	public String getHostname() {
		return getInetAddress().getHostName();
	}
}
