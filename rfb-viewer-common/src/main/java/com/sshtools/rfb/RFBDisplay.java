/* HEADER */
package com.sshtools.rfb;

import com.sshtools.rfb.RFBToolkit.RFBCursor;

public interface RFBDisplay<C, K>  {
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

	ProtocolEngine getEngine();

	void initialiseSession(RFBTransport transport, RFBContext context, RFBEventHandler prompt);

	C getDisplayComponent();

	void requestRepaint(int tm, int x, int y, int w, int h);

	void setUpdateRect(RFBRectangle updateRect);

	void resizeComponent();

	RFBDisplayModel getDisplayModel();

	RFBContext getContext();

	boolean handleKeyEvent(K evt);

	void setCursor(RFBCursor defaultCursor);

	int[] getDisplayComponentSize();
}
