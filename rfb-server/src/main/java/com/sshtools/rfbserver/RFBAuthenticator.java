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
