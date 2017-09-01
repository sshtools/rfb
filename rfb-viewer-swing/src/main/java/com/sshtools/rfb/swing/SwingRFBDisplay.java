/* HEADER */
package com.sshtools.rfb.swing;

import java.awt.AWTEvent;
import java.awt.AWTEventMulticaster;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBDisplayModel;
import com.sshtools.rfb.RFBEventHandler;
import com.sshtools.rfb.RFBRectangle;
import com.sshtools.rfb.RFBToolkit;
import com.sshtools.rfb.RFBToolkit.RFBCursor;
import com.sshtools.rfb.RFBTransport;
import com.sshtools.rfb.swing.SwingRFBToolkit.RFBAWTCursor;
import com.sshtools.rfb.swing.SwingRFBToolkit.RFBBufferedImage;
import com.sshtools.rfbcommon.ScreenData;
import com.sshtools.rfbcommon.ScreenDimension;

public class SwingRFBDisplay extends JComponent
		implements RFBDisplay<JComponent, KeyEvent>, KeyListener, MouseListener, MouseMotionListener {
	final static Logger LOG = LoggerFactory.getLogger(SwingRFBDisplay.class);
	private static final long serialVersionUID = 1L;
	private ProtocolEngine engine;
	private KeyListener keyListener;
	private RFBContext context;
	private RFBDisplayModel displayModel;
	private RFBRectangle updateRect;
	private MouseEventDispatcher mouseEventDispatcher;

	public SwingRFBDisplay() {
		super();
		setDoubleBuffered(true);
		enableEvents(AWTEvent.FOCUS_EVENT_MASK | AWTEvent.ACTION_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
				| AWTEvent.MOUSE_EVENT_MASK);
		try {
			Class<?>[] params = new Class[] { boolean.class };
			SwingRFBDisplay.class.getMethod("setFocusable", params).invoke(this, new Object[] { Boolean.TRUE });
			SwingRFBDisplay.class.getMethod("setFocusTraversalKeysEnabled", params).invoke(this, new Object[] { Boolean.FALSE });
		} catch (Throwable t) {
			System.err.println("unable to reset focus handling for java version ");
			t.printStackTrace();
		}
	}

	@Override
	public void initialiseSession(RFBTransport transport, RFBContext context, RFBEventHandler prompt) {
		this.context = context;
		context.resetEncodings();
		displayModel = new RFBDisplayModel(this);
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if (engine == null || !engine.isProcessingEvents())
					return;
				if (engine != null && SwingRFBDisplay.this.context.getScaleMode() != NO_SCALING) {
					if (SwingRFBDisplay.this.context.getScaleMode() == RESIZE_DESKTOP && engine.isUseExtendedDesktopSize()) {
						ScreenData sd = new ScreenData(displayModel.getScreenData());
						ScreenDimension dim = new ScreenDimension(SwingRFBDisplay.this.getWidth(),
								SwingRFBDisplay.this.getHeight());
						sd.getDimension().set(dim);
						try {
							engine.setDesktopSize(sd);
						} catch (IOException e1) {
							LOG.warn("Failed to set remote desktop size.", e1);
						}
					} else if (!displayModel.getScreenData().isEmpty())
						displayModel.updateBuffer();
				}
			}
		});
		displayModel.setContext(context);
		engine = new ProtocolEngine(this, transport, context, prompt, displayModel,
				RFBToolkit.get().loadImage("/images/empty-cursor.png"), RFBToolkit.get().loadImage("/images/dot-cursor.png")) {
			@Override
			public void disconnect() {
				super.disconnect();
			}
		};
		engine.setStopCursor(RFBToolkit.get().loadImage("/images/stop-cursor.png"));
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(new WheelListener());
		addKeyListener(this);
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
		return new Dimension(engine != null && engine.isConnected() ? displayModel.getRfbWidth() : 600,
				engine != null && engine.isConnected() ? displayModel.getRfbHeight() : 480);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(engine != null && engine.isConnected() ? displayModel.getRfbWidth() : 600,
				engine != null && engine.isConnected() ? displayModel.getRfbHeight() : 480);
	}

	@Override
	public Dimension getMaximumSize() {
		return new Dimension(engine != null && engine.isConnected() ? displayModel.getRfbWidth() : 600,
				engine != null && engine.isConnected() ? displayModel.getRfbHeight() : 480);
	}

	@Override
	public Dimension preferredSize() {
		return new Dimension(engine != null && engine.isConnected() ? displayModel.getRfbWidth() : 600,
				engine != null && engine.isConnected() ? displayModel.getRfbHeight() : 480);
	}

	@Override
	public Dimension minimumSize() {
		return new Dimension(engine != null && engine.isConnected() ? displayModel.getRfbWidth() : 600,
				engine != null && engine.isConnected() ? displayModel.getRfbHeight() : 480);
	}

	public Dimension maximumSize() {
		return new Dimension(engine != null && engine.isConnected() ? displayModel.getRfbWidth() : 600,
				engine != null && engine.isConnected() ? displayModel.getRfbHeight() : 480);
	}

	@Override
	public void paintComponent(Graphics g) {
		if (engine == null || displayModel.getImageBuffer() == null) {
			return;
		}
		// synchronized (displayModel.getImageBuffer()) {
		displayModel.updateScale(this);
		displayModel.paintBuffer(new RFBGraphics2D((Graphics2D) g));
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
			repaint(tm, (int) (x * displayModel.getXscale()) + displayModel.getImagex() - 2,
					(int) (y * displayModel.getYscale()) + displayModel.getImagey() - 2, (int) (w * displayModel.getXscale()) + 4,
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
	public JComponent getDisplayComponent() {
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
	public void setUpdateRect(RFBRectangle updateRect) {
		this.updateRect = updateRect;
	}

	@Override
	public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
		if ((infoflags & (ALLBITS | ABORT)) == 0) {
			return true; // We need more image data.
		}
		// If the whole image is available, draw it now.
		if ((infoflags & ALLBITS) != 0) {
			if (updateRect != null) {
				synchronized (updateRect) {
					displayModel.getGraphicBuffer().drawImage(new RFBBufferedImage((BufferedImage) img), updateRect.x,
							updateRect.y);
					repaint(updateRect.x, updateRect.y, updateRect.w, updateRect.h);
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

	@Override
	public void keyPressed(KeyEvent evt) {
		processLocalKeyEvent(evt);
	}

	@Override
	public void keyReleased(KeyEvent evt) {
		processLocalKeyEvent(evt);
	}

	@Override
	public void keyTyped(KeyEvent evt) {
		evt.consume();
	}

	@Override
	public void mousePressed(MouseEvent evt) {
		processLocalMouseEvent(evt, false);
	}

	@Override
	public void mouseReleased(MouseEvent evt) {
		processLocalMouseEvent(evt, false);
	}

	@Override
	public void mouseMoved(MouseEvent evt) {
		processLocalMouseEvent(evt, true);
	}

	@Override
	public void mouseDragged(MouseEvent evt) {
		processLocalMouseEvent(evt, true);
	}

	public void processLocalKeyEvent(KeyEvent evt) {
		if (engine.isConnected() && engine.isProcessingEvents()) {
			if (handleKeyEvent(evt)) {
				if (!engine.isInputEnabled()) {
					if ((evt.getKeyChar() == 'r' || evt.getKeyChar() == 'R') && evt.getID() == KeyEvent.KEY_PRESSED) {
						try {
							engine.requestFramebufferUpdate(0, 0, displayModel.getRfbWidth(), displayModel.getRfbHeight(), false);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} else {
					try {
						postKeyboardEvent(evt);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			evt.consume();
		}
	}

	public void processLocalMouseEvent(MouseEvent evt, boolean moved) {
		if (engine.isProcessingEvents()) {
			if (engine.isInputEnabled()) {
				if (context.getMouseEventDelay() != 0
						&& (evt.getID() == MouseEvent.MOUSE_MOVED || evt.getID() == MouseEvent.MOUSE_DRAGGED)) {
					if (mouseEventDispatcher == null || !mouseEventDispatcher.isAlive()) {
						mouseEventDispatcher = new MouseEventDispatcher();
						mouseEventDispatcher.start();
					}
					mouseEventDispatcher.dispatch(evt);
				} else {
					if (moved) {
						doMoveCursor(evt.getX(), evt.getY());
					}
					try {
						postPointerEvent(evt);
					} catch (IOException e) {
					}
					// notify();
				}
			}
		}
	}

	@Override
	public void mouseClicked(MouseEvent evt) {
		getDisplayComponent().requestFocus();
	}

	@Override
	public void mouseEntered(MouseEvent evt) {
	}

	@Override
	public void mouseExited(MouseEvent evt) {
	}

	/**
	 * Record a pointer event ready for sending
	 * 
	 * @param evt
	 * @throws IOException
	 */
	public synchronized void postPointerEvent(MouseEvent evt) throws IOException {
		int modifiers = evt.getModifiers();
		int mask2 = 2;
		int mask3 = 4;
		if (context.isReverseMouseButtons2And3()) {
			mask2 = 4;
			mask3 = 2;
		}
		int mask4 = 8;
		int mask5 = 16;
		if (evt.getID() == MouseEvent.MOUSE_WHEEL) {
			// #ifdef JAVA2
			java.awt.event.MouseWheelEvent we = (java.awt.event.MouseWheelEvent) evt;
			if (we.getWheelRotation() < 0) {
				engine.setPointerMask(mask4);
			} else {
				engine.setPointerMask(mask5);
			}
			// #endif
		} else if (evt.getID() == MouseEvent.MOUSE_PRESSED) {
			if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
				engine.setPointerMask(mask2);
				modifiers &= ~RFBDisplay.ALT_MASK;
			} else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
				engine.setPointerMask(mask3);
				modifiers &= ~RFBDisplay.META_MASK;
			} else {
				engine.setPointerMask(1);
			}
		} else if (evt.getID() == MouseEvent.MOUSE_RELEASED) {
			engine.setPointerMask(0);
			if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
				modifiers &= ~RFBDisplay.ALT_MASK;
			} else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
				modifiers &= ~RFBDisplay.META_MASK;
			}
		}
		int x = (int) (evt.getX() / displayModel.getXscale()) - displayModel.getImagex();
		int y = (int) (evt.getY() / displayModel.getYscale()) - displayModel.getImagey();
		if (x < 0) {
			x = 0;
		}
		if (y < 0) {
			y = 0;
		}
		if (y >= displayModel.getRfbHeight()) {
			y = displayModel.getRfbHeight() - 1;
		}
		if (x >= displayModel.getRfbWidth()) {
			y = displayModel.getRfbWidth() - 1;
		}
		engine.sendPointerEvent(modifiers, x, y);
		// Button up must be sent on mouse wheel
		if (evt.getID() == MouseEvent.MOUSE_WHEEL) {
			engine.setPointerMask(engine.getPointerMask() & ~mask4);
			engine.setPointerMask(engine.getPointerMask() & ~mask5);
			engine.sendPointerEvent(modifiers, x, y);
		}
		// A bug? Without this my server (TightVNC 1.2.9 on Linux) doesnt seem
		// to send a cursor update back upon click
		// if (context.isCursorUpdatesRequested()) {
		// requestFramebufferUpdate(x, y, 1, 1, true);
		// }
	}

	public synchronized void postKeyboardEvent(KeyEvent evt) throws IOException {
		int keyChar = evt.getKeyChar();
		if (keyChar == 0) {
			keyChar = KeyEvent.CHAR_UNDEFINED;
		}
		int key = 0;
		if (keyChar == KeyEvent.CHAR_UNDEFINED) {
			keyChar = evt.getKeyCode();
			switch (keyChar) {
			case KeyEvent.VK_CONTROL:
				key = evt.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT ? 0xffe4 : 0xffe3;
				break;
			case KeyEvent.VK_SHIFT:
				key = evt.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT ? 0xffe2 : 0xffe1;
				break;
			case KeyEvent.VK_ALT:
				key = evt.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT ? 0xffea : 0xffe9;
				break;
			case KeyEvent.VK_META:
				key = evt.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT ? 0xffe8 : 0xffe7;
				break;
			}
		}
		boolean down = (evt.getID() == KeyEvent.KEY_PRESSED);
		if (key == 0) {
			if (evt.isActionKey()) {
				switch (evt.getKeyCode()) {
				case KeyEvent.VK_HOME:
					key = 0xFF50;
					break;
				case KeyEvent.VK_LEFT:
					key = 0xFF51;
					break;
				case KeyEvent.VK_UP:
					key = 0xFF52;
					break;
				case KeyEvent.VK_RIGHT:
					key = 0xFF53;
					break;
				case KeyEvent.VK_DOWN:
					key = 0xFF54;
					break;
				case KeyEvent.VK_PAGE_UP:
					key = 0xFF55;
					break;
				case KeyEvent.VK_PAGE_DOWN:
					key = 0xFF56;
					break;
				case KeyEvent.VK_END:
					key = 0xFF57;
					break;
				case KeyEvent.VK_INSERT:
					key = 0xFF63;
					break;
				case KeyEvent.VK_F1:
					key = 0xFFBE;
					break;
				case KeyEvent.VK_F2:
					key = 0xFFBF;
					break;
				case KeyEvent.VK_F3:
					key = 0xFFC0;
					break;
				case KeyEvent.VK_F4:
					key = 0xFFC1;
					break;
				case KeyEvent.VK_F5:
					key = 0xFFC2;
					break;
				case KeyEvent.VK_F6:
					key = 0xFFC3;
					break;
				case KeyEvent.VK_F7:
					key = 0xFFC4;
					break;
				case KeyEvent.VK_F8:
					key = 0xFFC5;
					break;
				case KeyEvent.VK_F9:
					key = 0xFFC6;
					break;
				case KeyEvent.VK_F10:
					key = 0xFFC7;
					break;
				case KeyEvent.VK_F11:
					key = 0xFFC8;
					break;
				case KeyEvent.VK_F12:
					key = 0xFFC9;
					break;
				default:
					return;
				}
			} else {
				key = keyChar;
				if (key == KeyEvent.VK_DELETE) {
					key = 0xFFFF;
				} else {
					if (key < 0x20) {
						if (evt.isControlDown()) {
							key += 0x60;
						} else {
							switch (key) {
							case KeyEvent.VK_BACK_SPACE:
								key = 0xFF08;
								break;
							case KeyEvent.VK_TAB:
								key = 0xFF09;
								break;
							case KeyEvent.VK_ENTER:
								key = 0xFF0D;
								break;
							case KeyEvent.VK_ESCAPE:
								key = 0xFF1B;
								break;
							}
						}
					}
				}
			}
		}
		engine.postKeyboardEvent(key, down, evt.getModifiers());
	}

	private void doMoveCursor(int x, int y) {
		int[] displayClip = new int[] { displayModel.getImagex(), displayModel.getImagey(),
				getDisplayComponent().getSize().width - (displayModel.getImagex() * 2),
				getDisplayComponent().getSize().height - (displayModel.getImagey() * 2) };
		boolean changeToLocal = false;
		if (x < displayClip[0]) {
			x = displayClip[0];
			changeToLocal = true;
		} else if (x >= displayClip[0] + displayClip[2]) {
			x = displayClip[0] + displayClip[2] - 1;
			changeToLocal = true;
		}
		if (y < displayClip[1]) {
			y = displayClip[1];
			changeToLocal = true;
		} else if (y >= displayClip[1] + displayClip[3]) {
			y = displayClip[1] + displayClip[3] - 1;
			changeToLocal = true;
		}
		if (changeToLocal) {
			engine.setLocalCursor(null, -1, -1);
		} else {
			engine.updateCursor((int) (((float) x - (float) displayModel.getImagex()) / displayModel.getXscale()),
					(int) (((float) y - (float) displayModel.getImagey()) / displayModel.getYscale()));
		}
	}

	// #ifdef JAVA2
	class WheelListener implements java.awt.event.MouseWheelListener {
		@Override
		public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
			processLocalMouseEvent(e, false);
		}
	}
	// #endif

	class MouseEventDispatcher extends Thread {
		MouseEvent lastEvent;
		int events;

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(context.getMouseEventDelay());
					if (lastEvent != null) {
						postPointerEvent(lastEvent);
						if (context.isLocalCursorDisplayed()) {
							engine.setLocalCursor(displayModel.getCursor(), displayModel.getHotX(), displayModel.getHotY());
						} else {
							doMoveCursor(lastEvent.getX(), lastEvent.getY());
						}
						lastEvent = null;
					}
				} catch (InterruptedException ie) {
					if (lastEvent != null && events > context.getMouseEventThreshold()) {
						try {
							postPointerEvent(lastEvent);
						} catch (IOException ioe) {
							break;
						}
						// if (context.isLocalCursorDisplayed()) {
						// setLocalCursor(displayModel.getCursor(),
						// displayModel.hotX, displayModel.hotY);
						// } else {
						doMoveCursor(lastEvent.getX(), lastEvent.getY());
						// setLocalCursor(emptyCursor, 0, 0);
						// }
						lastEvent = null;
						events = 0;
					} else {
						if (context.isLocalCursorDisplayed()) {
							engine.setLocalCursor(displayModel.getCursor(), displayModel.getHotX(), displayModel.getHotY());
						} else {
							engine.setLocalCursor(engine.getDotCursor(), 2, 2);
						}
					}
				} catch (IOException ioe) {
					break;
				}
			}
		}

		public void dispatch(MouseEvent evt) {
			this.lastEvent = evt;
			events++;
			interrupt();
		}
	}

	@Override
	public void setCursor(RFBCursor defaultCursor) {
		getDisplayComponent().setCursor(((RFBAWTCursor) defaultCursor).cursor);
	}

	@Override
	public int[] getDisplayComponentSize() {
		return new int[] { getDisplayComponent().getWidth(), getDisplayComponent().getHeight() };
	}
}
