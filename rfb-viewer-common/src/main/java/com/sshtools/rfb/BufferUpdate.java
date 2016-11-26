/*
 */
package com.sshtools.rfb;

public class BufferUpdate extends RFBRectangle {
	public int encoding;

	public BufferUpdate(int x, int y, int w, int h, int encoding) {
		super(x, y, w, h);
		this.encoding = encoding;
	}

	public int getEncoding() {
		return encoding;
	}
}