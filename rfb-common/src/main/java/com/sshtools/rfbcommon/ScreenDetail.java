/**
 * RFB Common - Remote Frame Buffer common code used both in client and server.
 * Copyright © 2006 SSHTOOLS Limited (support@sshtools.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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