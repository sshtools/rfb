package com.sshtools.rfbserver;


public class FixedRFBServerConfiguration implements RFBServerConfiguration {

	public int getPort() {
		return 6900;
	}

	public String getAddress() {
		return "0.0.0.0";
	}

	public int getListenBacklog() {
		return 1;
	}

	public String getDesktopName() {
		return "JavaRFB";
	}

}
