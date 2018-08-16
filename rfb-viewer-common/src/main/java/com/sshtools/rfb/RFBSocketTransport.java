/**
 * RFB - Remote Frame Buffer (VNC) implementation.
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
	 * @param hostname hostname
	 * @param port port
	 * @throws IOException on error
	 */
	public RFBSocketTransport(String hostname, int port) throws IOException {
		super(hostname, port);
	}

	@Override
	public String getHostname() {
		return getInetAddress().getHostName();
	}
}
