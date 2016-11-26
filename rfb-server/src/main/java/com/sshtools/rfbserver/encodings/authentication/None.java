package com.sshtools.rfbserver.encodings.authentication;

import java.util.List;

import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.RFBAuthenticator;
import com.sshtools.rfbserver.RFBClient;

public class None implements RFBAuthenticator {

    public int getSecurityType() throws AuthenticationException {
        return RFBConstants.SCHEME_NO_AUTHENTICATION;
    }

    public TightCapability getCapability() {
        return RFBConstants.CAP_AUTH_NONE;
    }

    public boolean process(RFBClient rfbClient) throws AuthenticationException {
        return true;
    }

    public void postAuthentication(RFBClient rfbClient) {
    }


    public List<Integer> getSubAuthTypes() {
        return null;
    }
}
