package com.sshtools.rfbserver.encodings;


public class JPEGQualityLevel1 extends JPEGQualityLevel0 {

	public int getCode() {
		return getType().getCode() + 1;
	}

}
