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

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.ScreenData;
import com.sshtools.rfbcommon.ScreenDimension;
import com.sshtools.rfbserver.DisplayDriver;
import com.sshtools.rfbserver.RFBClient;

/**
 * A driver that can be overlaid over another driver to grab only a certain
 * monitor, or an area of the whole desktop.
 * <p>
 * Damage events, mount events and pixel grabs are both translated based on the
 * bounds of either the chosen monitor or the chosen area.
 * 
 */
public class WindowedDisplayDriver extends FilteredDisplayDriver {
	final static Logger LOG = LoggerFactory.getLogger(WindowedDisplayDriver.class);
	private int monitor = -1;
	private Rectangle area;
	private GraphicsDevice[] devices;
	private DamageListener damageListener;
	private PointerListener pointerListener;

	public WindowedDisplayDriver(DisplayDriver underlyingDriver) {
		super(underlyingDriver, true);
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		devices = ge.getScreenDevices();
	}

	public int getMonitorCount() {
		return devices.length;
	}

	public int getMonitor() {
		return monitor;
	}

	public void setMonitor(int monitor) {
		this.monitor = monitor;
	}

	public Rectangle getArea() {
		return area;
	}

	public void setArea(Rectangle area) {
		this.area = area;
	}

	public int getWidth() {
		if (monitor == -1) {
			if (area == null) {
				return underlyingDriver.getWidth();
			} else {
				return area.width;
			}
		} else {
			return devices[monitor].getDisplayMode().getWidth();
		}
	}

	public int getHeight() {
		if (monitor == -1) {
			if (area == null) {
				return underlyingDriver.getHeight();
			} else {
				return area.height;
			}
		} else {
			return devices[monitor].getDisplayMode().getHeight();
		}
	}

	protected void filteredMouseEvent(int x, int y) {
		Rectangle nr = alterBounds(new Rectangle(x, y, 1, 1));
		if (nr != null) {
			fireMouseEvent(nr.x, nr.y);
		}
	}

	@Override
	protected void filteredPointChangeEvent(PointerShape change) {
		Rectangle nr = alterBounds(change.getBounds());
		if (nr != null) {
			change.setBounds(nr);
			firePointerChange(change);
		}
	}

	@Override
	protected void filteredDamageEvent(String name, Rectangle rectangle) {
		Rectangle bounds = alterBounds(rectangle);
		if (bounds != null) {
			LOG.debug("Virtual damage at: " + name + " / " + bounds);
			fireDamageEvent(name, bounds, -1);
		}
	}

	@Override
	protected void filteredWindowClosed(String name, Rectangle rectangle) {
		Rectangle bounds = alterBounds(rectangle);
		if (bounds != null) {
			fireWindowClosed(name, bounds);
		}
	}

	@Override
	protected void filteredWindowCreated(String name, Rectangle rectangle) {
		Rectangle bounds = alterBounds(rectangle);
		if (bounds != null) {
			fireWindowCreated(name, bounds);
		}
	}

	@Override
	protected void filteredWindowResized(String name, Rectangle rectangle, Rectangle oldRectangle) {
		Rectangle b1 = alterBounds(rectangle);
		Rectangle b2 = alterBounds(oldRectangle);
		if (b1 != null || b2 != null) {
			fireWindowResized(name, b1, b2);
		}
	}

	@Override
	protected void filteredWindowMoved(String name, Rectangle rectangle, Rectangle oldRectangle) {
		Rectangle b1 = alterBounds(rectangle);
		Rectangle b2 = alterBounds(oldRectangle);
		if (b1 != null || b2 != null) {
			fireWindowMoved(name, b1, b2);
		}
	}

	@Override
	protected void filteredScreenBoundsChanged(ScreenData rectangle, boolean clientInitiated) {
		Rectangle actualBounds = getActualBounds();
		// Shrink the viewport if the underlying driver now has a smaller screen
		if (rectangle.getWidth() < actualBounds.width && area != null) {
			area.width = rectangle.getWidth();
		}
		if (rectangle.getHeight() < actualBounds.height && area != null) {
			area.height = rectangle.getHeight();
		}
		fireScreenBoundsChanged(getScreenData(makeRelativeToBounds(getScreenBounds(rectangle), actualBounds)), clientInitiated);
	}

	public void mouseEvent(RFBClient client, int buttonMask, int x, int y) {
		if (monitor == -1) {
			if (area != null) {
				x += area.x;
				y += area.y;
			}
		} else {
			Rectangle screenBounds = devices[monitor].getDefaultConfiguration().getBounds();
			x += screenBounds.x;
			y += screenBounds.y;
		}
		underlyingDriver.mouseEvent(client, buttonMask, x, y);
	}

	public BufferedImage grabArea(Rectangle grabArea) {
		if (monitor == -1) {
			if (area != null) {
				grabArea = makeRelativeFromBounds(grabArea, area);
			}
		} else {
			grabArea = makeRelativeFromBounds(grabArea, devices[monitor].getDefaultConfiguration().getBounds());
		}
		return underlyingDriver.grabArea(grabArea);
	}

	protected Rectangle getActualBounds() {
		if (monitor == -1) {
			if (area != null) {
				return area;
			}
		} else {
			return devices[monitor].getDefaultConfiguration().getBounds();
		}
		return new Rectangle(0, 0, underlyingDriver.getWidth(), underlyingDriver.getHeight());
	}

	protected Rectangle alterBounds(Rectangle rectangle) {
		if (rectangle == null) {
			return null;
		}
		Rectangle damageArea = new Rectangle(rectangle);
		if (monitor == -1) {
			if (area != null) {
				damageArea = makeRelativeToBounds(damageArea, area);
			}
		} else {
			damageArea = makeRelativeToBounds(damageArea, devices[monitor].getDefaultConfiguration().getBounds());
		}
		return damageArea;
	}

	private Rectangle makeRelativeFromBounds(Rectangle area, Rectangle bounds) {
		Rectangle newArea = new Rectangle(area);
		newArea.x += bounds.x;
		newArea.y += bounds.y;
		return newArea;
	}

	private ScreenData getScreenData(Rectangle makeRelativeToBounds) {
		return new ScreenData(new ScreenDimension(makeRelativeToBounds.width, makeRelativeToBounds.height));
	}

	private Rectangle getScreenBounds(ScreenData data) {
		return new Rectangle(0, 0, data.getWidth(), data.getHeight());
	}

	private Rectangle makeRelativeToBounds(Rectangle area, Rectangle bounds) {
		if (area == null) {
			return null;
		}
		Rectangle newArea = new Rectangle(area);
		if (newArea.intersects(bounds)) {
			if (newArea.x + newArea.width > bounds.x + bounds.width) {
				newArea.width -= (newArea.x + newArea.width) - (bounds.x + bounds.width);
			}
			if (newArea.y + newArea.height > bounds.y + bounds.height) {
				newArea.height -= (newArea.y + newArea.height) - (bounds.y + bounds.height);
			}
			newArea.x -= bounds.x;
			if (newArea.x < 0) {
				newArea.width += newArea.x;
				newArea.x = 0;
			}
			newArea.y -= bounds.y;
			if (newArea.y < 0) {
				newArea.height += newArea.y;
				newArea.y = 0;
			}
		} else {
			return null;
		}
		return newArea;
	}

	public void destroy() {
		underlyingDriver.removeDamageListener(damageListener);
		underlyingDriver.removePointerListener(pointerListener);
		underlyingDriver.destroy();
	}

	public static void main(String[] args) {
		WindowedDisplayDriver wdd = new WindowedDisplayDriver(new AbstractDisplayDriver() {
			public void setClipboardText(String string) {
			}

			public void mouseEvent(RFBClient client, int buttonMask, int x, int y) {
			}

			public void keyEvent(RFBClient client, boolean down, int key) {
			}

			public void init() throws Exception {
			}

			public BufferedImage grabArea(Rectangle area) {
				return new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_ARGB);
			}

			public int getWidth() {
				return 800;
			}

			public PointerShape getPointerShape() {
				return new PointerShape();
			}

			public Point getPointerPosition() {
				return new Point(400, 400);
			}

			public int getHeight() {
				return 600;
			}

			public void destroy() {
			}
		});
		wdd.setArea(new Rectangle(100, 100, 100, 100));
		wdd.filteredWindowMoved("Test", new Rectangle(50, 50, 10, 10), new Rectangle(40, 40, 10, 10));
		wdd.filteredWindowMoved("Test", new Rectangle(95, 95, 10, 10), new Rectangle(40, 40, 10, 10));
		wdd.filteredWindowMoved("Test", new Rectangle(195, 195, 10, 10), new Rectangle(40, 40, 10, 10));
		wdd.filteredMouseEvent(50, 50);
		wdd.filteredMouseEvent(100, 100);
		wdd.filteredMouseEvent(110, 110);
		wdd.filteredMouseEvent(199, 199);
		wdd.filteredMouseEvent(200, 200);
		wdd.filteredMouseEvent(201, 201);
	}
}
