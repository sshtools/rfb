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
package com.sshtools.rfb.auth;

import java.io.IOException;
import java.util.List;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBAuthenticationException;
import com.sshtools.rfb.SecurityType;
import com.sshtools.rfbcommon.RFBConstants;

public class None implements SecurityType {

	@Override
	public int process(ProtocolEngine engine) throws RFBAuthenticationException,
			IOException {
		return 1;
	}

	@Override
	public int getType() {
		return RFBConstants.SCHEME_NO_AUTHENTICATION;
	}

	@Override
	public String toString() {
		return "None";
	}

	@Override
	public void postServerInitialisation(ProtocolEngine engine)
			throws IOException {
	}

    @Override
	public List<Integer> getSubAuthTypes() {
        return null;
    }

}
