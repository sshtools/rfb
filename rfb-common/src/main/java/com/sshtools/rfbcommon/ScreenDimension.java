package com.sshtools.rfbcommon;

public class ScreenDimension {
	private int width;
	private int height;

	public ScreenDimension(ScreenDimension other) {
		this(other.width, other.height);
		
	}

	public ScreenDimension(int width, int height) {
		super();
		this.width = width;
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	@Override
	public String toString() {
		return "ScreenDimension [width=" + width + ", height=" + height + "]";
	}

	public void reset() {
		width = 0;
		height = 0;		
	}

	public boolean isEmpty() {
		return width == 0 || height == 0;
	}

	public void set(ScreenDimension dimension) {
		this.width = dimension.width;
		this.height = dimension.height;
	}
}