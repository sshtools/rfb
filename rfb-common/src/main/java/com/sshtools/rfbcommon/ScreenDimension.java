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