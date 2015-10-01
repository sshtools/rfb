package com.sshtools.rfb.auth;

import java.io.IOException;
import java.util.List;

import com.sshtools.profile.AuthenticationException;
import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.SecurityType;
import com.sshtools.rfbcommon.RFBConstants;

public class None implements SecurityType {

	public int process(ProtocolEngine engine) throws AuthenticationException,
			IOException {
		return 1;
	}

	public int getType() {
		return RFBConstants.SCHEME_NO_AUTHENTICATION;
	}

	@Override
	public String toString() {
		return "None";
	}

	public void postServerInitialisation(ProtocolEngine engine)
			throws IOException {
	}

    public List<Integer> getSubAuthTypes() {
        return null;
    }

}
