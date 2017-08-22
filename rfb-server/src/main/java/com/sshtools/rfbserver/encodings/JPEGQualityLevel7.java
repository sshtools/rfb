package com.sshtools.rfbserver.encodings;


public class JPEGQualityLevel7 extends JPEGQualityLevel0 {

	public int getCode() {
		return getType().getCode() + 7;
	}

}
