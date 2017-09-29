package com.sshtools.rfbserver;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.FlavorEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.RFBVersion;
import com.sshtools.rfbcommon.ScreenData;
import com.sshtools.rfbserver.DisplayDriver.DamageListener;
import com.sshtools.rfbserver.DisplayDriver.PointerListener;
import com.sshtools.rfbserver.DisplayDriver.PointerShape;
import com.sshtools.rfbserver.DisplayDriver.ScreenBoundsListener;
import com.sshtools.rfbserver.DisplayDriver.UpdateListener;
import com.sshtools.rfbserver.DisplayDriver.WindowListener;
import com.sshtools.rfbserver.RFBAuthenticator.AuthenticationException;
import com.sshtools.rfbserver.drivers.FilteredDisplayDriver;
import com.sshtools.rfbserver.files.RFBServerFS;
import com.sshtools.rfbserver.files.tight.TightFileTransferProtocolExtension;
import com.sshtools.rfbserver.files.uvnc.UVNCFileTransferProtocolExtension;
import com.sshtools.rfbserver.protocol.ClientCutTextProtocolExtension;
import com.sshtools.rfbserver.protocol.EnableContinuousUpdatesProtocolExtension;
import com.sshtools.rfbserver.protocol.KeyboardEventProtocolExtension;
import com.sshtools.rfbserver.protocol.NewEncodingsProtocolExtension;
import com.sshtools.rfbserver.protocol.NewPixelFormatProtocolExtension;
import com.sshtools.rfbserver.protocol.PointerEventProtocolExtension;
import com.sshtools.rfbserver.protocol.ProtocolExtension;
import com.sshtools.rfbserver.protocol.RFBEncoder;
import com.sshtools.rfbserver.protocol.Reply;
import com.sshtools.rfbserver.protocol.SetDesktopSizeExtension;
import com.sshtools.rfbserver.transport.RFBServerTransport;

public class RFBClient implements DamageListener, PointerListener, ScreenBoundsListener, WindowListener, UpdateListener {
	final static Logger LOG = LoggerFactory.getLogger(RFBClient.class);
	private RFBVersion version = new RFBVersion();
	private RFBClientContext server;
	private ProtocolReader din;
	private ProtocolWriter dout;
	private boolean share;
	private DisplayDriver displayDriver;
	private PixelFormat preferredPixelFormat = new PixelFormat();
	private PixelFormat pixelFormat;
	private PointerShape currentCursor;
	private Point cursorPosition;
	private DisplayDriver actualDisplayDriver;
	private RFBEncoder encoder;
	private BufferedImage backingStore;
	private boolean ready;
	private RFBAuthenticator securityType;
	private RFBServerFS serverFileSystem;
	// private BufferUpdate requestedUpdateArea;
	private Object writeLock = new Object();
	private Map<Integer, ProtocolExtension> protocolExtensions = new HashMap<Integer, ProtocolExtension>();
	private boolean colorMapSent;
	private Rectangle requestedArea = new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
	private boolean looping;
	private boolean forceSoftCursor = false;
	private boolean continuousUpdates;

	public RFBClient(RFBClientContext server, DisplayDriver underlyingDriver) {
		encoder = new RFBEncoder(this);
		this.server = server;
		actualDisplayDriver = underlyingDriver;
		displayDriver = new SoftPointerDisplayDriver(underlyingDriver);
		currentCursor = displayDriver.getPointerShape();
		cursorPosition = displayDriver.getPointerPosition();
		protocolExtensions.put(RFBConstants.SMSG_FILE_TRANSFER, new UVNCFileTransferProtocolExtension());
		protocolExtensions.put(RFBConstants.SMSG_TIGHT_FILETRANSFER, new TightFileTransferProtocolExtension());
		protocolExtensions.put(RFBConstants.CMSG_CUT_TEXT, new ClientCutTextProtocolExtension());
		protocolExtensions.put(RFBConstants.CMSG_POINTER_EVENT, new PointerEventProtocolExtension());
		protocolExtensions.put(RFBConstants.CMSG_KEYBOARD_EVENT, new KeyboardEventProtocolExtension());
		protocolExtensions.put(RFBConstants.CMSG_SET_ENCODINGS, new NewEncodingsProtocolExtension());
		protocolExtensions.put(RFBConstants.CMSG_SET_PIXEL_FORMAT, new NewPixelFormatProtocolExtension());
		protocolExtensions.put(RFBConstants.CMSG_ENABLE_CONTINUOUS_UPDATES, new EnableContinuousUpdatesProtocolExtension());
		protocolExtensions.put(RFBConstants.CMSG_SET_DESKTOP_SIZE, new SetDesktopSizeExtension());
	}

	public boolean isForceSoftCursor() {
		return forceSoftCursor;
	}

	public void setForceSoftCursor(boolean forceSoftCursor) {
		this.forceSoftCursor = forceSoftCursor;
	}

	public boolean isShared() {
		return share;
	}

	public PixelFormat getPreferredPixelFormat() {
		return preferredPixelFormat;
	}

	public void setPreferredPixelFormat(PixelFormat preferredPixelFormat) {
		this.preferredPixelFormat = preferredPixelFormat;
	}

	public PixelFormat getPixelFormat() {
		return pixelFormat;
	}

	public Map<Integer, ProtocolExtension> getProtocolExtensions() {
		return protocolExtensions;
	}

	public RFBServerFS getServerFileSystem() {
		return serverFileSystem;
	}

	public void setServerFileSystem(RFBServerFS serverFileSystem) {
		this.serverFileSystem = serverFileSystem;
	}

	public BufferedImage getBackingStore() {
		return backingStore;
	}

	public RFBClientContext getServer() {
		return server;
	}

	public RFBServerConfiguration getConfiguration() {
		return server.getConfiguration();
	}

	public RFBEncoder getEncoder() {
		return encoder;
	}

	public RFBVersion getVersion() {
		return version;
	}

	public void run(RFBServerTransport transport, Runnable... onUpdate) throws IOException {
		try {
			encoder.resetEncodings();
			din = new ProtocolReader(transport.getInputStream());
			dout = new ProtocolWriter(transport.getOutputStream());
			// Server Protocol version
			LOG.info("Server version is " + server.getVersion());
			dout.write(server.getVersion().formatVersion());
			dout.flush();
			// Read in the version the client actually wants
			byte[] buf = new byte[12];
			din.readFully(buf);
			version.determineVersion(buf);
			LOG.info("Client version is " + version);
			List<RFBAuthenticator> securityHandlers = new ArrayList<RFBAuthenticator>(server.getSecurityHandlers());
			// Authentication method
			while (true) {
				securityType = null;
				try {
					if (securityHandlers.size() == 0) {
						securityHandlers.add(RFBAuthenticator.NO_AUTHENTICATION);
					}
					if (version.compareTo(RFBVersion.VERSION_3_7) >= 0) {
						dout.write(securityHandlers.size());
						LOG.info("Offering " + securityHandlers.size() + " authentication methods");
						for (RFBAuthenticator a : securityHandlers) {
							LOG.info("    " + getBestName(a) + " (" + a.getSecurityType() + ")");
							dout.write(a.getSecurityType());
						}
						dout.flush();
						// Wait for clients decision
						int chosen = din.read();
						LOG.info("Client chose " + chosen);
						for (RFBAuthenticator a : securityHandlers) {
							if (a.getSecurityType() == chosen) {
								securityType = a;
							}
						}
					} else {
						LOG.info("Offering legacy authentication methods");
						for (RFBAuthenticator a : securityHandlers) {
							if (a.getSecurityType() > 0 && a.getSecurityType() < 3) {
								securityType = a;
							}
						}
						if (securityType == null) {
							throw new AuthenticationException("The client has requested protocol version " + version
									+ ", which only supports 'None' or 'VNC' authentication. This server has not been configured to support these.");
						}
						LOG.info("Offering legacy authentication methods");
						LOG.info("    " + securityType.getClass().getSimpleName() + " (" + securityType.getSecurityType() + ")");
						dout.writeUInt32(securityType.getSecurityType());
						dout.flush();
					}
				} catch (AuthenticationException e) {
					LOG.error("Authentication failed. ", e);
					dout.writeUInt32(RFBConstants.SCHEME_CONNECT_FAILED);
					dout.writeString(e.getMessage());
					return;
				}
				// Authentication itself
				try {
					LOG.info("Processing " + securityType.getClass().getSimpleName());
					if (securityType.process(this)) {
						if (version.compareTo(RFBVersion.VERSION_3_8) >= 0
								|| securityType.getSecurityType() != RFBConstants.SCHEME_NO_AUTHENTICATION) {
							dout.writeUInt32(0);
							dout.flush();
						}
						// Leave the loop
						break;
					} else {
						// Continue and try again
						LOG.info("Entering sub-authentication");
						List<Integer> subAuths = securityType.getSubAuthTypes();
						if (subAuths != null) {
							for (Iterator<RFBAuthenticator> secIt = securityHandlers.iterator(); secIt.hasNext();) {
								RFBAuthenticator a = secIt.next();
								if (!subAuths.contains(a.getSecurityType())) {
									LOG.info("Removing " + a.toString()
											+ " because the previous authentication does not support it as sub-authentication.");
									secIt.remove();
								}
							}
						}
						securityHandlers.remove(securityType);
					}
				} catch (AuthenticationException ae) {
					LOG.error("Authentication error.", ae);
					if (version.compareTo(RFBVersion.VERSION_3_8) < 0) {
						throw new IOException("Disconnecting because failed authentication.");
					}
					dout.writeUInt32(1);
					if (version.compareTo(RFBVersion.VERSION_3_8) >= 0) {
						dout.writeString(ae.getMessage());
					}
					return;
				}
			}
			// ClientINit
			share = din.read() == 1;
			// ServerInit
			int width = displayDriver.getWidth();
			dout.writeShort(width);
			int height = displayDriver.getHeight();
			dout.writeShort(height);
			LOG.info("Sending desktop size of " + width + " x " + height);
			preferredPixelFormat.write(dout);
			// Set the pixel format we are actually going to use. This is a copy
			// of
			// the current preferred format, so we start off as 'direct', i.e
			// will
			// be sending pixels with no conversion. When the client sends us a
			// set pixel format, this may change
			pixelFormat = new PixelFormat(preferredPixelFormat);
			// pixelFormat.setImageType(BufferedImage.TYPE_CUSTOM);
			// pixelFormat.setType(Type.DIRECT);
			// Send the desktop name
			LOG.info("Writing desktop name " + server.getConfiguration().getDesktopName());
			dout.writeString(server.getConfiguration().getDesktopName());
			dout.flush();
			// Hooks
			LOG.info("Doing post authentication");
			securityType.postAuthentication(this);
			updateLoop(onUpdate);
		} finally {
			displayDriver.destroy();
			transport.stop();
		}
	}

	public boolean isUseSoftCursor() {
		if (forceSoftCursor)
			return true;
		boolean noCursorShapeUpdates = !encoder.isEncodingEnabled(RFBConstants.ENC_X11_CURSOR)
				&& !encoder.isEncodingEnabled(RFBConstants.ENC_RICH_CURSOR);
//		boolean noCursorPositionUpdates = !encoder.isEncodingEnabled(RFBConstants.ENC_POINTER_POS);
//		return noCursorShapeUpdates || noCursorPositionUpdates;
		return noCursorShapeUpdates;
	}

	private void sendReply(Reply<?> reply) throws IOException {
		synchronized (writeLock) {
//			LOG.info("REPLY " + reply);
			dout.write(reply.getCode());
			reply.write(dout);
			dout.flush();
		}
	}

	private String getBestName(RFBAuthenticator auth) {
		Class<?> x = auth.getClass();
		String n = "";
		while (x != null && n.length() == 0) {
			n = x.getSimpleName();
			x = x.getSuperclass();
		}
		return n;
	}

	private void updateLoop(Runnable... onUpdate) throws IOException {
		looping = true;
		displayDriver.addDamageListener(this);
		displayDriver.addPointerListener(this);
		displayDriver.addScreenBoundsListener(this);
		displayDriver.addWindowListener(this);
		displayDriver.addUpdateListener(this);
		try {
			colorMapSent = false;
			LOG.info("Using pixel format " + pixelFormat);
			while (looping) {
				for (Runnable r : onUpdate)
					r.run();
				// if (pixelFormat.isNativeFormat() && !colorMapSent &&
				// !pixelFormat.isTrueColor()) {
				if (!colorMapSent && !pixelFormat.isTrueColor()) {
					sendColourMapEntries();
				}
				// Have some updates
				List<Reply<?>> s = encoder.popUpdates();
				for (Reply<?> r : s) {
					sendReply(r);
				}
				if (din.available() == 0) {
					encoder.waitForUpdates(50);
				} else {
					int msg = din.read();
					if (msg == -1) {
						// End of stream
						break;
					} else {
						/*
						 * Wait till the first request before starting to listen
						 * for the various events from the driver
						 */
						if (!ready) {
							ready = true;
						}
						if (msg == RFBConstants.CMSG_REQUEST_FRAMEBUFFER_UPDATE) {
							requestedArea = clientRequestsFramebufferUpdate();
						} else {
							ProtocolExtension pext = protocolExtensions.get(msg);
							if (pext != null) {
								pext.handle(msg, this);
							} else {
								throw new IOException("Unexpected request " + msg);
							}
						}
						if (!encoder.isPointerShapeSent()) {
							encoder.pointerShapeSent();
							LOG.info("Sent initial cursor shape");
							pointerChange(currentCursor);
						}
					}
				}
			}
		} finally {
			LOG.info("Client complete");
			displayDriver.removeDamageListener(this);
			displayDriver.removePointerListener(this);
			displayDriver.removeScreenBoundsListener(this);
			displayDriver.removeWindowListener(this);
			displayDriver.removeUpdateListener(this);
			looping = false;
		}
	}

	public void sendColourMapEntries() throws IOException {
		synchronized (dout) {
			LOG.info("Sending colour map");
			dout.write(RFBConstants.SMSG_SET_COLORMAP);
			dout.write(0);
			dout.writeShort(0); // First colour
			Map<Integer, Integer> m = pixelFormat.getColorMap();
			LOG.info("Native color map");
			dout.writeShort(m.size());
			for (Map.Entry<Integer, Integer> en : m.entrySet()) {
				writeEntry(en.getKey(), en.getValue());
			}
			colorMapSent = true;
		}
	}

	private void writeEntry(int idx, int i) throws IOException {
		short r = (short) ((i >> 8) & 0xff00);
		dout.writeShort(r);
		short g = (short) (i & 0xff00);
		dout.writeShort(g);
		short b = (short) ((i << 8) & 0xff00);
		dout.writeShort(b);
		LOG.debug(String.format("Map %d to %s %s %s", idx, Integer.toHexString(r), Integer.toHexString(g), Integer.toHexString(b)));
	}

	public Object getWriteLock() {
		return writeLock;
	}

	protected Rectangle clientRequestsFramebufferUpdate() throws IOException {
		Rectangle update;
		// Client requested update
		boolean incremental = din.read() > 0;
		update = new Rectangle(din.readUnsignedShort(), din.readUnsignedShort(), din.readUnsignedShort(), din.readUnsignedShort());
		/* If a full update, remove all other queued framebuffer updates */
		if (!incremental) {
			encoder.frameUpdate(displayDriver, update, true, -1);
			if (encoder.getEnabledEncoding(RFBConstants.ENC_EXTENDED_FB_SIZE) != null) {
				encoder.resizeWindow(displayDriver, displayDriver.getExtendedScreenData(), false);
			}
			/* Ignore the request because full update has already been sent */
			return null;
		} else {
			/* Ignore this request entirely if continuous updates are active */
			if (continuousUpdates)
				return null;
		}
		return update;
	}

	public void moved(int x, int y) {
		Point p = new Point(x, y);
		if (ready && isPointInDisplay(p)) {
			// synchronized (encoder.getLock()) {
			boolean useSoftCursor = isUseSoftCursor();
			Rectangle newBounds = getCursorBounds(p, currentCursor);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Pointer move bounds " + newBounds);
			}
			if (useSoftCursor) {
				if (currentCursor != null && cursorPosition != null) {
					Rectangle currentCursorBounds = getCursorBounds(cursorPosition, currentCursor);
					encoder.frameUpdate(actualDisplayDriver, currentCursorBounds, -1);
				}
			} else {
				encoder.pointerPositionUpdate(displayDriver, x, y);
			}
			cursorPosition = p;
			if (useSoftCursor && currentCursor != null) {
				encoder.frameUpdate(displayDriver, newBounds, -1);
			}
			// sendQueuedReplies();
			// }
		} else {
		}
	}

	public void resized(ScreenData newBounds, boolean clientInitiated) {
		if (ready) {
			encoder.resizeWindow(displayDriver, newBounds, clientInitiated);
		}
	}

	public void update(UpdateRectangle<?> update) {
		encoder.queueUpdate(update);
	}

	public void damage(String name, Rectangle rectangle, int preferredEncoding) {
		if (ready) {
			encoder.frameUpdate(displayDriver, rectangle, preferredEncoding);
			// try {
			// sendQueuedFrames();
			// } catch (IOException e) {
			// throw new RuntimeException(e);
			// }
		}
	}

	public void pointerChange() {
		pointerChange(currentCursor);
	}

	public void clipboardChanged(FlavorEvent e) {
		// synchronized (encoder.getLock()) {
		encoder.clipboardChanged(displayDriver, e);
		// sendQueuedReplies();
		// }
	}

	public void beep() {
		// synchronized (encoder.getLock()) {
		encoder.beep(displayDriver);
		// sendQueuedReplies();
		// }
	}

	public void pointerChange(PointerShape change) {
		Point p = new Point(change.getX(), change.getY());
		if (ready && isPointInDisplay(p)) {
			// synchronized (encoder.getLock()) {
			boolean useSoftCursor = isUseSoftCursor();
			if (useSoftCursor) {
				if (currentCursor != null && cursorPosition != null) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Queueing soft cursor shape change at " + cursorPosition);
					}
					encoder.frameUpdate(actualDisplayDriver, getCursorBounds(cursorPosition, currentCursor), RFBConstants.ENC_RAW);
				}
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.info("Queueing soft cursor shape change at " + cursorPosition);
				}
				encoder.pointerShapeUpdate(displayDriver, change);
			}
			// if (useSoftCursor) {
			// encoder.frameUpdate(displayDriver,
			// getCursorBounds(cursorPosition, currentCursor), true,
			// RFBConstants.ENC_RAW);
			// try {
			// sendQueuedFrames();
			// } catch (IOException e) {
			// throw new RuntimeException(e);
			// }
			// } else {
			// sendQueuedReplies();
			// }
			// }
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Pointer is not in display or not done first yet");
			}
			currentCursor = change;
			cursorPosition = p;
		}
	}

	public RFBServerFS getFs() {
		return serverFileSystem == null ? server.getServerFileSystem() : serverFileSystem;
	}

	private boolean isPointInDisplay(Point p) {
		return new Rectangle(0, 0, displayDriver.getWidth(), displayDriver.getHeight()).contains(p);
	}

	private Rectangle getCursorBounds(Point cursorPosition, PointerShape currentCursor) {
		Rectangle cursorBounds = new Rectangle(cursorPosition.x - currentCursor.getHotX(),
				cursorPosition.y - currentCursor.getHotY(), currentCursor.getWidth(), currentCursor.getHeight());
		return cursorBounds;
	}

	class SoftPointerDisplayDriver extends FilteredDisplayDriver {
		public SoftPointerDisplayDriver(DisplayDriver underlyingDriver) {
			super(underlyingDriver, false);
			try {
				init();
			} catch (Exception e) {
			}
		}

		@Override
		public BufferedImage grabArea(Rectangle area) {
			BufferedImage img = super.grabArea(area);
			if (isUseSoftCursor() && currentCursor != null && cursorPosition != null) {
				Rectangle cursorBounds = getCursorBounds(cursorPosition, currentCursor);
				System.out.println("SOFT POINT: " + cursorBounds + " / " + area);
				if (cursorBounds.intersects(area)) {
					img.getGraphics().drawImage(currentCursor.getData(), cursorBounds.x - area.x, cursorBounds.y - area.y, null);
				}
			}
			return img;
		}
	}

	public void created(String name, Rectangle bounds) {
		LOG.info("Window created '" + name + "' (" + bounds + ")");
	}

	public void moved(String name, Rectangle bounds, Rectangle oldBounds) {
		if (ready) {
			LOG.info("Window moved '" + name + "' (" + oldBounds + ", " + bounds + ")");
			// Either new or old bounds may be null if the window has moved out
			// of the viewport
			if (oldBounds != null) {
				oldBounds = new Rectangle(oldBounds);
				// Take into account window borders - should be function of
				// driver
				// oldBounds.x -= 30;
				// oldBounds.width += 60;
				// oldBounds.y -= 50;
				// oldBounds.height += 70;
				encoder.frameUpdate(displayDriver, oldBounds, -1);
			}
			if (bounds != null) {
				encoder.frameUpdate(displayDriver, bounds, -1);
			}
		}
	}

	public void resized(String name, Rectangle bounds, Rectangle oldBounds) {
		if (ready) {
			LOG.info("Window resized '" + name + "' (" + oldBounds + ", " + bounds + ")");
			// Either new or old bounds may be null if the window has moved out
			// of the viewport
			if (oldBounds != null) {
				oldBounds = new Rectangle(oldBounds);
				// oldBounds.x -= 30;
				// oldBounds.width += 60;
				// oldBounds.y -= 50;
				// oldBounds.height += 70;
				encoder.frameUpdate(displayDriver, oldBounds, -1);
			}
			if (bounds != null) {
				encoder.frameUpdate(displayDriver, bounds, -1);
			}
		}
	}

	public void closed(String name, Rectangle bounds) {
		LOG.info("Window closed '" + name + "' (" + bounds + ")");
	}

	public ProtocolWriter getOutput() {
		return dout;
	}

	public ProtocolReader getInput() {
		return din;
	}

	public void setOutput(ProtocolWriter dout) {
		this.dout = dout;
	}

	public void setInput(ProtocolReader din) {
		this.din = din;
	}

	public DisplayDriver getDisplayDriver() {
		return displayDriver;
	}

	public void setContinuousUpdates(boolean continuousUpdates) {
		this.continuousUpdates = continuousUpdates;
		if (!continuousUpdates) {
			encoder.queueUpdate(new EndContinuousUpdates());
		}
	}

	public void setRequestedArea(Rectangle requestedArea) {
		this.requestedArea = requestedArea;
	}

	public Rectangle getRequestedArea() {
		return requestedArea;
	}

	public boolean isContinuousUpdates() {
		return continuousUpdates;
	}
}
