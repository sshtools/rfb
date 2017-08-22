package com.sshtools.rfbserver.encodings;


public class JPEGQualityLevel8 extends JPEGQualityLevel0 {

	public int getCode() {
		return getType().getCode() + 8;
	}

}
