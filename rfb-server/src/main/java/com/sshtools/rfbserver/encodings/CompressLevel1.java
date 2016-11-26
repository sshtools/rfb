package com.sshtools.rfbserver.encodings;


public class CompressLevel1 extends CompressLevel0 {

	public int getCode() {
		return getType().getCode() + 1;
	}

}
