/*
 */
package com.sshtools.rfb;

/**
 * Class to represent a buffer update
 * 
 * @author Lee David Painter
 */
public class BufferUpdate {
	int encoding;
	int x;
	int y;
	int w;
	int h;

	public BufferUpdate(int x, int y, int w, int h, int encoding) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.encoding = encoding;
	}

	public int getEncoding() {
		return encoding;
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

	@Override
	public String toString() {
		return "BufferUpdate [encoding=" + encoding + ", " + String.format("%010x", encoding) + " x=" + x + ", y=" + y
				+ ", w=" + w + ", h=" + h + "]";
	}
}