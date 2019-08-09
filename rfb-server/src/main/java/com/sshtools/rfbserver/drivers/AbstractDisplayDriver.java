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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.rfbcommon.ScreenData;
import com.sshtools.rfbcommon.ScreenDetail;
import com.sshtools.rfbcommon.ScreenDimension;
import com.sshtools.rfbserver.DisplayDriver;
import com.sshtools.rfbserver.UpdateRectangle;

public abstract class AbstractDisplayDriver implements DisplayDriver {
	private List<DamageListener> listeners = new ArrayList<DamageListener>();
	private List<PointerListener> mouseListeners = new ArrayList<PointerListener>();
	private List<WindowListener> windowListeners = new ArrayList<WindowListener>();
	private List<UpdateListener> updateListeners = new ArrayList<UpdateListener>();
	private List<ScreenBoundsListener> screenChangeListeners = new ArrayList<ScreenBoundsListener>();
	private int mx = Integer.MIN_VALUE, my = Integer.MIN_VALUE;

	public void addScreenBoundsListener(ScreenBoundsListener listener) {
		screenChangeListeners.add(listener);
	}

	public void removeScreenBoundsListener(ScreenBoundsListener listener) {
		screenChangeListeners.remove(listener);
	}

	public void addUpdateListener(UpdateListener listener) {
		updateListeners.add(listener);
	}

	public void removeUpdateListener(UpdateListener listener) {
		updateListeners.remove(listener);
	}

	public void addWindowListener(WindowListener listener) {
		windowListeners.add(listener);
	}

	public void removeWindowListener(WindowListener listener) {
		windowListeners.remove(listener);
	}

	public void addDamageListener(DamageListener listener) {
		listeners.add(listener);
	}

	public void removeDamageListener(DamageListener listener) {
		listeners.remove(listener);
	}

	public void addPointerListener(PointerListener listener) {
		mouseListeners.add(listener);
	}

	public void removePointerListener(PointerListener listener) {
		mouseListeners.remove(listener);
	}

	@Override
	public boolean resize(ScreenData screen) {
		return false;
	}

	@Override
	public ScreenData getExtendedScreenData() {
		ScreenDimension dim = new ScreenDimension(getWidth(), getHeight());
		ScreenData sd = new ScreenData(dim);
		sd.getDetails().add(new ScreenDetail(0, 0, 0, dim, 0));
		return sd;
	}

	protected void mouseMoved(int x, int y) {
		if (x != mx || y != my) {
			mx = x;
			my = y;
			fireMouseEvent(x, y);
		}
	}

	protected void fireWindowMoved(String name, Rectangle area, Rectangle oldArea) {
		for (WindowListener l : new ArrayList<WindowListener>(windowListeners)) {
			l.moved(name, area, oldArea);
		}
	}

	protected void fireWindowCreated(String name, Rectangle area) {
		for (WindowListener l : new ArrayList<WindowListener>(windowListeners)) {
			l.created(name, area);
		}
	}

	protected void fireWindowClosed(String name, Rectangle area) {
		for (WindowListener l : new ArrayList<WindowListener>(windowListeners)) {
			l.closed(name, area);
		}
	}

	protected void fireWindowResized(String name, Rectangle area, Rectangle oldArea) {
		for (WindowListener l : new ArrayList<WindowListener>(windowListeners)) {
			l.resized(name, area, oldArea);
		}
	}

	protected void fireDamageEvent(String name, Rectangle area, int preferredEncoding) {
		for (DamageListener l : new ArrayList<DamageListener>(listeners)) {
			l.damage(name, area, preferredEncoding);
		}
	}

	protected void fireMouseEvent(int x, int y) {
		for (PointerListener l : new ArrayList<PointerListener>(mouseListeners)) {
			l.moved(x, y);
		}
	}

	protected void firePointerChange(PointerShape change) {
		for (PointerListener l : new ArrayList<PointerListener>(mouseListeners)) {
			l.pointerChange(change);
		}
	}

	protected void fireScreenBoundsChanged(ScreenData newBounds, boolean clientInitiated) {
		for (ScreenBoundsListener l : new ArrayList<ScreenBoundsListener>(screenChangeListeners)) {
			l.resized(newBounds, clientInitiated);
		}
	}

	protected void fireUpdate(UpdateRectangle<?> update) {
		for (UpdateListener l : new ArrayList<UpdateListener>(updateListeners)) {
			l.update(update);
		}
	}

	public String toString() {
		return getClass().getSimpleName();
	}
}
