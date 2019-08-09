/**
 * RFB - Remote Frame Buffer (VNC) implementation for JavaFX.
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
/* HEADER */
package com.sshtools.rfb.javafx;

import java.io.IOException;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBDisplayModel;
import com.sshtools.rfb.RFBEventHandler;
import com.sshtools.rfb.RFBRectangle;
import com.sshtools.rfb.RFBToolkit;
import com.sshtools.rfb.RFBToolkit.RFBCursor;
import com.sshtools.rfb.javafx.JavaFXRFBToolkit.RFBJavaFXCursor;
import com.sshtools.rfb.RFBTransport;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

public class JavaFXRFBDisplay implements RFBDisplay<Node, KeyEvent> {

	private static final long serialVersionUID = 1L;

	private ProtocolEngine engine;
	private RFBContext context;
	private RFBDisplayModel displayModel;
	private RFBRectangle updateRect;
	private Canvas canvas;
	private Pane group;
	private int modifiers;
	private int mouseX;
	private int mouseY;

	public JavaFXRFBDisplay() {
		super();
		canvas = new Canvas(640, 480);

		group = new Pane();
		group.layoutBoundsProperty().addListener(new ChangeListener<Bounds>() {

			@Override
			public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
				canvas.resize(newValue.getWidth(), newValue.getHeight());
				if (displayModel != null)
					displayModel.updateBuffer();
			}
		});
		group.getChildren().add(canvas);
	}
	
	@Override
	public Node getDisplayComponent() {
		return group;
	}

	public Canvas getCanvas() {
		return canvas;
	}

	@Override
	public void initialiseSession(RFBTransport transport, RFBContext context, RFBEventHandler prompt) {
		this.context = context;
		context.resetEncodings();
		displayModel = new RFBDisplayModel(this);
		displayModel.setContext(context);

		engine = new ProtocolEngine(this, transport, context, prompt, displayModel,
				RFBToolkit.get().loadImage("/images/empty-cursor.png"),
				RFBToolkit.get().loadImage("/images/dot-cursor.png")) {
			@Override
			public void disconnect() {
				super.disconnect();
			}

		};

		engine.setStopCursor(RFBToolkit.get().loadImage("/images/stop-cursor.png"));

		canvas.setOnKeyPressed((KeyEvent event) -> processLocalKeyEvent(event));
		canvas.setOnKeyReleased((KeyEvent event) -> processLocalKeyEvent(event));
		canvas.setOnKeyTyped((KeyEvent event) -> event.consume());
		canvas.setOnMouseMoved((MouseEvent event) -> processLocalMouseEvent(event, true));
		canvas.setOnMousePressed((MouseEvent event) -> processLocalMouseEvent(event, false));
		canvas.setOnMouseReleased((MouseEvent event) -> processLocalMouseEvent(event, false));
		canvas.setOnMouseDragged((MouseEvent event) -> processLocalMouseEvent(event, true));
		canvas.setOnMouseClicked((MouseEvent event) -> getDisplayComponent().requestFocus());
		canvas.setOnScroll((javafx.scene.input.ScrollEvent event) -> processLocalScrollEvent(event));

	}
	
	void adjustCanvasSize() {
		if(engine != null && engine.isConnected()) {
			canvas.setWidth(displayModel.getRfbWidth());
			canvas.setHeight(displayModel.getRfbHeight());
		}
		else {
			canvas.setWidth(640);
			canvas.setHeight(480);
		}
	}

	@Override
	public RFBContext getContext() {
		return context;
	}

//	@Override
//	public void paintComponent(Graphics g) {
//		if (engine == null || displayModel.getImageBuffer() == null) {
//			return;
//		}
//		// synchronized (displayModel.getImageBuffer()) {
//		displayModel.updateScale(this);
//		displayModel.paintBuffer(new RFBGraphics2D((Graphics2D) g));
//		// }
//	}

	@Override
	public void resizeComponent() {
		if (context.getScaleMode() == NO_SCALING || context.getScaleMode() == RESIZE_DESKTOP) {
			canvas.setWidth(displayModel.getRfbWidth());
			canvas.setHeight(displayModel.getRfbHeight());
		}
//		else {
//			canvas.repaint();
//		}
	}

	@Override
	public void requestRepaint(int tm, int x, int y, int w, int h) {
//		if (engine != null) {
//			repaint(tm, (int) (x * displayModel.getXscale()) + displayModel.getImagex() - 2,
//					(int) (y * displayModel.getYscale()) + displayModel.getImagey() - 2,
//					(int) (w * displayModel.getXscale()) + 4, (int) (h * displayModel.getYscale()) + 4);
//		}
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
	public boolean handleKeyEvent(KeyEvent evt) {
		return true;
	}

	public void processLocalKeyEvent(KeyEvent evt) {

		if (engine.isConnected() && engine.isProcessingEvents()) {
			if (handleKeyEvent(evt)) {
				modifiers = getModifiers(evt);
				if (!engine.isInputEnabled()) {
					if (evt.getCode() == KeyCode.R && evt.getEventType() == KeyEvent.KEY_PRESSED) {
						try {
							engine.requestFramebufferUpdate(0, 0, displayModel.getRfbWidth(),
									displayModel.getRfbHeight(), false);
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
				if (moved) {
					doMoveCursor((int)evt.getX(), (int)evt.getY());
				}
				try {
					postPointerEvent(evt);
				} catch (IOException e) {
				}
			}
		}
	}

	public void processLocalScrollEvent(ScrollEvent evt) {
		if (engine.isProcessingEvents()) {
			if (engine.isInputEnabled()) {
				try {
					postPointerEvent(evt);
				} catch (IOException e) {
				}
			}
		}
	}

	public synchronized void postPointerEvent(ScrollEvent evt) throws IOException {
		int mask4 = 8;
		int mask5 = 16;
		if (evt.getDeltaY() < 0) {
			engine.setPointerMask(mask4);
		} else {
			engine.setPointerMask(mask5);
		}
		engine.setPointerMask(engine.getPointerMask() & ~mask4);
		engine.setPointerMask(engine.getPointerMask() & ~mask5);
		engine.sendPointerEvent(modifiers, mouseX, mouseY);
	}

	private int getModifiers(KeyEvent evt) {
		int modifiers = 0;
		if (evt.isAltDown())
			modifiers += RFBDisplay.ALT_MASK;
		if (evt.isControlDown())
			modifiers += RFBDisplay.CTRL_MASK;
		if (evt.isShiftDown())
			modifiers += RFBDisplay.SHIFT_MASK;
		if (evt.isMetaDown())
			modifiers += RFBDisplay.META_MASK;
		return modifiers;
	}

	public synchronized void postPointerEvent(MouseEvent evt) throws IOException {

		int pointerMask = 0;
		if (evt.isPrimaryButtonDown())
			pointerMask += 1;
		if (((!context.isReverseMouseButtons2And3() && evt.isMiddleButtonDown())
				|| (context.isReverseMouseButtons2And3() && evt.isSecondaryButtonDown())))
			pointerMask += 2;
		if (((!context.isReverseMouseButtons2And3() && evt.isSecondaryButtonDown())
				|| (context.isReverseMouseButtons2And3() && evt.isMiddleButtonDown())))
			if (evt.isSecondaryButtonDown())
				pointerMask += 4;

		int x = (int) (evt.getX() / displayModel.getXscale()) - displayModel.getImagex();
		if (x < 0) {
			x = 0;
		}
		if (x >= displayModel.getRfbWidth()) {
			x = displayModel.getRfbWidth() - 1;
		}
		int y = (int) (evt.getY() / displayModel.getYscale()) - displayModel.getImagey();
		if (y < 0) {
			y = 0;
		}
		if (y >= displayModel.getRfbHeight()) {
			y = displayModel.getRfbHeight() - 1;
		}
		mouseX = x;
		mouseY = y;
		engine.setPointerMask(pointerMask);
		engine.sendPointerEvent(modifiers, x, y);
	}

	public synchronized void postKeyboardEvent(KeyEvent evt) throws IOException {
		int key = 0;
		switch(evt.getCode()) {
		case CONTROL:
			key = 0xffe3;
			break;
		case ALT:
			key = 0xffe9;
			break;
		case ALT_GRAPH:
			key = 0xffe4;
			break;
		case SHIFT:
			key = 0xffe2;
			break;
		case META:
			key = 0xffe7;
			break;
		case HOME:
			key = 0xff50;
			break;
		case LEFT:
			key = 0xff51;
			break;
		case UP:
			key = 0xff52;
			break;
		case RIGHT:
			key = 0xff53;
			break;
		case DOWN:
			key = 0xff54;
			break;
		case PAGE_UP:
			key = 0xff55;
			break;
		case PAGE_DOWN:
			key = 0xff56;
			break;
		case END:
			key = 0xff57;
			break;
		case INSERT:
			key = 0xff63;
			break;
		case F1:
			key = 0xffbe;
			break;
		case F2:
			key = 0xffbf;
			break;
		case F3:
			key = 0xffc0;
			break;
		case F4:
			key = 0xffc1;
			break;
		case F5:
			key = 0xffc2;
			break;
		case F6:
			key = 0xffc3;
			break;
		case F7:
			key = 0xffc4;
			break;
		case F8:
			key = 0xffc5;
			break;
		case F9:
			key = 0xffc6;
			break;
		case F10:
			key = 0xffc7;
			break;
		case F11:
			key = 0xffc8;
			break;
		case F12:
			key = 0xffc9;
			break;
		case DELETE:
			key = 0xffff;
			break;
		case BACK_SPACE:
			key = 0xff08;
			break;
		case TAB:
			key = 0xff09;
			break;
		case ENTER:
			key = 0xff0d;
			break;
		case ESCAPE:
			key = 0xff1b;
			break;
		}
		boolean down = (evt.getEventType() == KeyEvent.KEY_PRESSED);
		
		
		if (key < 0x20) {
			if (evt.isControlDown()) {
				key += 0x60;
			} else {
			}
		}
		engine.postKeyboardEvent(key, down, modifiers);
	}

	private void doMoveCursor(int x, int y) {
		int[] displayClip = new int[] { displayModel.getImagex(), displayModel.getImagey(),
				(int) getDisplayComponent().layoutBoundsProperty().get().getWidth() - (displayModel.getImagex() * 2),
				(int) getDisplayComponent().layoutBoundsProperty().get().getHeight() - (displayModel.getImagey() * 2) };
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

	@Override
	public void setCursor(RFBCursor defaultCursor) {
		getDisplayComponent().setCursor(((RFBJavaFXCursor) defaultCursor).cursor);
	}

	@Override
	public int[] getDisplayComponentSize() {
		return new int[] { (int) getDisplayComponent().layoutBoundsProperty().get().getWidth(),
				(int) getDisplayComponent().layoutBoundsProperty().get().getHeight() };
	}

}
