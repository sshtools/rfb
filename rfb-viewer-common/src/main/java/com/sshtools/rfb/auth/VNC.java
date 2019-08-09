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

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBAuthenticationException;
import com.sshtools.rfb.SecurityType;
import com.sshtools.rfbcommon.AcmeDesCipher;
import com.sshtools.rfbcommon.RFBConstants;

public class VNC implements SecurityType {

	@Override
	public int process(ProtocolEngine engine) throws IOException,
			RFBAuthenticationException {
		byte[] challenge = new byte[16];
		try {
			engine.getInputStream().readFully(challenge);
		} catch (EOFException eof) {
			throw new RFBAuthenticationException("Incorrect password.");
		}

		char[] initialPassword = engine.getInitialPassword();
		if ((initialPassword == null || initialPassword.length == 0)
				&& engine.getPrompt() != null) {
			String pwString = engine.getPrompt()
					.passwordAuthenticationRequired();
			if (pwString == null) {
				throw new RFBAuthenticationException("Authentication cancelled.");
			}
			initialPassword = pwString == null ? null : pwString.toCharArray();
		}
		if (initialPassword == null || initialPassword.length == 0) {
			throw new RFBAuthenticationException("Password required.");
		}
		String pw = new String(initialPassword);
		if (pw.length() > 8) {
			pw = pw.substring(0, 8);
		}
		int firstZero = pw.indexOf(0);
		if (firstZero != -1) {
			pw = pw.substring(0, firstZero);
		}
		byte[] key = new byte[8];
		System.arraycopy(pw.getBytes(), 0, key, 0, pw.length());
		AcmeDesCipher des = new AcmeDesCipher(key);
		// des.encrypt(challenge, challenge);
		des.encrypt(challenge, 0, challenge, 0);
		des.encrypt(challenge, 8, challenge, 8);
		engine.getOutputStream().write(challenge);
		engine.getOutputStream().flush();
		return 1;
	}

	@Override
	public void postServerInitialisation(ProtocolEngine engine)
			throws IOException {
	}

	@Override
	public int getType() {
		return RFBConstants.SCHEME_VNC_AUTHENTICATION;
	}

	@Override
	public String toString() {
		return "VNC";
	}

    @Override
	public List<Integer> getSubAuthTypes() {
        return null;
    }

}
