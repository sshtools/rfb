/*
 */
package com.sshtools.rfb;

public class RFBRectangle {
	public int x;
	public int y;
	public int w;
	public int h;

	public RFBRectangle(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getWidth() {
		return w;
	}

	public int getHeight() {
		return h;
	}

}