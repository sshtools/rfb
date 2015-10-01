/* HEADER */
package com.sshtools.rfb;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.ImageObserver;

public interface RFBDisplay extends ImageObserver {
	// Desktop scaling
	public final static int NO_SCALING = 0;
	public final static int NEAREST_NEIGHBOR = 1;
	public final static int BILINEAR = 2;
	public final static int BICUBIC = 3;
	// The protocol version
	public final static String VERSION_STRING = "3.8";
	// Useful shortcuts for modifier masks.
	public final static int CTRL_MASK = 1 << 1;
	public final static int SHIFT_MASK = 1 << 0;
	public final static int META_MASK = 1 << 2;
	public final static int ALT_MASK = 1 << 3;
	public final static int COLOR_8BIT = 8;
	public final static int COLOR_32BIT = 32;

	public ProtocolEngine getEngine();

	public void initialiseSession(RFBTransport transport, RFBContext context,
			RFBEventHandler prompt);

	public Component getDisplayComponent();

	public void requestRepaint(int tm, int x, int y, int w, int h);

	public void setUpdateRect(Rectangle updateRect);

	public void resizeComponent();

	public RFBDisplayModel getDisplayModel();

	public RFBContext getContext();

	public boolean handleKeyEvent(KeyEvent evt);

	public void setCursor(Cursor defaultCursor);
}
