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

import com.sshtools.rfbserver.RFBServerConfiguration;

public interface RFBServerTransportFactory {
	/**
	 * Get the next transport.  This should block until a transport
	 * is available. When there will be no more transports available,
	 * <code>null</code> will be returned. 
	 * 
	 * @return transport
	 * @throws IOException if transport cannot be obtained
	 */
	RFBServerTransport nextTransport() throws IOException;
	
	/**
	 * Initialise the transport factory with the configuration required.
	 * 
	 * @param configuration configuration
	 */
	void init(RFBServerConfiguration configuration);
	
	/**
	 * Get if the transport factory is started.
	 * 
	 * @return started
	 */
	boolean isStarted();

	/**
	 * Start accepting connections.
	 * 
	 * @throws IOException if no connections can be accepted
	 */
	void start() throws IOException;

	/**
	 * Stop accepting connections.
	 */
	void stop();
}
