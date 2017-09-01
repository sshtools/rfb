/* HEADER */
package com.sshtools.rfb;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfb.encoding.CORREEncoding;
import com.sshtools.rfb.encoding.CompressLevel0Encoding;
import com.sshtools.rfb.encoding.CompressLevel1Encoding;
import com.sshtools.rfb.encoding.CompressLevel2Encoding;
import com.sshtools.rfb.encoding.CompressLevel3Encoding;
import com.sshtools.rfb.encoding.CompressLevel4Encoding;
import com.sshtools.rfb.encoding.CompressLevel5Encoding;
import com.sshtools.rfb.encoding.CompressLevel6Encoding;
import com.sshtools.rfb.encoding.CompressLevel7Encoding;
import com.sshtools.rfb.encoding.CompressLevel8Encoding;
import com.sshtools.rfb.encoding.CompressLevel9Encoding;
import com.sshtools.rfb.encoding.ContinuousUpdatesEncoding;
import com.sshtools.rfb.encoding.CopyRectEncoding;
import com.sshtools.rfb.encoding.CursorPositionEncoding;
import com.sshtools.rfb.encoding.ExtendedDesktopSizeEncoding;
import com.sshtools.rfb.encoding.HextileEncoding;
import com.sshtools.rfb.encoding.JPEGQuality0Encoding;
import com.sshtools.rfb.encoding.JPEGQuality1Encoding;
import com.sshtools.rfb.encoding.JPEGQuality2Encoding;
import com.sshtools.rfb.encoding.JPEGQuality3Encoding;
import com.sshtools.rfb.encoding.JPEGQuality4Encoding;
import com.sshtools.rfb.encoding.JPEGQuality5Encoding;
import com.sshtools.rfb.encoding.JPEGQuality6Encoding;
import com.sshtools.rfb.encoding.JPEGQuality7Encoding;
import com.sshtools.rfb.encoding.JPEGQuality8Encoding;
import com.sshtools.rfb.encoding.JPEGQuality9Encoding;
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
import com.sshtools.rfbcommon.RFBConstants;

/**
 * Defines the configuration of an RFB protocol session including available
 * encodings and general preferences.
 */
public class RFBContext implements Serializable {
	final static Logger LOG = LoggerFactory.getLogger(RFBContext.class);
	private static final long serialVersionUID = 1413968852790401177L;
	public final static int PIXEL_FORMAT_AUTO = 0;
	public final static int PIXEL_FORMAT_8_BIT = 1;
	public final static int PIXEL_FORMAT_8_BIT_INDEXED = 2;
	public final static int PIXEL_FORMAT_15_BIT = 3;
	public final static int PIXEL_FORMAT_16_BIT = 4;
	public final static int PIXEL_FORMAT_32_BIT_24_BIT_COLOUR = 5;
	public final static int PIXEL_FORMAT_32_BIT = 6;
	private transient Map<Integer, RFBEncoding> encodings = new HashMap<Integer, RFBEncoding>();
	private int preferredEncoding = RFBConstants.ENC_TIGHT;
	private int compressLevel = 0;
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
	private boolean continuousUpdates = false;

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
		registerEncoding(new ExtendedDesktopSizeEncoding());
		registerEncoding(new ContinuousUpdatesEncoding());
		registerEncoding(new LastRectEncoding());
		registerEncoding(new TightEncoding());
		registerEncoding(new TightPNGEncoding());
		registerEncoding(new XCursorEncoding());
		registerEncoding(new RichCursorEncoding());
		registerEncoding(new CursorPositionEncoding());
		registerEncoding(new JPEGQuality0Encoding());
		registerEncoding(new JPEGQuality1Encoding());
		registerEncoding(new JPEGQuality2Encoding());
		registerEncoding(new JPEGQuality3Encoding());
		registerEncoding(new JPEGQuality4Encoding());
		registerEncoding(new JPEGQuality5Encoding());
		registerEncoding(new JPEGQuality6Encoding());
		registerEncoding(new JPEGQuality7Encoding());
		registerEncoding(new JPEGQuality8Encoding());
		registerEncoding(new JPEGQuality9Encoding());
		registerEncoding(new CompressLevel0Encoding());
		registerEncoding(new CompressLevel1Encoding());
		registerEncoding(new CompressLevel2Encoding());
		registerEncoding(new CompressLevel3Encoding());
		registerEncoding(new CompressLevel4Encoding());
		registerEncoding(new CompressLevel5Encoding());
		registerEncoding(new CompressLevel6Encoding());
		registerEncoding(new CompressLevel7Encoding());
		registerEncoding(new CompressLevel8Encoding());
		registerEncoding(new CompressLevel9Encoding());
	}

	public boolean isContinuousUpdates() {
		return continuousUpdates;
	}

	public void setContinuousUpdates(boolean continuousUpdates) {
		this.continuousUpdates = continuousUpdates;
	}

	public void registerEncoding(RFBEncoding encoder) {
		encodings.put(encoder.getType(), encoder);
	}

	public RFBEncoding selectEncoding(int encoding) throws IOException {
		RFBEncoding rfbEnc = encodings.get(encoding);
		if (rfbEnc == null) {
			throw new IOException("Unsupported encoding type! type=" + encoding);
		}
		return rfbEnc;
	}

	public RFBEncoding getEncoding(int i) {
		return encodings.get(i);
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
			v.add(RFBConstants.ENC_RICH_CURSOR);
			v.add(RFBConstants.ENC_X11_CURSOR);
			if (!ignoreCursorUpdates) {
				v.add(RFBConstants.ENC_POINTER_POS);
			}
		}
		v.add(new Integer(preferredEncoding));
		if (useCopyRect) {
			v.add(RFBConstants.ENC_COPYRECT);
		}
		for (RFBEncoding e : encodings.values()) {
			if (e.getType() != preferredEncoding && e.getType() != RFBConstants.ENC_COPYRECT && !e.isPseudoEncoding()) {
				v.add(e.getType());
			}
		}
		if (preferredEncoding == RFBConstants.ENC_ZLIB || preferredEncoding == RFBConstants.ENC_TIGHT
				|| preferredEncoding == RFBConstants.ENC_TIGHT_PNG) {
			if (compressLevel >= 0 && compressLevel <= 9) {
				v.add(new Integer(RFBConstants.ENC_COMPRESS_LEVEL0 + compressLevel));
			}
		}
		if ((preferredEncoding == RFBConstants.ENC_TIGHT || preferredEncoding == RFBConstants.ENC_TIGHT_PNG) && jpegQuality > -1) {
			v.add(new Integer(RFBConstants.ENC_JPEG_QUALITY_LEVEL0 + jpegQuality));
		}
		v.add(RFBConstants.ENC_LAST_RECT);
		v.add(RFBConstants.ENC_NEW_FB_SIZE);
		v.add(RFBConstants.ENC_CONTINUOUS_UPDATES);
		 v.add(RFBConstants.ENC_EXTENDED_FB_SIZE);
		for (Iterator<Integer> it = v.iterator(); it.hasNext();) {
			int id = it.next();
			if (getEncoding(id) == null) {
				LOG.warn(String.format("Removing missing encoding %d", id));
				it.remove();
			}
		}
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