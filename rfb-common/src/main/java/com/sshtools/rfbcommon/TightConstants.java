package com.sshtools.rfbcommon;

public interface TightConstants {
	final static int OP_FILL = 0x08;
	final static int OP_JPEG = 0x09;
	final static int OP_PNG  = 0x0A;
	final static int OP_COPY  = 0x00;
	
	final static int OP_READ_FILTER_ID = 0x40;
	
	final static int OP_FILTER_RAW = 0x00;
	final static int OP_FILTER_PALETTE = 0x01;
	final static int OP_FILTER_GRADIENT = 0x02;
}
