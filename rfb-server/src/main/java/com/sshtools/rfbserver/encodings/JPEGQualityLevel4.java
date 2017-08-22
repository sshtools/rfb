package com.sshtools.rfbserver.encodings;


public class JPEGQualityLevel4 extends JPEGQualityLevel0 {

	public int getCode() {
		return getType().getCode() + 4;
	}

}
