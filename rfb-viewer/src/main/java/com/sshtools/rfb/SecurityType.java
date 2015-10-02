package com.sshtools.rfb;

import java.io.IOException;
import java.util.List;

public interface SecurityType {

	int process(ProtocolEngine engine) throws RFBAuthenticationException,
			IOException;

	int getType();

	void postServerInitialisation(ProtocolEngine engine) throws IOException;
	
	List<Integer> getSubAuthTypes();
}
