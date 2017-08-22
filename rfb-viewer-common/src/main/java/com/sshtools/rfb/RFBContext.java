/* HEADER */
package com.sshtools.rfb;

import java.util.List;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import com.sshtools.rfb.encoding.TightPNGEncoding;
import com.sshtools.rfb.encoding.XCursorEncoding;
import com.sshtools.rfb.encoding.ZLIBEncoding;
import com.sshtools.rfb.encoding.ZRLEEncoding;

/**
 * Defines the configuration of an RFB protocol session including available
 * encodings and general preferences.
 */
public class RFBContext implements Serializable {

	private static final long serialVersionUID = 1413968852790401177L;

	public final static int PIXEL_FORMAT_AUTO = 0;
	public final static int PIXEL_FORMAT_8_BIT = 1;
	public final static int PIXEL_FORMAT_8_BIT_INDEXED = 2;
	public final static int PIXEL_FORMAT_15_BIT = 3;
	public final static int PIXEL_FORMAT_16_BIT = 4;
	public final static int PIXEL_FORMAT_32_BIT_24_BIT_COLOUR = 5;
	public final static int PIXEL_FORMAT_32_BIT = 6;
	// Not sure about these two

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

	private transient Map<String, RFBEncoding> encodings = new HashMap<String, RFBEncoding>();

	private int preferredEncoding = ENCODING_TIGHT;
	private int compressLevel = 8;
	private int screenSizePolicy = 0;
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

	public RFBContext() {
		resetEncodings();
	}

	public void resetEncodings() {
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
		registerEncoding(new TightPNGEncoding());
		registerEncoding(new XCursorEncoding());
		registerEncoding(new RichCursorEncoding());
		registerEncoding(new CursorPositionEncoding());
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

		List<Integer> v = new ArrayList<Integer>();

		if (requestCursorUpdates) {
			v.add(new Integer(MASK_ENCODING_RICHCURSOR)); // Rich Cursor
			v.add(new Integer(MASK_ENCODING_XCURSOR)); // X Cursor

			if (!ignoreCursorUpdates) {
				v.add(new Integer(MASK_ENCODING_POINTERPOS)); // Pointer
																// pos
			}
		}

		v.add(new Integer(preferredEncoding));

		if (useCopyRect) {
			v.add(new Integer(ENCODING_COPYRECT));
		}

		for (RFBEncoding e : encodings.values()) {
			if (e.getType() != preferredEncoding
					&& e.getType() != ENCODING_COPYRECT
					&& !e.isPseudoEncoding()) {
				v.add(new Integer(e.getType()));
			}
		}

		if (preferredEncoding == ENCODING_ZLIB
				|| preferredEncoding == ENCODING_TIGHT) {
			if (compressLevel >= 0 && compressLevel <= 9) {
				v.add(new Integer(MASK_ENCODING_COMPRESS_LEVEL + compressLevel));
			}
		}

		if (preferredEncoding == ENCODING_TIGHT && jpegQuality > -1) {
			v.add(new Integer(MASK_ENCODING_JPEG_QUALITY + jpegQuality));
		}

		v.add(new Integer(MASK_ENCODING_LAST_RECT));
		v.add(new Integer(MASK_ENCODING_NEW_SIZE));

		int[] ret = new int[v.size()];
		for (int i = 0; i < v.size(); i++) {
			ret[i] = v.get(i);
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

	public int getJpegQuality() {
		return jpegQuality;
	}

	public void setJpegQuality(int jpegQuality) {
		this.jpegQuality = jpegQuality;
	}

	public boolean isAdaptive() {
		return adaptive;
	}

	public void setAdaptive(boolean adaptive) {
		this.adaptive = adaptive;
	}

	public void setViewOnly(boolean viewOnly) {
		this.viewOnly = viewOnly;

	}
}