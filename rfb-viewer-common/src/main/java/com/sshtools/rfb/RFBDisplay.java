/**
 * RFB - Remote Frame Buffer (VNC) implementation.
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
package com.sshtools.rfb;

import com.sshtools.rfb.RFBToolkit.RFBCursor;

public interface RFBDisplay<C, K>  {
	// Desktop scaling
	public final static int RESIZE_DESKTOP = -1;
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
