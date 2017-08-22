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
			public void resized(Rectangle newBounds, boolean clientInitiated) {
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
			public void damage(String name, Rectangle rectangle, boolean important, int preferredEncoding) {
				filteredDamageEvent(name, rectangle, important);
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

	protected void filteredDamageEvent(String name, Rectangle rectangle, boolean important) {
		fireDamageEvent(name, rectangle, important, -1);
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

	protected void filteredScreenBoundsChanged(Rectangle rectangle, boolean clientInitiated) {
		fireScreenBoundsChanged(rectangle, clientInitiated);
	}

	@Override
	public void resize(ScreenData screen) {
		underlyingDriver.resize(screen);
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
