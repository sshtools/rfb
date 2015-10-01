package com.sshtools.vnc;

import java.io.IOException;

import com.sshtools.profile.AuthenticationException;
import com.sshtools.profile.ProfileException;
import com.sshtools.profile.ProfileTransport;
import com.sshtools.profile.ResourceProfile;
import com.sshtools.profile.SchemeHandler;
import com.sshtools.profile.SchemeOptions;

/*
 */
public class VNCSchemeHandler extends SchemeHandler {
	public VNCSchemeHandler() {
		super("vnc", "VNC protocol");
	}

	public SchemeOptions createSchemeOptions() {
		return null;
	}

	public ProfileTransport createProfileTransport(ResourceProfile profile)
			throws ProfileException, IOException, AuthenticationException {
		return new VNCProtocolTransport();
	}
}