package com.sshtools.rfbserver.encodings;


public class JPEGQualityLevel5 extends JPEGQualityLevel0 {

	public int getCode() {
		return getType().getCode() + 5;
	}

}
