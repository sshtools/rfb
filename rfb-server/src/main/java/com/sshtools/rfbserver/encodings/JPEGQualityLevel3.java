package com.sshtools.rfbserver.encodings;


public class JPEGQualityLevel3 extends JPEGQualityLevel0 {

	public int getCode() {
		return getType().getCode() + 3;
	}

}
