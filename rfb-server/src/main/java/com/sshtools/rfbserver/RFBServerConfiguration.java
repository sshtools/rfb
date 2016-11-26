package com.sshtools.rfbserver;

public interface RFBServerConfiguration {
	int getPort();
	int getListenBacklog();
	String getAddress();
	String getDesktopName();
}
