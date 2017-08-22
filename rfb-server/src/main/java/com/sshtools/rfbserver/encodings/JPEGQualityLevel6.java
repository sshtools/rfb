package com.sshtools.rfbserver.encodings;


public class JPEGQualityLevel6 extends JPEGQualityLevel0 {

	public int getCode() {
		return getType().getCode() + 6;
	}

}
