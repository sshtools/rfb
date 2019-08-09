/**
 * RFB Server - Remote Frame Buffer (VNC Server) implementation. This is the base module if you want to create a VNC server. It takes a layered driver approach to add native specific features (which is recommened as the cross-platform default "Robot" driver is very slow).
 *
 * See the vncserver module for a concrete server implementation that has some native performance improvements for some platforms.
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
package com.sshtools.rfbserver.drivers;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.sshtools.rfbcommon.ScreenData;
import com.sshtools.rfbserver.DisplayDriver;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public class FilteredDisplayDriver extends AbstractDisplayDriver {

	protected DisplayDriver underlyingDriver;
	private DamageListener damageListener;
	private PointerListener pointerListener;
	private boolean initAndDestroy;
	private ScreenBoundsListener screenBoundsListener;
	private WindowListener windowListener;
	private UpdateListener updateListener;

	public FilteredDisplayDriver(DisplayDriver underlyingDriver, boolean initAndDestroy) {
		this.underlyingDriver = underlyingDriver;
		this.initAndDestroy = initAndDestroy;
	}

	public void init() throws Exception {
		updateListener = new UpdateListener() {
			public void update(UpdateRectangle<?> update) {
				filteredUpdate(update);
			}
		};
		screenBoundsListener = new ScreenBoundsListener() {
			public void resized(ScreenData newBounds, boolean clientInitiated) {
				filteredScreenBoundsChanged(newBounds, clientInitiated);
			}
		};
		windowListener = new WindowListener() {

			public void resized(String name, Rectangle bounds, Rectangle oldBounds) {
				filteredWindowResized(name, bounds, oldBounds);
			}

			public void moved(String name, Rectangle bounds, Rectangle oldBounds) {
				filteredWindowMoved(name, bounds, oldBounds);
			}

			public void created(String name, Rectangle bounds) {
				filteredWindowCreated(name, bounds);
			}

			public void closed(String name, Rectangle bounds) {
				filteredWindowClosed(name, bounds);
			}
		};
		damageListener = new DamageListener() {
			public void damage(String name, Rectangle rectangle, int preferredEncoding) {
				filteredDamageEvent(name, rectangle);
			}
		};
		pointerListener = new PointerListener() {

			public void pointerChange(PointerShape change) {
				filteredPointChangeEvent(change);
			}

			public void moved(int x, int y) {
				filteredMouseEvent(x, y);
			}
		};
		if (initAndDestroy) {
			underlyingDriver.init();
		}
		underlyingDriver.addUpdateListener(updateListener);
		underlyingDriver.addScreenBoundsListener(screenBoundsListener);
		underlyingDriver.addWindowListener(windowListener);
		underlyingDriver.addDamageListener(damageListener);
		underlyingDriver.addPointerListener(pointerListener);
	}

	protected void filteredMouseEvent(int x, int y) {
		fireMouseEvent(x, y);
	}

	protected void filteredPointChangeEvent(PointerShape change) {
		firePointerChange(change);
	}

	protected void filteredDamageEvent(String name, Rectangle rectangle) {
		fireDamageEvent(name, rectangle, -1);
	}

	protected void filteredWindowClosed(String name, Rectangle rectangle) {
		fireWindowClosed(name, rectangle);
	}

	protected void filteredWindowCreated(String name, Rectangle rectangle) {
		fireWindowCreated(name, rectangle);
	}

	protected void filteredWindowResized(String name, Rectangle rectangle, Rectangle oldRectangle) {
		fireWindowResized(name, rectangle, oldRectangle);
	}

	protected void filteredWindowMoved(String name, Rectangle rectangle, Rectangle oldRectangle) {
		fireWindowMoved(name, rectangle, oldRectangle);
	}

	protected void filteredScreenBoundsChanged(ScreenData rectangle, boolean clientInitiated) {
		fireScreenBoundsChanged(rectangle, clientInitiated);
	}

	@Override
	public boolean resize(ScreenData screen) {
		return underlyingDriver.resize(screen);
	}

	protected void filteredUpdate(UpdateRectangle<?> update) {
		fireUpdate(update);
	}

	public int getWidth() {
		return underlyingDriver.getWidth();
	}

	public int getHeight() {
		return underlyingDriver.getHeight();
	}

	public void keyEvent(RFBClient client, boolean down, int key) {
		underlyingDriver.keyEvent(client, down, key);
	}

	public void mouseEvent(RFBClient client, int buttonMask, int x, int y) {
		underlyingDriver.mouseEvent(client, buttonMask, x, y);
	}

	public void setClipboardText(String string) {
		underlyingDriver.setClipboardText(string);
	}

	public BufferedImage grabArea(Rectangle area) {
		return underlyingDriver.grabArea(area);
	}

	public void destroy() {
		underlyingDriver.removeUpdateListener(updateListener);
		underlyingDriver.removeWindowListener(windowListener);
		underlyingDriver.removeScreenBoundsListener(screenBoundsListener);
		underlyingDriver.removeDamageListener(damageListener);
		underlyingDriver.removePointerListener(pointerListener);
		if (initAndDestroy) {
			underlyingDriver.destroy();
		}
	}

	public PointerShape getPointerShape() {
		return underlyingDriver.getPointerShape();
	}

	public Point getPointerPosition() {
		return underlyingDriver.getPointerPosition();
	}

	public String toString() {
		return getClass().getSimpleName() + "[" + underlyingDriver + "]";
	}

}
