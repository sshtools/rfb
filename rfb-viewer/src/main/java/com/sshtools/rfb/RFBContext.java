/* HEADER */
package com.sshtools.rfb;

import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.sshtools.profile.ResourceProfile;
import com.sshtools.rfb.encoding.CORREEncoding;
import com.sshtools.rfb.encoding.CopyRectEncoding;
import com.sshtools.rfb.encoding.CursorPositionEncoding;
import com.sshtools.rfb.encoding.HextileEncoding;
import com.sshtools.rfb.encoding.LastRectEncoding;
import com.sshtools.rfb.encoding.RFBResizeEncoding;
import com.sshtools.rfb.encoding.RREEncoding;
import com.sshtools.rfb.encoding.RawEncoding;
import com.sshtools.rfb.encoding.RichCursorEncoding;
import com.sshtools.rfb.encoding.TightEncoding;
import com.sshtools.rfb.encoding.XCursorEncoding;
import com.sshtools.rfb.encoding.ZLIBEncoding;
import com.sshtools.rfb.encoding.ZRLEEncoding;

/**
 * Defines the configuration of an RFB protocol session including available
 * encodings and general preferences.
 * 
 * @author Lee David Painter
 */
public class RFBContext implements Serializable {

	public final static int PIXEL_FORMAT_AUTO = 0;
	public final static int PIXEL_FORMAT_8_BIT = 1;
	public final static int PIXEL_FORMAT_8_BIT_INDEXED = 2;
	public final static int PIXEL_FORMAT_15_BIT = 3;
	public final static int PIXEL_FORMAT_16_BIT = 4;
	public final static int PIXEL_FORMAT_32_BIT_24_BIT_COLOUR = 5;
	public final static int PIXEL_FORMAT_32_BIT = 6;
	// Not sure about these two

	public static final String PROFILE_RFB_ADAPTIVE = "RFB.Adaptive";
	public static final String PROFILE_RFB_PREFERRED_ENCODING = "RFB.PreferredEncoding";
	public static final String PROFILE_USE_COPY_RECT = "RFB.UseCopyRect";
	public static final String PROFILE_RFB_COMPRESSION_LEVEL = "RFB.CompressionLevel";
	public static final String PROFILE_RFB_PIXEL_FORMAT = "RFB.PixelFormat";
	public static final String PROFILE_RFB_REVERSE_MOUSE_BUTTONS = "RFB.ReverseMouseButtons";
	public static final String PROFILE_RFB_VIEW_ONLY = "RFB.ViewOnly";
	public static final String PROFILE_RFB_SHARE_DESKTOP = "RFB.ShareDesktop";
	public static final String PROFILE_RFB_LOCAL_CURSOR = "RFB.ShowLocalCursor";
	public static final String PROFILE_RFB_SCALE_MODE = "RFB.ScaleMode";
	public static final String PROFILE_RFB_IGNORE_CURSOR_UPDATES = "RFB.IgnoreCursorUpdates";
	public static final String PROFILE_RFB_REQUEST_CURSOR_UPDATES = "RFB.RequestCursorUpdates";
	public static final int PROFILE_SCREEN_NO_CHANGE = 0;
	public static final int PROFILE_SCREEN_REMOTE_DESKTOP = 1;
	public static final String PROFILE_RFB_MOUSE_EVENT_DELAY = "RFB.MouseEventDelay";
	public static final String PROFILE_RFB_MOUSE_EVENT_THRESHOLD = "RFB.MouseEventThreshold";
	public static final String PROFILE_RFB_JPEG_QUALITY = "RFB.TightJpegQuality";
	public static final String PROFILE_RFB_RESIZE_POLICY = "RFB.ScreenResizePolicy";

	// Deprecated - kept for existing profiles
	private static final String PROFILE_RFB_8BIT_COLOR = "RF7.8BitColor";
	
	public final static int PROFILE_VNC_SERVER_OS_WINDOWSMAC = 0;
	public final static int PROFILE_VNC_SERVER_OS_LINUX = 1;

	// Supported pixel encoding formats
	public final static int ENCODING_RAW = 0;
	public final static int ENCODING_COPYRECT = 1;
	public final static int ENCODING_RRE = 2;
	public final static int ENCODING_CORRE = 4;
	public final static int ENCODING_HEXTILE = 5;
	public final static int ENCODING_ZLIB = 6;
	public final static int ENCODING_TIGHT = 7;

	// Additional masks for encoding settings
	final static int MASK_ENCODING_COMPRESS_LEVEL = 0xFFFFFF00;
	final static int MASK_ENCODING_JPEG_QUALITY = 0xFFFFFFE0;
	final static int MASK_ENCODING_LAST_RECT = 0xFFFFFF20;
	final static int MASK_ENCODING_NEW_SIZE = 0xFFFFFF21;
	final static int MASK_ENCODING_XCURSOR = 0xFFFFFF10;
	final static int MASK_ENCODING_RICHCURSOR = 0xFFFFFF11;
	final static int MASK_ENCODING_POINTERPOS = 0xFFFFFF18;

	private transient Hashtable encodings = new Hashtable();

	private int preferredEncoding = ENCODING_TIGHT;
	private int compressLevel = 8;
	private int screenSizePolicy = PROFILE_SCREEN_NO_CHANGE;
	private int scaleMode = -1;
	private boolean useCopyRect;
	private int pixelFormat = PIXEL_FORMAT_AUTO;
	private boolean reverseMouseButtons2And3 = false;
	private boolean viewOnly = false;
	private boolean shareDesktop = false;
	private boolean showLocalCursor = true;
	private int mouseEventDelay = 0;
	private int mouseEventThreshold = 40;
	private boolean requestCursorUpdates = true;
	private boolean ignoreCursorUpdates = false;
	private int jpegQuality = 6;
	private int cursorUpdateTimeout = 10;
	private int screenUpdateTimeout = 0;
	private int deferUpdateRequests = 20;
	private boolean adaptive;

	ResourceProfile profile;

	public RFBContext(ResourceProfile profile) {
		this();
		this.profile = profile;
		setProfile(profile);
	}

	public RFBContext() {
		resetEncodings();
	}

	public void resetEncodings() {
		if(encodings == null) {
			encodings = new Hashtable();
		}
		encodings.clear(); // perhaps encodings should have a reset() method so
							// the objects can be re-used
		registerEncoding(new RawEncoding());
		registerEncoding(new CopyRectEncoding());
		registerEncoding(new RREEncoding());
		registerEncoding(new CORREEncoding());
		registerEncoding(new HextileEncoding());
		registerEncoding(new ZLIBEncoding());
		registerEncoding(new ZRLEEncoding());
		registerEncoding(new RFBResizeEncoding());
		registerEncoding(new LastRectEncoding());
		registerEncoding(new TightEncoding());
		registerEncoding(new XCursorEncoding());
		registerEncoding(new RichCursorEncoding());
		registerEncoding(new CursorPositionEncoding());
	}

	public static boolean isConfigured(ResourceProfile profile) {
		return profile.getApplicationPropertyBoolean("RFB.isConfigured", false);
	}

	public void registerEncoding(RFBEncoding encoder) {
		encodings.put(String.valueOf(encoder.getType()), encoder);
	}

	public RFBEncoding selectEncoding(int encoding) throws IOException {
		RFBEncoding rfbEnc = (RFBEncoding) encodings.get(String
				.valueOf(encoding));
		if (rfbEnc == null) {
			throw new IOException("Unsupported encoding type! type="
					+ String.valueOf(encoding));
		}
		return rfbEnc;
	}

	public RFBEncoding getEncoding(int i) {
		return (RFBEncoding) encodings.get(String.valueOf(i));
	}

	public int getPreferredEncoding() {
		return preferredEncoding;
	}

	public void setPreferredEncoding(int preferredEncoding) {
		this.preferredEncoding = preferredEncoding;
	}

	public void setLocalCursorDisplayed(boolean showLocalCursor) {
		this.showLocalCursor = showLocalCursor;
	}

	public boolean isLocalCursorDisplayed() {
		return showLocalCursor;
	}

	public boolean isReverseMouseButtons2And3() {
		return reverseMouseButtons2And3;
	}

	public boolean isCursorUpdateIgnored() {
		return ignoreCursorUpdates;
	}

	public void setCursorUpdatesIgnored(boolean ignoreCursorUpdates) {
		this.ignoreCursorUpdates = ignoreCursorUpdates;
	}

	public boolean isCursorUpdatesRequested() {
		return requestCursorUpdates;
	}

	public void setCursorUpdatesRequested(boolean requestCursorUpdates) {
		this.requestCursorUpdates = requestCursorUpdates;
	}

	public boolean isViewOnly() {
		return viewOnly;
	}

	public boolean isShareDesktop() {
		return shareDesktop;
	}

	public int getCompressLevel() {
		return compressLevel;
	}

	public int getPixelFormat() {
		return pixelFormat;
	}

	public void setPixelFormat(int pixelFormat) {
		this.pixelFormat = pixelFormat;
	}

	public void setScaleMode(int scaleMode) {
		this.scaleMode = scaleMode;
	}

	public int getScreenSizePolicy() {
		return screenSizePolicy;
	}

	public int getScaleMode() {
		return scaleMode;
	}

	public int getMouseEventDelay() {
		return mouseEventDelay;
	}

	public void setMouseEventDelay(int mouseEventDelay) {
		this.mouseEventDelay = mouseEventDelay;
	}

	public int getMouseEventThreshold() {
		return mouseEventThreshold;
	}

	public void setMouseEventThreshold(int mouseEventThreshold) {
		this.mouseEventThreshold = mouseEventThreshold;
	}

	public int[] getEncodings() {

		Vector v = new Vector();

		if (requestCursorUpdates) {
			v.addElement(new Integer(MASK_ENCODING_RICHCURSOR)); // Rich Cursor
			v.addElement(new Integer(MASK_ENCODING_XCURSOR)); // X Cursor

			if (!ignoreCursorUpdates) {
				v.addElement(new Integer(MASK_ENCODING_POINTERPOS)); // Pointer
																		// pos
			}
		}

		v.addElement(new Integer(preferredEncoding));

		if (useCopyRect) {
			v.addElement(new Integer(ENCODING_COPYRECT));
		}

		RFBEncoding e;
		for (Enumeration en = encodings.elements(); en.hasMoreElements();) {
			e = (RFBEncoding) en.nextElement();
			if (e.getType() != preferredEncoding
					&& e.getType() != ENCODING_COPYRECT
					&& !e.isPseudoEncoding()) {
				v.addElement(new Integer(e.getType()));
			}
		}

		if (preferredEncoding == ENCODING_ZLIB
				|| preferredEncoding == ENCODING_TIGHT) {
			if (compressLevel >= 0 && compressLevel <= 9) {
				v.addElement(new Integer(MASK_ENCODING_COMPRESS_LEVEL
						+ compressLevel));
			}
		}

		if (preferredEncoding == ENCODING_TIGHT && jpegQuality > -1) {
			v.addElement(new Integer(MASK_ENCODING_JPEG_QUALITY + jpegQuality));
			;
		}

		v.addElement(new Integer(MASK_ENCODING_LAST_RECT));
		v.addElement(new Integer(MASK_ENCODING_NEW_SIZE));

		int[] ret = new int[v.size()];
		for (int i = 0; i < v.size(); i++) {
			ret[i] = ((Integer) v.elementAt(i)).intValue();
		}

		return ret;
	}

	public void setCursorUpdateTimeout(int cursorUpdateTimeout) {
		this.cursorUpdateTimeout = cursorUpdateTimeout;
	}

	public int getCursorUpdateTimeout() {
		return cursorUpdateTimeout;
	}

	public void setScreenUpdateTimeout(int screenUpdateTimeout) {
		this.screenUpdateTimeout = screenUpdateTimeout;
	}

	public int getScreenUpdateTimeout() {
		return screenUpdateTimeout;
	}

	public void setDeferUpdateRequests(int deferUpdateRequests) {
		this.deferUpdateRequests = deferUpdateRequests;
	}

	public int getDeferUpdateRequests() {
		return deferUpdateRequests;
	}

	public boolean isUseCopyRect() {
		return useCopyRect;
	}

	public void setUseCopyRect(boolean useCopyRect) {
		this.useCopyRect = useCopyRect;
	}

	/**
	 * Set the context up from a profile
	 * 
	 * @param profile
	 *            profile
	 */
	public void setProfile(ResourceProfile profile) {
		this.profile = profile;
		adaptive = profile.getApplicationPropertyBoolean(PROFILE_RFB_ADAPTIVE,
				false);
		preferredEncoding = profile.getApplicationPropertyInt(
				PROFILE_RFB_PREFERRED_ENCODING, ENCODING_ZLIB);
		compressLevel = profile.getApplicationPropertyInt(
				PROFILE_RFB_COMPRESSION_LEVEL, -1);
		useCopyRect = profile.getApplicationPropertyBoolean(
				PROFILE_USE_COPY_RECT, true);
		screenSizePolicy = profile.getApplicationPropertyInt(
				PROFILE_RFB_RESIZE_POLICY, PROFILE_SCREEN_NO_CHANGE);
		useCopyRect = profile.getApplicationPropertyBoolean(
				PROFILE_USE_COPY_RECT, true);
		pixelFormat = profile
				.getApplicationPropertyInt(PROFILE_RFB_PIXEL_FORMAT, profile
						.getApplicationPropertyBoolean(PROFILE_RFB_8BIT_COLOR,
								false) ? PIXEL_FORMAT_8_BIT : PIXEL_FORMAT_AUTO);
		reverseMouseButtons2And3 = profile.getApplicationPropertyBoolean(
				PROFILE_RFB_REVERSE_MOUSE_BUTTONS, false);
		viewOnly = profile.getApplicationPropertyBoolean(PROFILE_RFB_VIEW_ONLY,
				false);
		shareDesktop = profile.getApplicationPropertyBoolean(
				PROFILE_RFB_SHARE_DESKTOP, false);
		requestCursorUpdates = profile.getApplicationPropertyBoolean(
				PROFILE_RFB_REQUEST_CURSOR_UPDATES, true);
		ignoreCursorUpdates = profile.getApplicationPropertyBoolean(
				PROFILE_RFB_IGNORE_CURSOR_UPDATES, false);
		scaleMode = profile
				.getApplicationPropertyInt(PROFILE_RFB_SCALE_MODE, 0);
		showLocalCursor = profile.getApplicationPropertyBoolean(
				PROFILE_RFB_LOCAL_CURSOR, true);
		jpegQuality = profile.getApplicationPropertyInt(
				PROFILE_RFB_JPEG_QUALITY, 6);
		mouseEventDelay = profile.getApplicationPropertyInt(
				PROFILE_RFB_MOUSE_EVENT_DELAY, 0);
		mouseEventThreshold = profile.getApplicationPropertyInt(
				PROFILE_RFB_MOUSE_EVENT_THRESHOLD, 40);

	}

	/**
	 * @return Returns the jpegQuality.
	 */
	public int getJpegQuality() {
		return jpegQuality;
	}

	/**
	 * @param jpegQuality
	 *            The jpegQuality to set.
	 */
	public void setJpegQuality(int jpegQuality) {
		this.jpegQuality = jpegQuality;
	}

	/**
	 * @return Returns the adaptive.
	 */
	public boolean isAdaptive() {
		return adaptive;
	}

	/**
	 * @param adaptive
	 *            The adaptive to set.
	 */
	public void setAdaptive(boolean adaptive) {
		this.adaptive = adaptive;
	}

	public void setViewOnly(boolean viewOnly) {
		this.viewOnly = viewOnly;

	}
}