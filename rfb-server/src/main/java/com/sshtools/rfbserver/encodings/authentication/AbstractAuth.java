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
package com.sshtools.rfbserver.encodings.authentication;

import java.util.List;

import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.RFBAuthenticator;

public abstract class AbstractAuth implements RFBAuthenticator {
	private TightCapability cap;
	private int code;

	protected AbstractAuth(TightCapability cap) {
		this.cap = cap;
		this.code = cap.getCode();
	}
	
	protected AbstractAuth(int code) {
		this.code = code;
	}

	@Override
	public final TightCapability getCapability() {
		return cap;
	}

	@Override
	public final int getSecurityType() {
		return code;
	}

	@Override
	public List<Integer> getSubAuthTypes() {
		return null;
	}
}
