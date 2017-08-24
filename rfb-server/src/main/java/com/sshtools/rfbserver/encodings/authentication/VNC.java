package com.sshtools.rfbserver.encodings.authentication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import com.sshtools.rfbcommon.AcmeDesCipher;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbserver.RFBClient;

public abstract class VNC extends AbstractAuth {
	public VNC() {
		super(RFBConstants.CAP_AUTH_VNC);
	}

	public void postAuthentication(RFBClient rfbClient) throws IOException {
	}

	public boolean process(RFBClient rfbClient) throws AuthenticationException {
		try {
			DataOutputStream output = rfbClient.getOutput();
			byte[] challenge = new byte[16];
			for (int i = 0; i < 16; i++) {
				challenge[i] = (byte) (Math.random() * 256);
			}
			output.write(challenge);
			output.flush();
			DataInputStream din = rfbClient.getInput();
			byte[] result = new byte[16];
			din.readFully(result);
			byte[] passwordBytes = new String(getPassword()).getBytes("ASCII");
			if (passwordBytes.length != 8) {
				byte[] n = new byte[8];
				System.arraycopy(passwordBytes, 0, n, 0, Math.min(passwordBytes.length, n.length));
				passwordBytes = n;
			}
			AcmeDesCipher dec = new AcmeDesCipher(passwordBytes);
			dec.decrypt(result, 0, result, 0);
			dec.decrypt(result, 8, result, 8);
			if (!Arrays.equals(challenge, result)) {
				throw new AuthenticationException("Incorrect password.");
			}
		} catch (IOException ioe) {
			throw new AuthenticationException("I/O error during authentication.");
		}
		return true;
	}

	protected abstract char[] getPassword();
}
