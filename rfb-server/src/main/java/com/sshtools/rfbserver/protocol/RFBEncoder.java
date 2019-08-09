/**
 * RFB Server - Remote Frame Buffer (VNC Server) implementation. This is the base module if you want to create a VNC server. It takes a layered driver approach to add native specific features (which is recommened as the cross-platform default "Robot" driver is very slow).
 *
 * See the vncserver module for a concrete server implementation that has some native performance improvements for some platforms.
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
package com.sshtools.rfbserver.protocol;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.PixelFormatImageFactory;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.ScreenData;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.Beep;
import com.sshtools.rfbserver.DisplayDriver;
import com.sshtools.rfbserver.DisplayDriver.PointerShape;
import com.sshtools.rfbserver.FrameBufferUpdate;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.ServerCut;
import com.sshtools.rfbserver.UpdateRectangle;
import com.sshtools.rfbserver.encodings.CORREEncoding;
import com.sshtools.rfbserver.encodings.CompressLevel0;
import com.sshtools.rfbserver.encodings.CompressLevel1;
import com.sshtools.rfbserver.encodings.CompressLevel2;
import com.sshtools.rfbserver.encodings.CompressLevel3;
import com.sshtools.rfbserver.encodings.CompressLevel4;
import com.sshtools.rfbserver.encodings.CompressLevel5;
import com.sshtools.rfbserver.encodings.CompressLevel6;
import com.sshtools.rfbserver.encodings.CompressLevel7;
import com.sshtools.rfbserver.encodings.CompressLevel8;
import com.sshtools.rfbserver.encodings.CompressLevel9;
import com.sshtools.rfbserver.encodings.ContinuousUpdatesEncoding;
import com.sshtools.rfbserver.encodings.CopyRectEncoding;
import com.sshtools.rfbserver.encodings.CursorEncoding;
import com.sshtools.rfbserver.encodings.CursorPositionEncoding;
import com.sshtools.rfbserver.encodings.ExtendedDesktopSizeEncoding;
import com.sshtools.rfbserver.encodings.HextileEncoding;
import com.sshtools.rfbserver.encodings.JPEGQualityLevel0;
import com.sshtools.rfbserver.encodings.JPEGQualityLevel1;
import com.sshtools.rfbserver.encodings.JPEGQualityLevel2;
import com.sshtools.rfbserver.encodings.JPEGQualityLevel3;
import com.sshtools.rfbserver.encodings.JPEGQualityLevel4;
import com.sshtools.rfbserver.encodings.JPEGQualityLevel5;
import com.sshtools.rfbserver.encodings.JPEGQualityLevel6;
import com.sshtools.rfbserver.encodings.JPEGQualityLevel7;
import com.sshtools.rfbserver.encodings.JPEGQualityLevel8;
import com.sshtools.rfbserver.encodings.JPEGQualityLevel9;
import com.sshtools.rfbserver.encodings.RFBResizeEncoding;
import com.sshtools.rfbserver.encodings.RFBServerEncoding;
import com.sshtools.rfbserver.encodings.RREEncoding;
import com.sshtools.rfbserver.encodings.RawEncoding;
import com.sshtools.rfbserver.encodings.TightEncoding;
import com.sshtools.rfbserver.encodings.TightPNGEncoding;
import com.sshtools.rfbserver.encodings.XCursorEncoding;
import com.sshtools.rfbserver.encodings.ZLIBEncoding;
import com.sshtools.rfbserver.encodings.ZRLEEncoding;

public class RFBEncoder {
	final static Logger LOG = LoggerFactory.getLogger(RFBEncoder.class);
	private Map<Integer, RFBServerEncoding<?>> encodings = new HashMap<Integer, RFBServerEncoding<?>>();
	private List<Integer> enabledEncodings = new ArrayList<Integer>();
	private List<Reply<? extends Object>> damaged = new ArrayList<Reply<?>>();
	private Object lock = new Object();
	private Object waitLock = new Object();
	private RFBClient client;
	private boolean pointerShapeSent;

	public RFBEncoder(RFBClient client) {
		this.client = client;
		addEncoding(new RawEncoding());
		addEncoding(new RREEncoding());
		addEncoding(new ZLIBEncoding());
		addEncoding(new CompressLevel0());
		addEncoding(new CompressLevel1());
		addEncoding(new CompressLevel2());
		addEncoding(new CompressLevel3());
		addEncoding(new CompressLevel4());
		addEncoding(new CompressLevel5());
		addEncoding(new CompressLevel6());
		addEncoding(new CompressLevel7());
		addEncoding(new CompressLevel8());
		addEncoding(new CompressLevel9());
		addEncoding(new HextileEncoding());
		addEncoding(new TightEncoding());
		addEncoding(new TightPNGEncoding());
		addEncoding(new JPEGQualityLevel0());
		addEncoding(new JPEGQualityLevel1());
		addEncoding(new JPEGQualityLevel2());
		addEncoding(new JPEGQualityLevel3());
		addEncoding(new JPEGQualityLevel4());
		addEncoding(new JPEGQualityLevel5());
		addEncoding(new JPEGQualityLevel6());
		addEncoding(new JPEGQualityLevel7());
		addEncoding(new JPEGQualityLevel8());
		addEncoding(new JPEGQualityLevel9());
		addEncoding(new ZRLEEncoding());
		addEncoding(new RFBResizeEncoding());
		addEncoding(new CursorEncoding());
		addEncoding(new CursorPositionEncoding());
		addEncoding(new XCursorEncoding());
		addEncoding(new CORREEncoding());
		addEncoding(new CopyRectEncoding());
		addEncoding(new ExtendedDesktopSizeEncoding());
		addEncoding(new ContinuousUpdatesEncoding());
	}

	public void addEncoding(RFBServerEncoding<?> enc) {
		if (encodings.containsKey(enc.getCode())) {
			throw new IllegalArgumentException(
					"Encoding with code " + enc.getCode() + " already exists (" + encodings.get(enc.getCode()) + ")");
		}
		encodings.put(enc.getType().getCode(), enc);
	}

	public void removeEncoding(RFBServerEncoding<?> enc) {
		enabledEncodings.remove((Object) enc.getType().getCode());
		encodings.remove(enc.getType().getCode());
	}

	public void resetEncodings() {
		enabledEncodings.clear();
		enabledEncodings.add(RFBConstants.ENC_RAW);
	}

	public boolean isEncodingEnabled(int type) {
		return enabledEncodings.contains(type);
	}

	public List<TightCapability> getAvailableEncodingsAsCapabilities() {
		List<TightCapability> e = new ArrayList<TightCapability>();
		for (Integer i : encodings.keySet()) {
			RFBServerEncoding<?> enc = encodings.get(i);
			if (enc == null) {
				throw new IllegalStateException("Encoding " + i + " is enabled but unknown");
			}
			e.add(enc.getType());
		}
		return e;
	}

	public List<RFBServerEncoding<?>> getEnabledEncodings() {
		List<RFBServerEncoding<?>> e = new ArrayList<RFBServerEncoding<?>>();
		for (Integer i : enabledEncodings) {
			e.add(encodings.get(i));
		}
		return e;
	}

	public RFBServerEncoding<?> getEnabledEncoding(int type) {
		return encodings.containsKey(type) && enabledEncodings.contains(type) ? encodings.get(type) : null;
	}

	@SuppressWarnings("unchecked")
	public <T extends RFBServerEncoding<?>> T getEnabledEncoding(Class<T> clazz) {
		for (RFBServerEncoding<?> e : encodings.values()) {
			if (clazz.isAssignableFrom(e.getClass())) {
				if (enabledEncodings.contains(e.getCode()))
					return (T) e;
			}
		}
		return null;
	}

	public UpdateRectangle<? extends Object> resizeWindow(DisplayDriver displayDriver, ScreenData screenData,
			boolean clientInitiated) {
		ExtendedDesktopSizeEncoding extEncoding = getEnabledEncoding(ExtendedDesktopSizeEncoding.class);
		if (extEncoding != null) {
			UpdateRectangle<ScreenData> upd = new UpdateRectangle<ScreenData>(displayDriver,
					new Rectangle(0, 0, screenData.getWidth(), screenData.getHeight()), extEncoding.getType().getCode());
			upd.setData(screenData);
			queueUpdate(upd);
			return upd;
		} else {
			RFBResizeEncoding resizeEncoding = getEnabledEncoding(RFBResizeEncoding.class);
			UpdateRectangle<Void> updateRectangle = resizeEncoding == null ? null
					: new UpdateRectangle<Void>(displayDriver, new Rectangle(0, 0, screenData.getWidth(), screenData.getHeight()),
							resizeEncoding.getType().getCode());
			if (updateRectangle != null) {
				queueUpdate(updateRectangle);
			}
			return updateRectangle;
		}
	}

	public UpdateRectangle<Void> pointerPositionUpdate(DisplayDriver displayDriver, int x, int y) {
		CursorPositionEncoding cursorPositionEncoding = getEnabledEncoding(CursorPositionEncoding.class);
		UpdateRectangle<Void> updateRectangle = cursorPositionEncoding == null ? null
				: new UpdateRectangle<Void>(displayDriver, new Rectangle(x, y, 0, 0), cursorPositionEncoding.getType().getCode());
		if (updateRectangle != null) {
			queueUpdate(updateRectangle);
		}
		return updateRectangle;
	}

	public UpdateRectangle<PointerShape> pointerShapeUpdate(DisplayDriver displayDriver, PointerShape change) {
		RFBServerEncoding<PointerShape> pointerShapeEncoding = getEnabledEncoding(CursorEncoding.class);
		if (pointerShapeEncoding == null) {
			pointerShapeEncoding = getEnabledEncoding(XCursorEncoding.class);
			if (pointerShapeEncoding == null) {
				return null;
			}
		}
		UpdateRectangle<PointerShape> upd = new UpdateRectangle<PointerShape>(displayDriver,
				new Rectangle(change.getHotX(), change.getHotY(), change.getWidth(), change.getHeight()),
				pointerShapeEncoding.getType().getCode());
		upd.setData(change);
		queueUpdate(upd);
		return upd;
	}

	public UpdateRectangle<BufferedImage> frameUpdate(DisplayDriver displayDriver, Rectangle rectangle, int preferredEncoding) {
		return frameUpdate(displayDriver, rectangle, false, preferredEncoding);
	}

	public UpdateRectangle<BufferedImage> frameUpdate(DisplayDriver displayDriver, Rectangle rectangle, boolean full,
			int preferredEncoding) {
		UpdateRectangle<BufferedImage> rect = doFrameUpdate(displayDriver, rectangle, null, full, preferredEncoding);
		if (rect != null) {
			queueUpdate(rect);
		}
		return rect;
	}

	private UpdateRectangle<BufferedImage> doFrameUpdate(DisplayDriver displayDriver, Rectangle rectangle,
			UpdateRectangle<BufferedImage> update, boolean full, int updatePreferredEncoding) {
		assert rectangle != null;
		assert displayDriver != null;
		if (rectangle.x < 0) {
			rectangle.x = 0;
		} else if (rectangle.x > displayDriver.getWidth()) {
			rectangle.x = displayDriver.getWidth();
		}
		if (rectangle.y < 0) {
			rectangle.y = 0;
		} else if (rectangle.y > displayDriver.getHeight()) {
			rectangle.y = displayDriver.getHeight();
		}
		if (rectangle.width + rectangle.x > displayDriver.getWidth()) {
			rectangle.width = displayDriver.getWidth() - rectangle.x;
		}
		if (rectangle.height + rectangle.y > displayDriver.getHeight()) {
			rectangle.height = displayDriver.getHeight() - rectangle.y;
		}
		if (rectangle.width == 0 || rectangle.height == 0) {
			// Update is out of bounds
			LOG.warn("Rectangle out of bounds, skipping: " + rectangle);
			return null;
		}
		boolean found = false;
		Rectangle requestedArea = client.getRequestedArea();
		if (full || requestedArea == null) {
			found = true;
		} else {
			found = requestedArea.intersects(rectangle);
		}
		if (!found) {
			/*
			 * Neither using continuous requests mode nor is the update
			 * intersecting any areas the client said they are interested in
			 */
			return null;
		}
		if (update == null) {
			update = new UpdateRectangle<BufferedImage>(displayDriver, rectangle, updatePreferredEncoding);
		} else {
			update.setEncoding(updatePreferredEncoding);
			update.setArea(rectangle);
		}
		// Create compatible image to send
		BufferedImage img = new PixelFormatImageFactory(client.getPixelFormat()).create(rectangle.width, rectangle.height);
		img.getGraphics().drawImage(displayDriver.grabArea(rectangle), 0, 0, null);
		update.setData(img);
		return update;
	}

	public int getPreferredEncoding() {
		for (int i : enabledEncodings) {
			if (encodings.containsKey(i) && !encodings.get(i).isPseudoEncoding() && i != RFBConstants.ENC_COPYRECT) {
				return i;
			}
		}
		return RFBConstants.ENC_RAW;
	}

	public boolean isAvailable(int enc) {
		return encodings.containsKey(enc);
	}

	public void clearEnabled() {
		enabledEncodings.clear();
	}

	public void enable(RFBClient client, int enc) {
		RFBServerEncoding<?> enco = encodings.get(enc);
		if (enco == null) {
			LOG.warn("No such encoding as " + enc);
		} else {
			enabledEncodings.add(enc);
			enco.selected(client);
			LOG.info("Enabling " + enco.getType().getSignature());
		}
	}

	private void queueUpdate(int position, Reply<?> reply) {
		if (reply != null) {
			synchronized (lock) {
				damaged.add(position == -1 ? damaged.size() : position, reply);
				synchronized (waitLock) {
					waitLock.notifyAll();
				}
			}
		}
	}

	// private void queueUpdate(int position, Reply<?> reply) {
	// if (reply != null) {
	// synchronized (lock) {
	// for (Iterator<Reply<?>> it = damaged.iterator(); it.hasNext();) {
	// Reply<?> r = it.next();
	// if (reply r instanceof UpdateRectangle) {
	// UpdateRectangle<?> u = (UpdateRectangle<?>) r;
	// RFBServerEncoding enc = u.getEncoding() == -1 ? null :
	// encodings.get(u.getEncoding());
	// if (enc == null || !enc.isPseudoEncoding()) {
	// Rectangle area = u.getArea();
	// UpdateRectangle<?> update = (UpdateRectangle<?>) reply;
	// Rectangle updateArea = update.getArea();
	// if (area.contains(updateArea) || area.equals(updateArea)) {
	// // There is already an update that covers this
	// // area
	// if (!update.isImportant()) {
	// // TODO optimise to only update the enclosed
	// // rectangle
	// doFrameUpdate(update.getDriver(), update.getArea(),
	// (UpdateRectangle<BufferedImage>) update,
	// update.isImportant(), update.getEncoding());
	//
	// return;
	// }
	// } else if (updateArea.contains(area)) {
	// // Our update contains an existing update
	// if (!u.isImportant()) {
	// it.remove();
	// }
	// }
	// }
	// }
	// }
	// LOG.info("Damaged " + reply);
	// damaged.add(position == -1 ? damaged.size() : position, reply);
	// synchronized (waitLock) {
	// waitLock.notifyAll();
	// }
	// }
	// }
	// }
	@SuppressWarnings("unchecked")
	public <T> UpdateRectangle<T> queueUpdate(UpdateRectangle<T> update) {
		synchronized (lock) {
			// Look for a frame buffer update to add the rectangle too
			FrameBufferUpdate fbu = null;
			for (Reply<?> f : damaged) {
				if (f instanceof FrameBufferUpdate) {
					fbu = (FrameBufferUpdate) f;
					break;
				}
			}
			if (fbu == null) {
				fbu = new FrameBufferUpdate(client.getPixelFormat(), this);
				fbu.getData().add((UpdateRectangle<Object>) update);
				queueUpdate(fbu);
			} else {
				fbu.getData().add((UpdateRectangle<Object>) update);
			}
			return update;
		}
	}

	public RFBClient getClient() {
		return client;
	}

	public void queueUpdate(Reply<?> update) {
		queueUpdate(-1, update);
	}

	public Object getLock() {
		return lock;
	}

	public List<Reply<?>> popUpdates() {
		synchronized (lock) {
			List<Reply<?>> rep = new ArrayList<>(damaged);
			damaged.clear();
			return rep;
		}
	}

	public void beep(DisplayDriver displayDriver) {
		queueUpdate(new Beep());
	}

	public void clipboardChanged(DisplayDriver displayDriver, FlavorEvent e) {
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
		try {
			queueUpdate(new ServerCut((String) t.getTransferData(DataFlavor.stringFlavor)));
		} catch (Exception ex) {
			LOG.error("Failed to get clipboard contents.", ex);
		}
	}

	public boolean waitForUpdates(long timeout) {
		synchronized (waitLock) {
			if (!damaged.isEmpty()) {
				return true;
			}
			try {
				waitLock.wait(timeout);
			} catch (InterruptedException e) {
			}
			return !damaged.isEmpty();
		}
	}

	public void clearUpdates() {
		damaged.clear();
	}

	public boolean isPointerShapeSent() {
		return pointerShapeSent;
	}

	public void pointerShapeSent() {
		pointerShapeSent = true;
	}

	public void resetPointerShape() {
		pointerShapeSent = false;
	}
}
