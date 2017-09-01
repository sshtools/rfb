package com.sshtools.rfbserver;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.sshtools.rfbcommon.ScreenData;

public interface DisplayDriver {

	public class PointerShape {
		private int width;
		private int height;
		private int x;
		private int y;
		private int hotX;
		private int hotY;
		private BufferedImage data;

		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		public int getX() {
			return x;
		}

		public void setX(int x) {
			this.x = x;
		}

		public int getY() {
			return y;
		}

		public void setY(int y) {
			this.y = y;
		}

		public int getHotX() {
			return hotX;
		}

		public void setHotX(int hotX) {
			this.hotX = hotX;
		}

		public int getHotY() {
			return hotY;
		}

		public void setHotY(int hotY) {
			this.hotY = hotY;
		}

		public BufferedImage getData() {
			return data;
		}

		public void setData(BufferedImage data) {
			this.data = data;
		}

		@Override
		public String toString() {
			return "PointerChange [width=" + width + ", height=" + height + ", x=" + x + ", y=" + y + ", hotX=" + hotX + ", hotY="
				+ hotY + "]";
		}

		public Rectangle getBounds() {
			return new Rectangle(x, y, width, height);
		}

		public void setBounds(Rectangle bounds) {
			x = bounds.x;
			y = bounds.y;
			width = bounds.width;
			height = bounds.height;
		}

	}

	public interface ScreenBoundsListener {
		void resized(ScreenData newBounds, boolean clientInitiated);
	}

	public interface WindowListener {
		void created(String name, Rectangle bounds);

		void moved(String name, Rectangle newBounds, Rectangle oldBounds);

		void resized(String name, Rectangle bounds, Rectangle oldBounds);

		void closed(String name, Rectangle bounds);
	}

	public interface DamageListener {
		void damage(String name, Rectangle rectangle, int preferredEncoding);
	}

	public interface PointerListener {
		void moved(int x, int y);

		void pointerChange(PointerShape change);
	}
	
	public interface UpdateListener {
		void update(UpdateRectangle<?> update);
	}

	PointerShape getPointerShape();

	Point getPointerPosition();

	void addUpdateListener(UpdateListener listener);

	void removeUpdateListener(UpdateListener listener);

	void addWindowListener(WindowListener listener);

	void removeWindowListener(WindowListener listener);

	void addScreenBoundsListener(ScreenBoundsListener listener);

	void removeScreenBoundsListener(ScreenBoundsListener listener);

	void addDamageListener(DamageListener listener);

	void removeDamageListener(DamageListener listener);

	void addPointerListener(PointerListener listener);

	void removePointerListener(PointerListener listener);

	void init() throws Exception;

	void destroy();

	int getWidth();

	int getHeight();

	void keyEvent(RFBClient client, boolean down, int key);

	void mouseEvent(RFBClient client, int buttonMask, int x, int y);

	void setClipboardText(String string);

	BufferedImage grabArea(Rectangle area);
	
	boolean resize(ScreenData screen);

	ScreenData getExtendedScreenData();
}
