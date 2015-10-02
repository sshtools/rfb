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

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBDisplayModel;
import com.sshtools.rfb.RFBEventHandler;
import com.sshtools.rfb.RFBTransport;

public class SwingRFBDisplay extends JComponent implements RFBDisplay {

	private static final long serialVersionUID = 1L;
	
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


		try {
			Class<?>[] params = new Class[] { boolean.class };
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

	@Override
	public void initialiseSession(RFBTransport transport, RFBContext context,
			RFBEventHandler prompt) {
		this.context = context;
		context.resetEncodings();
		displayModel = new RFBDisplayModel(this);

		addComponentListener(new ComponentAdapter() {
			@Override
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
				displayModel, new ImageIcon(this.getClass().getResource(
						"/images/empty-cursor.png")).getImage(),
				new ImageIcon(this.getClass().getResource(
						"/images/dot-cursor.png")).getImage()) {
			@Override
			public void disconnect() {
				super.disconnect();
			}

		};
		engine.setStopCursor(new ImageIcon(this.getClass().getResource(
				"/images/stop-cursor.png")).getImage());
		addKeyListener(engine);
	}

	@Override
	public RFBContext getContext() {
		return context;
	}

	@Override
	public void addKeyListener(KeyListener listener) {
		keyListener = AWTEventMulticaster.add(keyListener, listener);
		enableEvents(AWTEvent.KEY_EVENT_MASK);
	}
	@Override
	public void removeKeyListener(KeyListener listener) {
		keyListener = AWTEventMulticaster.remove(keyListener, listener);
	}
	@Override
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

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(
				engine != null && engine.isConnected() ? displayModel.getRfbWidth()
						: 600,
				engine != null && engine.isConnected() ? displayModel
						.getRfbHeight() : 480);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(
				engine != null && engine.isConnected() ? displayModel.getRfbWidth()
						: 600,
				engine != null && engine.isConnected() ? displayModel
						.getRfbHeight() : 480);
	}

	@Override
	public Dimension getMaximumSize() {
		return new Dimension(
				engine != null && engine.isConnected() ? displayModel.getRfbWidth()
						: 600,
				engine != null && engine.isConnected() ? displayModel
						.getRfbHeight() : 480);
	}

	@Override
	public Dimension preferredSize() {
		return new Dimension(
				engine != null && engine.isConnected() ? displayModel.getRfbWidth()
						: 600,
				engine != null && engine.isConnected() ? displayModel
						.getRfbHeight() : 480);
	}

	@Override
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

	@Override
	public void paintComponent(Graphics g) {
		if (engine == null || displayModel.getImageBuffer() == null) {
			return;
		}
		// synchronized (displayModel.getImageBuffer()) {
		displayModel.updateScale(this);
		displayModel.paintBuffer(g, this);
		// }
	}

	@Override
	public void resizeComponent() {
		if (context.getScaleMode() == NO_SCALING) {
			setSize(displayModel.getRfbWidth(), displayModel.getRfbHeight());
			validate();
		} else {
			repaint();
		}
	}

	@Override
	public void requestRepaint(int tm, int x, int y, int w, int h) {
		if (engine != null) {
			repaint(tm,
					(int) (x * displayModel.getXscale())
							+ displayModel.getImagex() - 2,
					(int) (y * displayModel.getYscale())
							+ displayModel.getImagey() - 2,
					(int) (w * displayModel.getXscale()) + 4,
					(int) (h * displayModel.getYscale()) + 4);
		}
	}

	@Override
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

	@Override
	public Component getDisplayComponent() {
		return this;
	}

	@Override
	public ProtocolEngine getEngine() {
		return engine;
	}

	@Override
	public RFBDisplayModel getDisplayModel() {
		return displayModel;
	}

	@Override
	public void setUpdateRect(Rectangle updateRect) {
		this.updateRect = updateRect;
	}

	@Override
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

	@Override
	public boolean handleKeyEvent(KeyEvent evt) {
		return true;
	}

}
