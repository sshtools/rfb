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
