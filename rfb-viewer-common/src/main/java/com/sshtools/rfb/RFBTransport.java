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
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Defines the attributes of a transport for the RFB protocol. Essentially this
 * allows RFB to run over any type of transport that can provide an InputStream
 * and OutputStream.
 *
 * @author Lee David Painter
 */
public interface RFBTransport {

	/**
	 * Get the hostname of the remote computer. This is for informational
	 * purposes only.
	 * 
	 * @return
	 */
	String getHostname();

	/**
	 * The InputStream for reading RFB data.
	 * 
	 * @return
	 * @throws IOException
	 */
	InputStream getInputStream() throws IOException;

	/**
	 * The OutputStream to write RFB data.
	 * 
	 * @return
	 * @throws IOException
	 */
	OutputStream getOutputStream() throws IOException;

	/**
	 * Close the connection and terminate the RFB protocol.
	 * 
	 * @throws IOException
	 */
	void close() throws IOException;

	/**
	 * Get the port on which this transport is connector, or <code>-1</code> if
	 * not applicable.
	 * 
	 * @return port
	 */
	int getPort();

}