/**
 * RFB - Remote Frame Buffer (VNC) implementation for Swing.
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
package com.sshtools.rfb.swing;

import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.sshtools.rfb.RFBToolkit.RFBColor;
import com.sshtools.rfb.RFBToolkit.RFBGraphicsContext;
import com.sshtools.rfb.RFBToolkit.RFBImage;
import com.sshtools.rfb.swing.SwingRFBToolkit.RFBAWTColor;
import com.sshtools.rfb.swing.SwingRFBToolkit.RFBBufferedImage;

public class RFBGraphics2D implements RFBGraphicsContext {
	private Graphics2D g2d;

	public RFBGraphics2D(Graphics2D g2d) {
		this.g2d = g2d;
	}

	@Override
	public void setColor(RFBColor pixel) {
		g2d.setColor(((RFBAWTColor) pixel).nativeColor);
	}

	@Override
	public void fillRect(int x, int y, int width, int height) {
		g2d.fillRect(x, y, width, height);
	}

	@Override
	public void copyArea(int x, int y, int width, int height, int dx, int dy) {
		g2d.copyArea(x, y, width, height, dx, dy);
	}

	@Override
	public void drawImage(RFBImage imageBuffer, int dx1, int dy1, int width, int height, int scaleMode) {
		g2d.drawImage(((RFBBufferedImage) imageBuffer).backing, dx1, dy1, width, height, null);
	}

	@Override
	public int[] getClipBounds() {
		Rectangle c = g2d.getClipBounds();
		return c == null ? null : new int[] { c.x, c.y, c.width, c.height };
	}

	@Override
	public void setClip(int[] clip) {
		if (clip == null)
			g2d.setClip(null);
		else
			g2d.setClip(clip[0], clip[1], clip[2], clip[3]);
	}

	@Override
	public void drawImage(RFBImage img, int x, int y) {
		g2d.drawImage(((RFBBufferedImage) img).backing, x, y, null);
	}
}