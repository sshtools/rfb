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
package com.sshtools.rfbserver;

import java.io.IOException;
import java.util.List;

import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;

public interface RFBAuthenticator {

	public class NoAuthentication implements RFBAuthenticator {
		public int getSecurityType() {
			return RFBConstants.SCHEME_NO_AUTHENTICATION;
		}

		public boolean process(RFBClient rfbClient) {
		    return true;
		}

        public TightCapability getCapability() {
            return RFBConstants.CAP_AUTH_NONE;
        }

        public void postAuthentication(RFBClient rfbClient) {
        }

        @Override
        public List<Integer> getSubAuthTypes() {
            return null;
        }
	}

	public final static RFBAuthenticator NO_AUTHENTICATION = new NoAuthentication();

	public class AuthenticationException extends Exception {
		private static final long serialVersionUID = 1L;

		public AuthenticationException(String message) {
			super(message);
		}
	}
	
	TightCapability getCapability();

	int getSecurityType();

	boolean process(RFBClient rfbClient) throws AuthenticationException;

    void postAuthentication(RFBClient rfbClient) throws IOException;

    List<Integer> getSubAuthTypes();
}
