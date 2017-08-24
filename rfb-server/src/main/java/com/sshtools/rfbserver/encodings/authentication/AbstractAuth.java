package com.sshtools.rfbserver.encodings.authentication;

import java.util.List;

import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.RFBAuthenticator;

public abstract class AbstractAuth implements RFBAuthenticator {
	private TightCapability cap;
	private int code;

	protected AbstractAuth(TightCapability cap) {
		this.cap = cap;
		this.code = cap.getCode();
	}
	
	protected AbstractAuth(int code) {
		this.code = code;
	}

	@Override
	public final TightCapability getCapability() {
		return cap;
	}

	@Override
	public final int getSecurityType() {
		return code;
	}

	@Override
	public List<Integer> getSubAuthTypes() {
		return null;
	}
}
