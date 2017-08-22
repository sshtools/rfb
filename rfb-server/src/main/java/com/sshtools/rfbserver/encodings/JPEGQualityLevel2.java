package com.sshtools.rfbserver.encodings;


public class JPEGQualityLevel2 extends JPEGQualityLevel0 {

	public int getCode() {
		return getType().getCode() + 2;
	}

}
