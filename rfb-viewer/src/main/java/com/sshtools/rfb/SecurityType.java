package com.sshtools.rfb;

import java.io.IOException;
import java.util.List;

import com.sshtools.profile.AuthenticationException;

public interface SecurityType {

	int process(ProtocolEngine engine) throws AuthenticationException,
			IOException;

	int getType();

	void postServerInitialisation(ProtocolEngine engine) throws IOException;
	
	List<Integer> getSubAuthTypes();
}
