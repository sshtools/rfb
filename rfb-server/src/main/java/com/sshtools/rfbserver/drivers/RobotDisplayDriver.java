package com.sshtools.rfbserver.drivers;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.keysym;

public class RobotDisplayDriver extends AbstractDisplayDriver implements ClipboardOwner {

	private static final int MOUSE_POLL_INTERVAL = 10;

	final static Logger LOG = LoggerFactory.getLogger(RobotDisplayDriver.class);

	private Robot robot;
	private int mouseState;
	private Timer timer;
	private int mousePollInterval = MOUSE_POLL_INTERVAL;
	

	public int getMousePollInterval() {
		return mousePollInterval;
	}

	public void setMousePollInterval(int mousePollInterval) {
		this.mousePollInterval = mousePollInterval;
	}

	public void init() throws Exception {
		robot = new Robot();
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				Point location = getPointerPosition();
				mouseMoved(location.x, location.y);
			}
		}, mousePollInterval, mousePollInterval);
	}

	public BufferedImage grabArea(Rectangle area) {
		long started = System.currentTimeMillis();
		BufferedImage createScreenCapture = robot.createScreenCapture(area);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Grab of " + area + " took " + (System.currentTimeMillis() - started) + "ms");
		}
		return createScreenCapture;
	}

	public int getWidth() {
		return Toolkit.getDefaultToolkit().getScreenSize().width;
	}

	public int getHeight() {
		return Toolkit.getDefaultToolkit().getScreenSize().height;
	}

	public void keyEvent(RFBClient client, boolean down, int key) {
		System.out.println("DownL " + down + " key = " + key);
		int[] vk = new int[2];
		keysym.toVKall( key, vk );
		System.out.println("vk[0]:"  + vk[0]);
		if( vk[0] != KeyEvent.VK_UNDEFINED )
		{
			if( down ) {
				robot.keyPress( vk[0] );
			}
			else {	
				robot.keyRelease( vk[0] );
			}
		}	
	}

	public void mouseEvent(RFBClient client, int buttonMask, int x, int y) {
		robot.mouseMove(x, y);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Mouse event: " + x + ", " + y + " =" + buttonMask);
		}
		for (int i = 1; i <= 5; i++) {
			int mask = getMaskForButton(i);
			if ((mouseState & mask) != (buttonMask & mask)) {
				if (i > 3) {
					sendMouseWheelEvent(buttonMask, 1);
				} else {
					sendMouseEvent(buttonMask, 1);
				}
			}
		}
		mouseState = buttonMask;

	}

	private int getMaskForButton(int i) {
		return (int) Math.pow(2, i);
	}

	private void sendMouseWheelEvent(int val, int button) {
		if ((val & getMaskForButton(button)) == 0) {
			robot.mouseWheel(button == 4 ? 1 : -1);
		}
	}

	private void sendMouseEvent(int val, int button) {
		if ((val & getMaskForButton(button)) == 0) {
			robot.mouseRelease(button);
		} else {
			robot.mousePress(button);
		}
	}

	public void setClipboardText(String string) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), this);
	}

	public void lostOwnership(Clipboard arg0, Transferable arg1) {
	}

	public void destroy() {
		timer.cancel();
	}

	public PointerShape getPointerShape() {
		try {
			BufferedImage fakeCursor = ImageIO.read(getClass().getResource("/images/pointer.png"));
			PointerShape pointer = new PointerShape();
			pointer.setHeight(fakeCursor.getHeight());
			pointer.setWidth(fakeCursor.getWidth());
			pointer.setHotX(3);
			pointer.setHotY(1);
			pointer.setData(fakeCursor);
			return pointer;
		} catch (IOException e1) {
			throw new RuntimeException("No cursor image.");
		}
	}

	public Point getPointerPosition() {
		PointerInfo pm = MouseInfo.getPointerInfo();
		Point location = pm.getLocation();
		return location;
	}
}
