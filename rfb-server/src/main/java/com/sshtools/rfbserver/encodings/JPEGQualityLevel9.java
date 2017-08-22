package com.sshtools.rfbserver.encodings;


public class JPEGQualityLevel9 extends JPEGQualityLevel0 {

	public int getCode() {
		return getType().getCode() + 9;
	}

}
