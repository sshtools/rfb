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

/**
 * Callback interface that enables the RFB protocol to prompt the user for a
 * password.
 * 
 * @author Lee David Painter
 */
public interface RFBEventHandler {

	String passwordAuthenticationRequired();

	void connected();

	void disconnected();

	void remoteResize(int width, int height);

	void encodingChanged(RFBEncoding currentEncoding);
}