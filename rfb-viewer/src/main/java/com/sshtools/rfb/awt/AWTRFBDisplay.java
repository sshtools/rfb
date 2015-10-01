/* HEADER */
package com.sshtools.rfb.awt;

import java.awt.AWTEvent;
import java.awt.AWTEventMulticaster;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBDisplayModel;
import com.sshtools.rfb.RFBEventHandler;
import com.sshtools.rfb.RFBTransport;
import com.sshtools.ui.awt.UIUtil;

public class AWTRFBDisplay extends Canvas implements RFBDisplay {

	ProtocolEngine engine;
	KeyListener keyListener;
	RFBContext context;
	RFBDisplayModel displayModel;
	Rectangle updateRect;

	public AWTRFBDisplay() {
		super();
		enableEvents(AWTEvent.FOCUS_EVENT_MASK | AWTEvent.ACTION_EVENT_MASK
			| AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);

		displayModel = new RFBDisplayModel(this);

		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				if (engine != null && context.getScaleMode() != NO_SCALING
					&& displayModel.getRfbWidth() != 0
					&& displayModel.getRfbHeight() != 0) {
					displayModel.updateBuffer();
				}
			}
		});

		try {
			Class[] params = new Class[] { boolean.class };
			AWTRFBDisplay.class.getMethod("setFocusable", params).invoke(this,
				new Object[] { Boolean.TRUE });
			AWTRFBDisplay.class.getMethod("setFocusTraversalKeysEnabled",
				params).invoke(this, new Object[] { Boolean.FALSE });

		} catch (Throwable t) {
			System.err
				.println("unable to reset focus handling for java version ");
			t.printStackTrace();
		}

	}

	public void initialiseSession(RFBTransport transport, RFBContext context,
			RFBEventHandler prompt) {
		this.context = context;

		if (engine != null) {
			removeKeyListener(engine);
		}
		context.resetEncodings();
		displayModel.init();
		displayModel.setContext(context);
		Image empty = null;
		Image dot = null;
		// gifs must be used for cursors on windows, but pngs on linux ..
		// argggh!
		if (System.getProperty("os.name").toLowerCase().startsWith("linux")) {
			empty = UIUtil.loadImage(this.getClass(),
				"/images/empty-cursor.png");
			dot = UIUtil.loadImage(this.getClass(), "/images/dot-cursor.png");
		} else {
			empty = UIUtil.loadImage(this.getClass(),
				"/images/empty-cursor.gif");
			dot = UIUtil.loadImage(this.getClass(), "/images/dot-cursor.gif");
		}
		engine = new ProtocolEngine(this, transport, context, prompt,
			displayModel, empty, dot);
		addKeyListener(engine);
	}

	public RFBContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Component#addKeyListener(java.awt.event.KeyListener)
	 */
	public void addKeyListener(KeyListener listener) {
		keyListener = AWTEventMulticaster.add(keyListener, listener);
		enableEvents(AWTEvent.KEY_EVENT_MASK);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Component#removeKeyListener(java.awt.event.KeyListener)
	 */
	public void removeKeyListener(KeyListener listener) {
		keyListener = AWTEventMulticaster.remove(keyListener, listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Component#processKeyEvent(java.awt.event.KeyEvent)
	 */
	public void processKeyEvent(KeyEvent evt) {
		if (keyListener != null) {
			switch (evt.getID()) {
			case KeyEvent.KEY_PRESSED:
				keyListener.keyPressed(evt);
				break;
			case KeyEvent.KEY_RELEASED:
				keyListener.keyReleased(evt);
				break;
			case KeyEvent.KEY_TYPED:
				keyListener.keyTyped(evt);
				break;
			}
		}
		// consume TAB keys if they originate from our component
		if ((evt.getKeyCode() == KeyEvent.VK_TAB) && (evt.getSource() == this)) {
			evt.consume();
		}
		super.processKeyEvent(evt);
	}

	public Dimension getPreferredSize() {
		return new Dimension(
			engine != null && engine.isConnected() ? displayModel.getRfbWidth()
				: 600, engine != null && engine.isConnected() ? displayModel
				.getRfbHeight() : 480);
	}

	public Dimension getMinimumSize() {
		return new Dimension(
			engine != null && engine.isConnected() ? displayModel.getRfbWidth()
				: 600, engine != null && engine.isConnected() ? displayModel
				.getRfbHeight() : 480);
	}

	public Dimension getMaximumSize() {
		return new Dimension(
			engine != null && engine.isConnected() ? displayModel.getRfbWidth()
				: 600, engine != null && engine.isConnected() ? displayModel
				.getRfbHeight() : 480);
	}

	public Dimension preferredSize() {
		return new Dimension(
			engine != null && engine.isConnected() ? displayModel.getRfbWidth()
				: 600, engine != null && engine.isConnected() ? displayModel
				.getRfbHeight() : 480);
	}

	public Dimension minimumSize() {
		return new Dimension(
			engine != null && engine.isConnected() ? displayModel.getRfbWidth()
				: 600, engine != null && engine.isConnected() ? displayModel
				.getRfbHeight() : 480);
	}

	public Dimension maximumSize() {
		return new Dimension(
			engine != null && engine.isConnected() ? displayModel.getRfbWidth()
				: 600, engine != null && engine.isConnected() ? displayModel
				.getRfbHeight() : 480);
	}

	public void paint(Graphics g) {
		if (engine == null || displayModel.getImageBuffer() == null) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getSize().width, getSize().height);
			super.paint(g);
			return;
		}
		synchronized (displayModel.getImageBuffer()) {
			displayModel.updateScale(this);
			displayModel.paintBuffer(g, this);
		}
	}

	public void resizeComponent() {
		if (context.getScaleMode() == NO_SCALING) {
			setSize(displayModel.getRfbWidth(), displayModel.getRfbHeight());
			validate();
		} else {
			repaint();
		}
	}

	public void requestRepaint(int tm, int x, int y, int w, int h) {
		if (engine != null) {
			double xscale = displayModel.getXscale();
			double yscale = displayModel.getYscale();
			int imagex = displayModel.getImagex();
			int imagey = displayModel.getImagey();
			repaint(tm, (int) (x * xscale) + imagex - 2, (int) (y * yscale)
				+ imagey - 2, (int) (w * xscale) + 4, (int) (h * yscale) + 4);
		}
		/* repaint(x + imagex, y + imagey, w, h); */
	}

	public void update(Graphics g) {
		paint(g);
		// g.dispose();
		// g = null;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer("[AWTRFBDisplay] ");
		if (engine != null) {
			buf.append("xscale=");
			buf.append(displayModel.getXscale());
			buf.append(" yscale=");
			buf.append(displayModel.getYscale());
			buf.append(" imagex=");
			buf.append(displayModel.getImagex());
			buf.append(" imagey=");
			buf.append(displayModel.getImagey());
			buf.append(" rfbWidth=");
			buf.append(displayModel.getRfbWidth());
			buf.append(" rfbHeight=");
			buf.append(displayModel.getRfbHeight());
		}
		buf.append(" size=");
		buf.append(getSize());
		return buf.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.rfb.RFBDisplay#getDisplayComponent()
	 */
	public Component getDisplayComponent() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.rfb.RFBDisplay#getEngine()
	 */
	public ProtocolEngine getEngine() {
		return engine;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.rfb.RFBDisplay#getDisplayModel()
	 */
	public RFBDisplayModel getDisplayModel() {
		return displayModel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Component#processEvent(java.awt.AWTEvent)
	 */
	protected void processEvent(AWTEvent e) {
		super.processEvent(e);
	}

	public void setUpdateRect(Rectangle updateRect) {
		this.updateRect = updateRect;
	}

	/**
	 * This method is called when information about an image which was
	 * previously requested using an asynchronous interface becomes available.
	 * 
	 * @param img
	 *            the image being observed.
	 * @param infoflags
	 *            the bitwise inclusive OR of the following flags:
	 *            <code>WIDTH</code>, <code>HEIGHT</code>,
	 *            <code>PROPERTIES</code>, <code>SOMEBITS</code>,
	 *            <code>FRAMEBITS</code>, <code>ALLBITS</code>,
	 *            <code>ERROR</code>, <code>ABORT</code>.
	 * @param x
	 *            the <i>x</i> coordinate.
	 * @param y
	 *            the <i>y</i> coordinate.
	 * @param width
	 *            the width.
	 * @param height
	 *            the height.
	 * @return <code>false</code> if the infoflags indicate that the image is
	 *         completely loaded; <code>true</code> otherwise.
	 * @todo Implement this java.awt.image.ImageObserver method
	 */
	public boolean imageUpdate(Image img, int infoflags, int x, int y,
			int width, int height) {

		if ((infoflags & (ALLBITS | ABORT)) == 0) {
			return true; // We need more image data.
		}
		// If the whole image is available, draw it now.
		if ((infoflags & ALLBITS) != 0) {
			if (updateRect != null) {
				synchronized (updateRect) {
					displayModel.getGraphicBuffer().drawImage(img,
						updateRect.x, updateRect.y, null);
					requestRepaint(context.getScreenUpdateTimeout(),
						updateRect.x, updateRect.y, updateRect.width,
						updateRect.height);
					updateRect.notify();
				}
			}
		}
		return false; // All image data was processed.
	}

	public boolean handleKeyEvent(KeyEvent evt) {
		return true;
	}

	public Image createBufferImage(int width, int height) {
		return createVolatileImage(width, height);
	}
}
