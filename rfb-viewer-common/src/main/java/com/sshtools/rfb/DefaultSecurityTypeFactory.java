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
package com.sshtools.rfb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sshtools.rfb.auth.AnonTLS;
import com.sshtools.rfb.auth.None;
import com.sshtools.rfb.auth.Tight;
import com.sshtools.rfb.auth.VNC;
import com.sshtools.rfbcommon.RFBConstants;

public class DefaultSecurityTypeFactory implements SecurityTypeFactory {

	private final static List<Integer> availableTypes = Arrays.asList(
			RFBConstants.SCHEME_NO_AUTHENTICATION,
			RFBConstants.SCHEME_VNC_AUTHENTICATION,
			RFBConstants.SCHEME_TIGHT_AUTHENTICATION,
			RFBConstants.SCHEME_TLS_AUTHENTICATION
			);

	@Override
	public SecurityType getSecurityType(int type) {
	    if(!isAvailable(type)) {
	        throw new IllegalArgumentException("Not available.");
	    }
		switch (type) {
		case RFBConstants.SCHEME_NO_AUTHENTICATION:
			return new None();
		case RFBConstants.SCHEME_VNC_AUTHENTICATION:
			return new VNC();
		case RFBConstants.SCHEME_TIGHT_AUTHENTICATION:
			return new Tight();
		case RFBConstants.SCHEME_TLS_AUTHENTICATION:
			return new AnonTLS();
		default:
			throw new UnsupportedOperationException(
					"No security type implementation for " + type);
		}
	}

	@Override
	public int selectScheme(List<Integer> supportedServerTypes)
			throws IOException {
		int authScheme;
		List<Integer> supportedTypes = new ArrayList<Integer>();
		for (Integer serverType : getSecurityTypes()) {
			if (supportedServerTypes.contains(serverType)) {
				supportedTypes.add(serverType);
			}
		}
		int[] supportedSecurityTypes = new int[supportedTypes.size()];
		for (int i = 0; i < supportedTypes.size(); i++) {
			supportedSecurityTypes[i] = supportedTypes.get(i);
		}
		if (supportedSecurityTypes.length == 0) {
			authScheme = RFBConstants.SCHEME_NO_AUTHENTICATION;
		} else {
//			authScheme = supportedSecurityTypes[0];
			 authScheme = supportedSecurityTypes[supportedSecurityTypes.length
			 - 1];
		}
		return authScheme;
	}

	@Override
	public boolean isAvailable(int type) {
		return availableTypes.contains(type);
	}

	@Override
	public List<Integer> getSecurityTypes() {
		return availableTypes;
	}
}
