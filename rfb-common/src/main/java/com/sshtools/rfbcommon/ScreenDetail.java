package com.sshtools.rfbcommon;

public class ScreenDetail {
	private long id;
	private ScreenDimension dimension;
	private int x;
	private int y;
	private long flags;

	public ScreenDetail(long id, int x, int y, ScreenDimension dimension, long flags) {
		super();
		this.id = id;
		this.dimension = dimension;
		this.x = x;
		this.y = y;
		this.flags = flags;
	}

	public ScreenDetail(ScreenDetail other) {
		this.id = other.id;
		this.dimension = new ScreenDimension(other.dimension);
		this.x = other.x;
		this.y = other.y;
		this.flags = other.flags;
	}

	public long getId() {
		return id;
	}

	public ScreenDimension getDimension() {
		return dimension;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}
	
	public int getWidth() {
		return dimension.getWidth();
	}
	
	public int getHeight() {
		return dimension.getHeight();
	}

	public long getFlags() {
		return flags;
	}

}