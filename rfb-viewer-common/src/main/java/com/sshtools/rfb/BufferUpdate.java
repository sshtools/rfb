/**
 * RFB - Remote Frame Buffer (VNC) implementation.
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

	@Override
	public String toString() {
		return "BufferUpdate [encoding=" + encoding + ", x=" + x + ", y=" + y + ", w=" + w + ", h=" + h + "]";
	}
}