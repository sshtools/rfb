/* HEADER */
package com.sshtools.rfb.swing;

import java.awt.AWTEvent;
import java.awt.AWTEventMulticaster;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JComponent;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBDisplayModel;
import com.sshtools.rfb.RFBEventHandler;
import com.sshtools.rfb.RFBTransport;
import com.sshtools.ui.swing.ResourceIcon;

public class SwingRFBDisplay extends JComponent implements RFBDisplay {

	ProtocolEngine engine;
	KeyListener keyListener;
	RFBContext context;
	RFBDisplayModel displayModel;
	Rectangle updateRect;

	public SwingRFBDisplay() {
		super();
		setDoubleBuffered(true);
		enableEvents(AWTEvent.FOCUS_EVENT_MASK | AWTEvent.ACTION_EVENT_MASK
				| AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);

		// displayModel = new RFBDisplayModel(this);
		// displayModel.setupColors();
		//
		// addComponentListener(new ComponentAdapter() {
		// public void componentResized(ComponentEvent e) {
		// if (engine != null && context.getScaleMode() != NO_SCALING
		// && displayModel.getRfbWidth() != 0
		// && displayModel.getRfbHeight() != 0) {
		// displayModel.updateFramebufferSize(SwingRFBDisplay.this);
		// }
		// }
		// });

		try {
			Class[] params = new Class[] { boolean.class };
			SwingRFBDisplay.class.getMethod("setFocusable", params).invoke(
					this, new Object[] { Boolean.TRUE });
			SwingRFBDisplay.class.getMethod("setFocusTraversalKeysEnabled",
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
		context.resetEncodings();
		displayModel = new RFBDisplayModel(this);

		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				if (engine != null
						&& SwingRFBDisplay.this.context.getScaleMode() != NO_SCALING
						&& displayModel.getRfbWidth() != 0
						&& displayModel.getRfbHeight() != 0) {
					displayModel.updateBuffer();
				}
			}
		});

		displayModel.setContext(context);

		if (engine != null) {
			removeKeyListener(engine);
		}
		engine = new ProtocolEngine(this, transport, context, prompt,
				displayModel, new ResourceIcon(this.getClass(),
						"/images/empty-cursor.png").getImage(),
				new ResourceIcon(this.getClass(), "/images/dot-cursor.png")
						.getImage()) {
			@Override
			public void disconnect() {
				super.disconnect();
			}

		};
		engine.setStopCursor(new ResourceIcon(this.getClass(),
				"/images/stop-cursor.png").getImage());
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
						: 600,
				engine != null && engine.isConnected() ? displayModel
						.getRfbHeight() : 480);
	}

	public Dimension getMinimumSize() {
		return new Dimension(
				engine != null && engine.isConnected() ? displayModel.getRfbWidth()
						: 600,
				engine != null && engine.isConnected() ? displayModel
						.getRfbHeight() : 480);
	}

	public Dimension getMaximumSize() {
		return new Dimension(
				engine != null && engine.isConnected() ? displayModel.getRfbWidth()
						: 600,
				engine != null && engine.isConnected() ? displayModel
						.getRfbHeight() : 480);
	}

	public Dimension preferredSize() {
		return new Dimension(
				engine != null && engine.isConnected() ? displayModel.getRfbWidth()
						: 600,
				engine != null && engine.isConnected() ? displayModel
						.getRfbHeight() : 480);
	}

	public Dimension minimumSize() {
		return new Dimension(
				engine != null && engine.isConnected() ? displayModel.getRfbWidth()
						: 600,
				engine != null && engine.isConnected() ? displayModel
						.getRfbHeight() : 480);
	}

	public Dimension maximumSize() {
		return new Dimension(
				engine != null && engine.isConnected() ? displayModel.getRfbWidth()
						: 600,
				engine != null && engine.isConnected() ? displayModel
						.getRfbHeight() : 480);
	}

	public void paintComponent(Graphics g) {
		if (engine == null || displayModel.getImageBuffer() == null) {
			return;
		}
		// synchronized (displayModel.getImageBuffer()) {
		displayModel.updateScale(this);
		displayModel.paintBuffer(g, this);
		// }
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

		int myX = this.getX();
		int myY = this.getY();

		if (engine != null) {
			repaint(tm,
					(int) (x * displayModel.getXscale())
							+ displayModel.getImagex() - 2,
					(int) (y * displayModel.getYscale())
							+ displayModel.getImagey() - 2,
					(int) (w * displayModel.getXscale()) + 4,
					(int) (h * displayModel.getYscale()) + 4);
		}
		/* repaint(x + imagex, y + imagey, w, h); */
	}

	public String toString() {
		StringBuffer buf = new StringBuffer("[SwingRFBDisplay] ");
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

					repaint(updateRect.x, updateRect.y, updateRect.width,
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

}
