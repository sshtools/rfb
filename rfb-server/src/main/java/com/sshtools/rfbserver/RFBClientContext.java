package com.sshtools.rfbserver;

import java.util.Collection;

import com.sshtools.rfbcommon.RFBVersion;
import com.sshtools.rfbserver.files.RFBServerFS;

public interface RFBClientContext {

	RFBServerConfiguration getConfiguration();

	RFBVersion getVersion();

	Collection<RFBAuthenticator> getSecurityHandlers();

	RFBServerFS getServerFileSystem();

	RFBAuthenticator getSecurityHandler(int selectedAuthentication);

}
