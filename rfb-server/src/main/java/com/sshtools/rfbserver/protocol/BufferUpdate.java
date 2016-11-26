package com.sshtools.rfbserver.protocol;

import java.awt.Rectangle;

public class BufferUpdate {
	private boolean incremental;
	private int x;
	private int y;
	private int w;
	private int h;

	public BufferUpdate(boolean incremental, int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.incremental = incremental;
	}

	public boolean isIncremental() {
		return incremental;
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

	public Rectangle getArea() {
		return new Rectangle(x, y, w, h);
	}

	@Override
	public String toString() {
		return "BufferUpdate [incremental=" + incremental + ", x=" + x + ", y=" + y + ", w=" + w + ", h=" + h + "]";
	}
}