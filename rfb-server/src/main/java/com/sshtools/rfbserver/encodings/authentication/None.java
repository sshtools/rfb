package com.sshtools.rfbserver.encodings.authentication;

import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbserver.RFBClient;

public class None extends AbstractAuth {
	public None() {
		super(RFBConstants.CAP_AUTH_NONE);
	}

	public boolean process(RFBClient rfbClient) throws AuthenticationException {
		return true;
	}

	public void postAuthentication(RFBClient rfbClient) {
	}
}
