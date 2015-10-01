package com.sshtools.rfb;

import java.io.IOException;
import java.util.List;

public interface SecurityTypeFactory {

	SecurityType getSecurityType(int type);

	boolean isAvailable(int type);

	List<Integer> getSecurityTypes();

	int selectScheme(List<Integer> supportedServerTypes) throws IOException;
}
