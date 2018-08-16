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
/*
 */
package com.sshtools.rfb;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfb.RFBToolkit.RFBImage;
import com.sshtools.rfb.encoding.ExtendedDesktopSizeEncoding;
import com.sshtools.rfb.files.TightVNCFS;
import com.sshtools.rfb.files.UltraVNCFS;
import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.RFBVersion;
import com.sshtools.rfbcommon.ScreenData;
import com.sshtools.rfbcommon.ScreenDetail;
import com.sshtools.rfbcommon.ScreenDimension;

public class ProtocolEngine implements Runnable {
	final static Logger LOG = LoggerFactory.getLogger(ProtocolEngine.class);
	protected static final int VNCR_FRAMEBUFFER_UPDATE = 1;
	final static int BUFFER_SIZE = 65536;
	private RFBImage emptyCursor, dotCursor;
	private RFBImage localCursorImage;
	private char[] initialPassword;
	private RFBDisplay<?, ?> display;
	private RFBEncoding currentEncoding;
	private RFBTransport transport;
	private RFBEventHandler prompt;
	private DataOutputStream recordingOutputStream;
	private RFBVersion version = new RFBVersion();
	private boolean isProcessingEvents = false;
	private boolean isClosed;
	private boolean isDisconnecting = false;
	private RFBContext context;
	private RFBDisplayModel displayModel;
	private byte[] eventBuffer = new byte[72];
	private int eventBufferPos;
	private int pointerMask = 0;
	private int oldModifiers = 0;
	private boolean inputEnabled = true;
	private int currentLocalCursorHotspotX;
	private int currentLocalCursorHotspotY;
	private MonitorDataInputStream monitor;
	private RFBImage stopCursor;
	private int cursorX, cursorY;
	private boolean requestFullUpdate;
	private RFBFS fileSystem;
	private ProtocolReader in;
	private ProtocolWriter out;
	// private int[] supportedSecurityTypes;
	// private int selectedTunnelType;
	private SecurityType securityType;
	private List<SecurityType> securityTypes = new ArrayList<SecurityType>();
	private RFBVersion clientProtocolVersion = new RFBVersion(System.getProperty("rfb.version", RFBDisplay.VERSION_STRING));
	private SecurityTypeFactory securityTypeFactory;
	private boolean continuousUpdatesSupported;
	private boolean useExtendedDesktopSize;

	public ProtocolEngine(RFBDisplay<?, ?> display, RFBTransport transport, RFBContext context, RFBEventHandler prompt,
			RFBDisplayModel displayModel, RFBImage emptyCursor, RFBImage dotCursor) {
		this.context = context;
		this.transport = transport;
		this.prompt = prompt;
		this.display = display;
		this.displayModel = displayModel;
		this.emptyCursor = emptyCursor;
		this.dotCursor = dotCursor;
		securityTypeFactory = new DefaultSecurityTypeFactory();
	}

	public RFBImage getDotCursor() {
		return dotCursor;
	}

	public int getPointerMask() {
		return pointerMask;
	}

	public void setPointerMask(int pointerMask) {
		this.pointerMask = pointerMask;
	}

	public int getCurrentLocalCursorHotspotX() {
		return currentLocalCursorHotspotX;
	}

	public int getCurrentLocalCursorHotspotY() {
		return currentLocalCursorHotspotY;
	}

	public SecurityTypeFactory getSecurityTypeFactory() {
		return securityTypeFactory;
	}

	public void setSecurityTypeFactory(SecurityTypeFactory securityTypeFactory) {
		this.securityTypeFactory = securityTypeFactory;
	}

	public RFBEventHandler getPrompt() {
		return prompt;
	}

	public RFBFS getFileSystem() {
		return fileSystem;
	}

	public RFBDisplay<?, ?> getDisplay() {
		return display;
	}

	public RFBContext getContext() {
		return context;
	}

	/**
	 * Determine the servers version.
	 * 
	 * @throws IOException
	 */
	void determineVersion() throws IOException {
		byte[] buffer = new byte[12];
		in.readFully(buffer);
		version.determineVersion(buffer);
		LOG.info("Server RFB Version: " + version);
	}

	/**
	 * Set the maximum client protocol version to use.
	 * 
	 * @param major major version
	 * @param minor minor version
	 */
	public void setClientProtocolVersion(int major, int minor) {
		clientProtocolVersion.set(major, minor);
	}

	/**
	 * Get the all of the security types used for authentication. The instances
	 * of these may be cast to get authentication type specific details, such as
	 * supported encodings from Tight authentication
	 * 
	 * @return primary security type
	 */
	public List<SecurityType> getSecurityTypes() {
		return securityTypes;
	}

	/**
	 * Send our version
	 * 
	 * @throws IOException
	 */
	RFBVersion sendVersion() throws IOException {
		int c = clientProtocolVersion.compareTo(version);
		RFBVersion sendVersion = version;
		if (c < 0) {
			sendVersion = clientProtocolVersion;
		}
		LOG.info("Sending client version " + sendVersion.formatVersion());
		out.write(sendVersion.formatVersion());
		return sendVersion;
	}

	/**
	 * Determine the authentication scheme
	 * 
	 * @return
	 * @throws java.lang.Exception
	 */
	SecurityType determineAuthenticationRequired() throws IOException {
		int securityTypeCode = in.readInt();
		switch (securityTypeCode) {
		case RFBConstants.SCHEME_NO_AUTHENTICATION:
		case RFBConstants.SCHEME_VNC_AUTHENTICATION:
			if (!securityTypeFactory.isAvailable(securityTypeCode)) {
				throw new IOException("Selected authentication scheme (" + securityTypeCode + ") is not available.");
			}
			break;
		case RFBConstants.SCHEME_CONNECT_FAILED:
			int len = in.readInt();
			byte[] description = new byte[len];
			in.readFully(description);
			throw new IOException(new String(description));
		default:
			throw new IOException(
					"The server reported an invalid authentication scheme! " + "scheme=" + String.valueOf(securityTypeCode));
		}
		return securityTypeFactory.getSecurityType(securityTypeCode);
	}

	/**
	 * Perform client initialization
	 * 
	 * @throws IOException
	 */
	void performClientInitialization() throws IOException {
		LOG.info("Initializing client (share desktop = " + context.isShareDesktop() + ")");
		out.write(context.isShareDesktop() ? 1 : 0);
	}

	void initializeProtocol() throws IOException {
		LOG.info("Initializing protocol");
		performClientInitialization();
		processServerInitialization();
		setPixelFormat();
		setEncodings(context.getEncodings());
		displayModel.updateBuffer();
		for (RFBFS fs : new RFBFS[] { new TightVNCFS(this), new UltraVNCFS(this) }) {
			if (fs.isActive()) {
				fileSystem = fs;
				LOG.info("Chosen " + fs + " as for file transfer");
				break;
			}
		}
	}

	@SuppressWarnings("resource")
	public void enableContinuousUpdates() throws IOException {
		if (!isContinuousUpdatesSupported())
			throw new UnsupportedOperationException();
		if (context.isContinuousUpdates()) {
			LOG.info("Enabling continuous updates");
			context.setContinuousUpdates(true);
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ProtocolWriter paw = new ProtocolWriter(bout);
			paw.writeByte(RFBConstants.CMSG_ENABLE_CONTINUOUS_UPDATES);
			paw.writeByte(1);
			paw.writeShort(0);
			paw.writeShort(0);
			paw.writeShort(displayModel.getScreenData().getWidth());
			paw.writeShort(displayModel.getScreenData().getHeight());
			synchronized (out) {
				out.write(bout.toByteArray());
			}
		}
	}

	/**
	 * Tell the server about supported encodings.
	 * 
	 * @param encs
	 * @param len
	 * @throws IOException
	 */
	void setEncodings(int[] encs) throws IOException {
		LOG.info("Chosen encodings :-");
		for (int i : encs) {
			LOG.info(String.format("    %s [%d]", context.getEncoding(i).getName(), i));
		}
		byte[] msg = new byte[4 + (4 * encs.length)];
		msg[0] = (byte) RFBConstants.CMSG_SET_ENCODINGS;
		msg[2] = (byte) ((encs.length >> 8) & 0xfF);
		msg[3] = (byte) (encs.length & 0xFF);
		for (int i = 0; i < encs.length; i++) {
			msg[4 + (4 * i)] = (byte) ((encs[i] >> 24) & 0xFF);
			msg[5 + (4 * i)] = (byte) ((encs[i] >> 16) & 0xFF);
			msg[6 + (4 * i)] = (byte) ((encs[i] >> 8) & 0xFF);
			msg[7 + (4 * i)] = (byte) (encs[i] & 0xFF);
		}
		out.write(msg);
	}

	/**
	 * Process the servers initialization message
	 * 
	 * @throws IOException
	 */
	void processServerInitialization() throws IOException {
		displayModel.changeFramebufferSize(ExtendedDesktopSizeEncoding.SERVER_SIDE_CHANGE,
				new ScreenData(new ScreenDimension(in.readUnsignedShort(), in.readUnsignedShort())));
		// Pixel format
		displayModel.setBitsPerPixel(in.readUnsignedByte());
		displayModel.setColorDepth(in.readUnsignedByte());
		displayModel.setBigEndian((in.readUnsignedByte() != 0));
		displayModel.setTrueColor((in.readUnsignedByte() != 0));
		displayModel.setRedMax(in.readUnsignedShort());
		displayModel.setGreenMax(in.readUnsignedShort());
		displayModel.setBlueMax(in.readUnsignedShort());
		displayModel.setRedShift(in.readUnsignedByte());
		displayModel.setGreenShift(in.readUnsignedByte());
		displayModel.setBlueShift(in.readUnsignedByte());
		byte[] padding = new byte[3];
		in.readFully(padding);
		// Desktop name
		int len = in.readInt();
		byte[] tmp = new byte[len];
		in.readFully(tmp);
		displayModel.setRfbName(new String(tmp));
		LOG.info("Server's initial pixel format: " + displayModel);
		isProcessingEvents = true;
		if (displayModel.getRfbName().equalsIgnoreCase("libvncserver") && context.getPreferredEncoding() == RFBConstants.ENC_ZLIB) {
			// TODO is this still required
			System.out.println("WARNING: Enabling LibVNCServer / Zlib workaround, changing preferred encoding to Tight");
			context.setPreferredEncoding(RFBConstants.ENC_TIGHT);
		}
		for (SecurityType t : securityTypes) {
			t.postServerInitialisation(this);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getSecurityType(Class<T> clazz) {
		for (SecurityType t : getSecurityTypes()) {
			if (clazz.isAssignableFrom(t.getClass())) {
				return (T) t;
			}
		}
		return null;
	}

	public void setPixelFormat() throws IOException {
		switch (context.getPixelFormat()) {
		case RFBContext.PIXEL_FORMAT_AUTO:
			// Use server requested format
			break;
		case RFBContext.PIXEL_FORMAT_8_BIT:
			changePixelFormat(8, 8, false, true, 7, 7, 3, 0, 3, 6);
			break;
		case RFBContext.PIXEL_FORMAT_15_BIT:
			changePixelFormat(16, 15, false, true, 31, 31, 31, 10, 5, 0);
			break;
		case RFBContext.PIXEL_FORMAT_16_BIT:
			changePixelFormat(16, 16, false, true, 31, 63, 31, 11, 5, 0);
			break;
		case RFBContext.PIXEL_FORMAT_32_BIT_24_BIT_COLOUR:
			changePixelFormat(32, 24, false, true, 255, 255, 255, 16, 8, 0);
			break;
		case RFBContext.PIXEL_FORMAT_32_BIT:
			changePixelFormat(32, 32, false, true, 255, 255, 255, 16, 8, 0);
			break;
		case RFBContext.PIXEL_FORMAT_8_BIT_INDEXED:
			changePixelFormat(8, 8, false, false, 0, 0, 0, 0, 0, 0);
			break;
		default:
			throw new IllegalArgumentException("Unknown pixel format constant.");
		}
		// Use server requested format
		displayModel.updateBuffer();
	}

	/**
	 * Request a frame buffer update from the server.
	 * 
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @param inc
	 * @throws IOException
	 */
	public void requestFramebufferUpdate(int x, int y, int w, int h, boolean inc) throws IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Requesting frame buffer update for " + x + "," + y + "," + w + "," + h + " = " + inc);
		}
		byte[] msg = new byte[10];
		msg[0] = (byte) RFBConstants.CMSG_REQUEST_FRAMEBUFFER_UPDATE;
		msg[1] = (byte) (inc ? 1 : 0);
		msg[2] = (byte) ((x >> 8) & 0xFF);
		msg[3] = (byte) (x & 0xFF);
		msg[4] = (byte) ((y >> 8) & 0xFF);
		msg[5] = (byte) (y & 0xFF);
		msg[6] = (byte) ((w >> 8) & 0xFF);
		msg[7] = (byte) (w & 0xFF);
		msg[8] = (byte) ((h >> 8) & 0xFF);
		msg[9] = (byte) (h & 0xFF);
		synchronized (out) {
			out.write(msg);
		}
	}

	/**
	 * Get clipboard data from the server
	 * 
	 * @return
	 * @throws IOException
	 */
	String getServerCutText() throws IOException {
		byte[] padding = new byte[3];
		in.readFully(padding);
		int len = in.readInt();
		byte[] tmp = new byte[len];
		in.readFully(tmp);
		return new String(tmp);
	}

	/**
	 * Change the pixel format
	 * 
	 * @param bitsPerPixel
	 * @param depth
	 * @param bigEndian
	 * @param trueColour
	 * @param redMax
	 * @param greenMax
	 * @param blueMax
	 * @param redShift
	 * @param greenShift
	 * @param blueShift
	 * @throws IOException
	 */
	void changePixelFormat(int bitsPerPixel, int colorDepth, boolean isBigEndian, boolean isTrueColor, int redMax, int greenMax,
			int blueMax, int redShift, int greenShift, int blueShift) throws IOException {
		displayModel.setBitsPerPixel(bitsPerPixel);
		displayModel.setColorDepth(colorDepth);
		displayModel.setBigEndian(isBigEndian);
		displayModel.setTrueColor(isTrueColor);
		displayModel.setRedMax(redMax);
		displayModel.setGreenMax(greenMax);
		displayModel.setBlueMax(blueMax);
		displayModel.setRedShift(redShift);
		displayModel.setGreenShift(greenShift);
		displayModel.setBlueShift(blueShift);
		byte[] msg = new byte[20];
		msg[0] = (byte) RFBConstants.CMSG_SET_PIXEL_FORMAT;
		msg[4] = (byte) bitsPerPixel;
		msg[5] = (byte) colorDepth;
		msg[6] = (byte) (isBigEndian ? 1 : 0);
		msg[7] = (byte) (isTrueColor ? 1 : 0);
		msg[8] = (byte) ((redMax >> 8) & 0xfF);
		msg[9] = (byte) (redMax & 0xFF);
		msg[10] = (byte) ((greenMax >> 8) & 0xFF);
		msg[11] = (byte) (greenMax & 0xFF);
		msg[12] = (byte) ((blueMax >> 8) & 0xFF);
		msg[13] = (byte) (blueMax & 0xFF);
		msg[14] = (byte) redShift;
		msg[15] = (byte) greenShift;
		msg[16] = (byte) blueShift;
		LOG.info("Requesting next pixel format: " + displayModel);
		synchronized (out) {
			out.write(msg);
		}
	}

	/**
	 * Get the next buffer update rect
	 */
	BufferUpdate getFramebufferUpdateRect() throws IOException {
		BufferUpdate update = new BufferUpdate(in.readUnsignedShort(), in.readUnsignedShort(), in.readUnsignedShort(),
				in.readUnsignedShort(), in.readInt());
		return update;
	}

	String readString() throws IOException {
		int size = in.read();
		byte[] buf = new byte[size];
		in.readFully(buf);
		return new String(buf);
	}

	private void processProtocol() throws IOException, RFBAuthenticationException {
		try {
			// Handshake versions
			determineVersion();
			version = sendVersion();
			// Authenticate
			authenticate();
			// Negotiate session
			initializeProtocol();
			// Notify display of new state
			this.display.resizeComponent();
			prompt.connected();
			prompt.remoteResize(displayModel.getRfbWidth(), displayModel.getRfbHeight());
			// Start protocol thread
			requestFullUpdate = true;
			new Thread(this).start();
		} catch (IOException ioe) {
			if (!isDisconnecting)
				disconnect();
			throw ioe;
		}
	}

	@Override
	public void run() {
		try {
			while (true) { // rely on the IOException to break out
				if (requestFullUpdate) {
					requestFramebufferUpdate(0, 0, displayModel.getRfbWidth(), displayModel.getRfbHeight(), false);
					requestFullUpdate = false;
				}
				int type = in.readUnsignedByte();
				switch (type) {
				case RFBConstants.SMSG_FRAMEBUFFER_UPDATE:
					recordHeader(VNCR_FRAMEBUFFER_UPDATE);
					int numUpdates;
					// ?
					in.read(); // ?
					numUpdates = in.readUnsignedShort();
					boolean cursorPosReceived = false;
					BufferUpdate rect;
					RFBEncoding encoding;
					for (int i = 0; i < numUpdates; i++) {
						rect = getFramebufferUpdateRect();
						if (LOG.isDebugEnabled()) {
							LOG.debug("Update rectangle " + rect);
						}
						try {
							if (monitor != null) {
								monitor.setMonitoring(true);
							}
							encoding = context.selectEncoding(rect.getEncoding());
							if (encoding != null) {
								if (LOG.isDebugEnabled()) {
									LOG.debug("Encoding: " + encoding);
								}
								if ((currentEncoding == null || currentEncoding != encoding) && !encoding.isPseudoEncoding()) {
									currentEncoding = encoding;
									prompt.encodingChanged(currentEncoding);
								}
								encoding.processEncodedRect(display, rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(),
										rect.getEncoding());
								if (monitor != null) {
									monitor.setMonitoring(false);
								}
								if (rect.getEncoding() == RFBConstants.ENC_POINTER_POS
										|| rect.getEncoding() == RFBConstants.ENC_X11_CURSOR
										|| rect.getEncoding() == RFBConstants.ENC_RICH_CURSOR) {
									cursorPosReceived = true;
									continue;
								}
								if (rect.getEncoding() == RFBConstants.ENC_LAST_RECT
										|| rect.getEncoding() == RFBConstants.ENC_NEW_FB_SIZE
										|| rect.getEncoding() == RFBConstants.ENC_EXTENDED_FB_SIZE)
									break;
							} else {
								LOG.warn(String.format("Unknown encoding %s", rect.getEncoding()));
							}
						} catch (IOException ex) {
							ex.printStackTrace();
							throw ex;
						}
					}
					boolean fullUpdateNeeded = false;
					// if (context.isEightBitColor() !=
					// (displayModel
					// .getBitsPerPixel() == RFBDisplay.COLOR_8BIT))
					// {
					// setPixelFormat();
					// fullUpdateNeeded = true;
					// }
					if (context.getDeferUpdateRequests() > 0 && in.available() == 0 && !cursorPosReceived) {
						synchronized (display) {
							try {
								display.wait(context.getDeferUpdateRequests());
							} catch (InterruptedException e) {
							}
						}
					}
					if (monitor != null && adapt()) {
						fullUpdateNeeded = true;
					}
					if (!context.isContinuousUpdates() || fullUpdateNeeded)
						requestFramebufferUpdate(0, 0, displayModel.getRfbWidth(), displayModel.getRfbHeight(), !fullUpdateNeeded);
					break;
				case RFBConstants.SMSG_SET_COLORMAP:
					readColourMap();
				case RFBConstants.SMSG_BELL:
					RFBToolkit.get().beep();
					break;
				case RFBConstants.SMSG_END_CONTINUOUS_UPDATES:
					if (display.getContext().isContinuousUpdates()) {
						LOG.info("Turning off continuous updates");
						display.getContext().setContinuousUpdates(false);
					}
					break;
				case RFBConstants.SMSG_SERVER_CUT_TEXT:
					String s = getServerCutText();
					RFBToolkit.get().getClipboard().setData(s);
					break;
				default:
					if (fileSystem == null || (fileSystem != null && !fileSystem.handleReply(type))) {
						throw new IOException("Unknown RFB message type " + type);
					}
					break;
				}
			}
		} catch (Exception ioe) {
			if (!(ioe instanceof EOFException)) {
				LOG.error("Error in connection.", ioe);
			}
			if (!isDisconnecting) {
				try {
					transport.close();
				} catch (IOException e) {
				}
				prompt.disconnected();
			}
		}
	}

	private void authenticate() throws IOException, RFBAuthenticationException {
		securityType = null;
		LOG.info("Starting authentication");
		if (version.getMajor() == 3 && version.getMinor() == 3) {
			LOG.info("Using legacy authentication");
			securityType = determineAuthenticationRequired();
		} else {
			LOG.info("Using standard authentication");
			securityType = negotiateType();
		}
		if (securityType == null) {
			throw new RFBAuthenticationException("No matching security type.");
		}
		LOG.info("Initially chosen security type is " + securityType);
		handleAuth(securityType);
	}

	private void readColourMap() throws IOException {
		in.read();
		in.readShort(); // "First colour"?
		int colours = in.readShort();
		displayModel.getColorMap().clear();
		if (displayModel.isTrueColor()) {
			LOG.warn("Color map sent, but not using one");
		}
		for (int i = 0; i < colours; i++) {
			int r = displayModel.getColorDepth() == 16 ? in.readShort() : (in.readShort() >> 8) & 0x000000ff;
			int g = displayModel.getColorDepth() == 16 ? in.readShort() : (in.readShort() >> 8) & 0x000000ff;
			int b = displayModel.getColorDepth() == 16 ? in.readShort() : (in.readShort() >> 8) & 0x000000ff;
			LOG.info(
					String.format("Map %d to %s %s %s", i, Integer.toHexString(r), Integer.toHexString(g), Integer.toHexString(b)));
			displayModel.getColorMap().put(i, r << 16 | g << 8 | b);
		}
	}

	private void handleAuth(SecurityType securityType) throws IOException, RFBAuthenticationException {
		SecurityType type = securityType;
		while (true) {
			securityTypes.add(type);
			LOG.info("Processing authentication using type " + type.toString());
			int result = type.process(this);
			LOG.info("Security type returned " + result);
			if (result == 0) {
				sendAuthenticationError();
				break;
			} else if (result == 1) {
				// TODO does this mean a loop?
				// state_ = RFBSTATE_SECURITY_RESULT;
				if (version.compareTo(RFBVersion.VERSION_3_8) < 0 && type.getType() == RFBConstants.SCHEME_NO_AUTHENTICATION) {
					LOG.info("Legacy authentication, completing OK now.");
					result = RFBConstants.AUTHENTICATION_OK;
				} else {
					result = in.readInt();
					LOG.info("Standard authentication, result is " + result);
				}
				switch (result) {
				case RFBConstants.AUTHENTICATION_OK:
					// All done!
					LOG.info("Authentication complete.");
					break;
				case RFBConstants.AUTHENTICATION_FAILED:
					LOG.info("Authentication failed.");
					sendAuthenticationError();
					break;
				case RFBConstants.AUTHENTICATION_TOO_MANY:
					LOG.info("Too many attempts.");
					sendAuthenticationError();
					break;
				default:
					LOG.info("Unknown result " + result + ".");
					sendAuthenticationError();
					break;
				}
				break;
			} else {
				type = securityTypeFactory.getSecurityType(result - 2);
				LOG.info("Next authentication cycle " + type);
			}
		}
	}

	private void sendAuthenticationError() throws RFBAuthenticationException, IOException {
		String authenticationError = getAuthenticationError();
		LOG.error("Authentication error. " + authenticationError);
		throw new RFBAuthenticationException(authenticationError);
	}
	// private int XXhandleAuth(int authScheme, boolean sendResult)
	// throws IOException, AuthenticationException {
	// try {
	// if (authScheme == RFBConstants.SCHEME_NO_AUTHENTICATION) {
	// if (sendResult) {
	// if (version.compareTo(RFBVersion.VERSION_3_8) >= 0) {
	// String result = getSecurityResult();
	// if (result != null) {
	// throw new AuthenticationException(result);
	// }
	// }
	// }
	// } else if (authScheme == RFBConstants.SCHEME_VNC_AUTHENTICATION) {
	// byte[] challenge = new byte[16];
	// try {
	// in.readFully(challenge);
	// } catch (EOFException eof) {
	// throw new AuthenticationException("Incorrect password.");
	// }
	// String pw = initialPassword == null ? prompt
	// .passwordAuthenticationRequired() : new String(
	// initialPassword);
	// if (initialPassword != null) {
	// initialPassword = null;
	// }
	// if (pw == null) {
	// throw new AuthenticationException(
	// "Authentication cancelled.");
	// }
	// if (pw.length() > 8) {
	// pw = pw.substring(0, 8);
	// }
	// int firstZero = pw.indexOf(0);
	// if (firstZero != -1) {
	// pw = pw.substring(0, firstZero);
	// }
	// byte[] key = new byte[8];
	// System.arraycopy(pw.getBytes(), 0, key, 0, pw.length());
	// AcmeDesCipher des = new AcmeDesCipher(key);
	// // des.encrypt(challenge, challenge);
	// des.encrypt(challenge, 0, challenge, 0);
	// des.encrypt(challenge, 8, challenge, 8);
	// this.out.write(challenge);
	// if (sendResult) {
	// String result = getSecurityResult();
	// if (result != null) {
	// throw new AuthenticationException(result);
	// }
	// }
	// } else if (authScheme == RFBConstants.SCHEME_TIGHT_AUTHENTICATION) {
	// authScheme = negotiateTightAuthentication();
	// handleAuth(authScheme, false);
	// if (sendResult) {
	// String result = getSecurityResult();
	// if (result != null) {
	// throw new AuthenticationException(result);
	// }
	// }
	// } else if (authScheme == RFBConstants.SCHEME_TLS_AUTHENTICATION) {
	// setupTLS();
	// authScheme = negotiateType();
	// } else {
	// throw new IOException(
	// "The RFB protocol returned an unexpecetd authentication scheme! scheme="
	// + String.valueOf(authScheme));
	// }
	// return authScheme;
	// } catch (NoSuchAlgorithmException nsae) {
	// throw new IOException(nsae);
	// } catch (KeyManagementException nsae) {
	// throw new IOException(nsae);
	// }
	// }

	// private String getSecurityResult() throws IOException {
	// int authResult = this.in.readInt();
	// if (authResult == RFBConstants.AUTHENTICATION_OK) {
	// return null;
	// } else if (authResult == RFBConstants.AUTHENTICATION_FAILED) {
	// return getAuthenticationError();
	// } else if (authResult == RFBConstants.AUTHENTICATION_TOO_MANY) {
	// return "Too many authentication attempts";
	// } else {
	// return
	// "The RFB protocol returned an unexpected authentication result! result="
	// + String.valueOf(authResult);
	// }
	// }
	private String getAuthenticationError() throws IOException {
		if (version.compareTo(RFBVersion.VERSION_3_8) >= 0) {
			int len = in.readInt();
			byte[] tmp = new byte[len];
			in.readFully(tmp);
			return new String(tmp);
		}
		return "Authentication failed.";
	}

	// private int negotiateTightAuthentication() throws IOException {
	// int authScheme = RFBConstants.SCHEME_NO_AUTHENTICATION;
	// int numberOfTunnels = (int) in.readInt();
	// if (numberOfTunnels != 0) {
	// selectedTunnelType = -1;
	// for (int i = 0; i < numberOfTunnels; i++) {
	// TightCapability c = new TightCapability(in);
	// selectedTunnelType = c.getCode();
	// }
	// if (selectedTunnelType != 0) {
	// throw new IOException("Unsupported tunnel type");
	// }
	// out.writeInt(selectedTunnelType);
	// }
	// int numberOfAuthTypes = in.readInt();
	// if (numberOfAuthTypes != 0) {
	// List<Integer> supportedServerTypes = new ArrayList<Integer>();
	// for (int i = 0; i < numberOfAuthTypes; i++) {
	// TightCapability c = new TightCapability(in);
	// supportedServerTypes.add(Integer.valueOf(c.getCode()));
	// }
	// authScheme = securityTypeFactory.selectScheme(supportedServerTypes);
	// out.writeInt(authScheme);
	// }
	// return authScheme;
	// }
	public SecurityType negotiateType() throws IOException {
		LOG.info("Negotiating security types");
		// TODO
		// Not sure if this is right
		List<Integer> validSubAuths = securityType == null ? null : securityTypes.get(securityTypes.size() - 1).getSubAuthTypes();
		int newSecurityTypeCode = 0;
		int securityTypes = in.readUnsignedByte();
		if (securityTypes == 0) {
			String reason = readString();
			LOG.error("Failed to connect. " + reason);
			throw new IOException("Failed to connect. " + reason);
		}
		List<Integer> supportedServerTypes = new ArrayList<Integer>();
		for (int i = 0; i < securityTypes; i++) {
			Integer serverType = Integer.valueOf(in.readUnsignedByte());
			if (securityTypeFactory.isAvailable(serverType)
					&& (validSubAuths == null || (validSubAuths != null && validSubAuths.contains(serverType)))) {
				LOG.info("Server and client both support authentication type " + serverType);
				supportedServerTypes.add(serverType);
			} else {
				LOG.info("Server support authentications type " + serverType + " but this client does not");
			}
		}
		newSecurityTypeCode = securityTypeFactory.selectScheme(supportedServerTypes);
		if (newSecurityTypeCode == RFBConstants.SCHEME_CONNECT_FAILED) {
			LOG.info("No security type now selected.");
		} else {
			LOG.info("Selected security type " + newSecurityTypeCode);
			out.write(newSecurityTypeCode);
			out.flush();
		}
		return securityTypeFactory.getSecurityType(newSecurityTypeCode);
	}

	public void sendCtrlAltDel() throws IOException {
		if (isInputEnabled()) {
			postKeyboardEvent(0xFFFF, true, RFBDisplay.CTRL_MASK | RFBDisplay.ALT_MASK);
			postKeyboardEvent(0xFFFF, false, RFBDisplay.CTRL_MASK | RFBDisplay.ALT_MASK);
		}
	}

	public boolean isProcessingEvents() {
		return isProcessingEvents && !isClosed;
	}

	/**
	 * Send the server some clipboard data
	 * 
	 * @param text
	 * @throws IOException
	 */
	public void sendClipboardText(String text) throws IOException {
		byte[] msg = new byte[8 + text.length()];
		msg[0] = (byte) RFBConstants.CMSG_CUT_TEXT;
		msg[4] = (byte) ((text.length() >> 24) & 0xFF);
		msg[5] = (byte) ((text.length() >> 16) & 0xFF);
		msg[6] = (byte) ((text.length() >> 8) & 0xFF);
		msg[7] = (byte) (text.length() & 0xFF);
		System.arraycopy(text.getBytes(), 0, msg, 8, text.length());
		synchronized (out) {
			out.write(msg);
		}
	}

	public boolean isContinuousUpdatesSupported() {
		return continuousUpdatesSupported;
	}

	public void setContinuousUpdatesSupported(boolean continuousUpdatesSupported) {
		this.continuousUpdatesSupported = continuousUpdatesSupported;
	}

	public boolean isUseExtendedDesktopSize() {
		return useExtendedDesktopSize;
	}

	public void setDesktopSize(ScreenData screenData) throws IOException {
		if (!isUseExtendedDesktopSize()) {
			throw new IOException("Extended desktop size is not a supported encoding.");
		}
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		@SuppressWarnings("resource")
		ProtocolWriter paw = new ProtocolWriter(bout);
		paw.writeByte(RFBConstants.CMSG_SET_DESKTOP_SIZE);
		paw.writeByte(0);
		paw.writeByte(0);
		paw.writeShort(screenData.getWidth());
		paw.writeShort(screenData.getHeight());
		List<ScreenDetail> a = screenData.getAllDetails();
		paw.writeByte(a.size());
		paw.writeByte(0);
		LOG.info(String.format("Writing %d screens, primary is %dx%d", a.size(), screenData.getWidth(), screenData.getHeight()));
		int scr = 1;
		for (ScreenDetail d : a) {
			paw.writeUInt32(d.getId());
			paw.writeShort(d.getX());
			paw.writeShort(d.getY());
			paw.writeShort(d.getDimension().getWidth());
			paw.writeShort(d.getDimension().getHeight());
			paw.writeUInt32(d.getFlags());
			LOG.info(String.format("    %2d [%10d] %dx%d@%d,%d (%d)", scr, d.getId(), d.getDimension().getWidth(), d.getDimension().getHeight(), d.getX(), d.getY(), d.getFlags()));
			scr++;
		}
		synchronized (out) {
			out.write(bout.toByteArray());
		}
	}

	public void sendPointerEvent(int modifiers, int x, int y) throws IOException {
		eventBufferPos = 0;
		encodeModifierKeyEvents(modifiers);
		eventBuffer[eventBufferPos++] = (byte) RFBConstants.CMSG_POINTER_EVENT;
		eventBuffer[eventBufferPos++] = (byte) pointerMask;
		eventBuffer[eventBufferPos++] = (byte) ((x >> 8) & 0xFF);
		eventBuffer[eventBufferPos++] = (byte) (x & 0xfF);
		eventBuffer[eventBufferPos++] = (byte) ((y >> 8) & 0xFF);
		eventBuffer[eventBufferPos++] = (byte) (y & 0xFF);
		if (pointerMask == 0) {
			encodeModifierKeyEvents(0);
		}
		synchronized (out) {
			out.write(eventBuffer, 0, eventBufferPos);
			out.flush();
		}
	}

	public synchronized void postKeyboardEvent(int keysym, boolean down, int modifier) throws IOException {
		eventBufferPos = 0;
		encodeModifierKeyEvents(modifier);
		encodeKeyEvent(keysym, down);
		if (!down) {
			encodeModifierKeyEvents(0);
		}
		/* DEBUG */debugByteArray(eventBuffer, 0, eventBufferPos, "Key");
		synchronized (out) {
			out.write(eventBuffer, 0, eventBufferPos);
		}
	}

	private void debugByteArray(byte[] arr, int start, int len, String label) {
		StringBuffer buf = new StringBuffer(label);
		buf.append(": ");
		for (int i = start; i < len; i++) {
			if (i > start) {
				buf.append(",");
			}
			buf.append(arr[i]);
		}
	}

	/**
	 * Encoded a key event into the current event message buffer
	 * 
	 * @param keysym
	 * @param down
	 */
	public synchronized void encodeKeyEvent(int keysym, boolean down) {
		eventBuffer[eventBufferPos++] = (byte) RFBConstants.CMSG_KEYBOARD_EVENT;
		eventBuffer[eventBufferPos++] = (byte) (down ? 1 : 0);
		eventBuffer[eventBufferPos++] = (byte) 0;
		eventBuffer[eventBufferPos++] = (byte) 0;
		eventBuffer[eventBufferPos++] = (byte) ((keysym >> 24) & 0xff);
		eventBuffer[eventBufferPos++] = (byte) ((keysym >> 16) & 0xff);
		eventBuffer[eventBufferPos++] = (byte) ((keysym >> 8) & 0xff);
		eventBuffer[eventBufferPos++] = (byte) (keysym & 0xff);
	}

	/**
	 * Encode key modifiers into the event message buffer
	 * 
	 * @param newModifiers
	 */
	public void encodeModifierKeyEvents(int newModifiers) {
		if ((newModifiers & RFBDisplay.CTRL_MASK) != (oldModifiers & RFBDisplay.CTRL_MASK)) {
			encodeKeyEvent(0xffe3, (newModifiers & RFBDisplay.CTRL_MASK) != 0);
		}
		if ((newModifiers & RFBDisplay.SHIFT_MASK) != (oldModifiers & RFBDisplay.SHIFT_MASK)) {
			encodeKeyEvent(0xffe1, (newModifiers & RFBDisplay.SHIFT_MASK) != 0);
		}
		if ((newModifiers & RFBDisplay.META_MASK) != (oldModifiers & RFBDisplay.META_MASK)) {
			encodeKeyEvent(0xffe7, (newModifiers & RFBDisplay.META_MASK) != 0);
		}
		if ((newModifiers & RFBDisplay.ALT_MASK) != (oldModifiers & RFBDisplay.ALT_MASK)) {
			encodeKeyEvent(0xffe9, (newModifiers & RFBDisplay.ALT_MASK) != 0);
		}
		oldModifiers = newModifiers;
	}

	public void updateCursor(int realx, int realy) {
		cursorX = realx;
		cursorY = realy;
		if (!inputEnabled) {
			if (stopCursor != null) {
				setLocalCursor(stopCursor, 16, 16);
			} else {
				display.setCursor(RFBToolkit.get().getDefaultCursor());
			}
		} else {
			if (!context.isCursorUpdatesRequested() || context.isCursorUpdateIgnored()) {
				// Let the server draw the cursor
				setLocalCursor(emptyCursor, 16, 16);
			} else {
				if (context.isLocalCursorDisplayed()) {
					if (displayModel.getCursor() != null) {
						setLocalCursor(displayModel.getCursor(), displayModel.getHotX(), displayModel.getHotY());
					} else {
						setLocalCursor(null, -1, -1);
					}
				} else {
					if (displayModel.getCursor() != null) {
						setLocalCursor(emptyCursor, 0, 0);
						displayModel.softCursorMove(realx, realy);
					} else {
						setLocalCursor(dotCursor, 16, 16);
					}
				}
			}
		}
	}

	public void refresh() throws IOException {
		requestFramebufferUpdate(0, 0, displayModel.getRfbWidth(), displayModel.getRfbHeight(), false);
	}

	/**
	 * @return Returns the inputEnabled.
	 */
	public boolean isInputEnabled() {
		return inputEnabled;
	}

	/**
	 * @param inputEnabled The inputEnabled to set.
	 */
	public void setInputEnabled(boolean inputEnabled) {
		if (inputEnabled != this.inputEnabled) {
			this.inputEnabled = inputEnabled;
			updateCursor(cursorX, cursorY);
		}
	}

	public RFBDisplayModel getDisplayModel() {
		return displayModel;
	}

	/**
	 * @param b
	 */
	public boolean isDisconnecting() {
		return isDisconnecting;
	}

	/**
	 * @param b
	 */
	public boolean isClosed() {
		return isClosed;
	}

	/**
	 * @return
	 */
	public RFBTransport getTransport() {
		return transport;
	}

	public void disconnect() {
		if (!isClosed() && !isDisconnecting()) {
			isDisconnecting = true;
			setLocalCursor(null, -1, -1);
			try {
				if (transport != null) {
					transport.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				isClosed = true;
				prompt.disconnected();
				isDisconnecting = false;
			}
		}
	}

	public void startRFBProtocol() throws IOException, RFBAuthenticationException {
		if (context.isAdaptive()) {
			setInputStream(new ProtocolReader(monitor = new MonitorDataInputStream(transport.getInputStream())));
		} else {
			monitor = null;
			setInputStream(new ProtocolReader(transport.getInputStream()));
		}
		setOutputStream(transport.getOutputStream());
		setInputEnabled(!context.isViewOnly());
		if (!context.isLocalCursorDisplayed()) {
			setLocalCursor(emptyCursor, 0, 0);
		}
		processProtocol();
	}

	public boolean isConnected() {
		return transport == null ? false : !isClosed;
	}

	public void setLocalCursor(final RFBImage img, final int hotx, final int hoty) {
		java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<Void>() {
			@Override
			public Void run() {
				setLocalCursorImpl(img, hotx, hoty);
				return null;
			}
		});
	}

	/**
	 * @return Returns the inputStream.
	 */
	public ProtocolReader getInputStream() {
		return in;
	}

	/**
	 * @param in The inputStream to set.
	 */
	public void setInputStream(ProtocolReader in) {
		this.in = in;
	}

	/**
	 * @param out The outputStream to set.
	 */
	public void setOutputStream(OutputStream out) {
		this.out = out instanceof ProtocolWriter ? (ProtocolWriter) out : new ProtocolWriter(out);
	}

	private void setLocalCursorImpl(RFBImage img, int hotX, int hotY) {
		if ((localCursorImage == null && img != null) || (img == null && localCursorImage != null) || (img != localCursorImage)) {
			localCursorImage = img;
			try {
				if (localCursorImage != null) {
					setCursorImage(localCursorImage, hotX, hotY);
					currentLocalCursorHotspotX = hotX;
					currentLocalCursorHotspotY = hotY;
				} else {
					display.setCursor(RFBToolkit.get().getDefaultCursor());
					currentLocalCursorHotspotX = 0;
					currentLocalCursorHotspotY = 0;
				}
			} catch (Throwable t) {
				// Not supported for some reason
				display.setCursor(RFBToolkit.get().getDefaultCursor());
				currentLocalCursorHotspotX = 0;
				currentLocalCursorHotspotY = 0;
			}
		}
	}

	private void setCursorImage(RFBImage img, int hotX, int hotY) throws Exception {
		display.setCursor(RFBToolkit.get().createCursor(img, hotX, hotY));
	}

	private boolean adapt() {
		boolean fullUpdate = false;
		long speed = monitor.getSpeed();
		RFBEncoding newEncoding = currentEncoding;
		if (speed > 3128) {
			newEncoding = context.getEncoding(RFBConstants.ENC_HEXTILE);
		} else if (speed < 1496) {
			newEncoding = context.getEncoding(RFBConstants.ENC_TIGHT);
		}
		if (context.getPreferredEncoding() != newEncoding.getType()) {
			context.setPreferredEncoding(newEncoding.getType());
			try {
				setEncodings(context.getEncodings());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if (speed > 4098 && !displayModel.isTrueColor()) {
				displayModel.setTrueColor(true);
				try {
					setPixelFormat();
					fullUpdate = true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (speed < 1024) {
				if (displayModel.isTrueColor()) {
					displayModel.setTrueColor(false);
					try {
						setPixelFormat();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return fullUpdate;
	}

	public char[] getInitialPassword() {
		return initialPassword;
	}

	public void setInitialPassword(char[] initialPassword) {
		this.initialPassword = initialPassword;
	}

	/**
	 * @return
	 */
	public RFBEncoding getCurrentEncoding() {
		return currentEncoding;
	}

	public void setStopCursor(RFBImage stopCursor) {
		this.stopCursor = stopCursor;
	}

	public synchronized void startRecording(OutputStream stream) throws IOException {
		if (recordingOutputStream != null) {
			throw new IOException("Already recording.");
		}
		DataOutputStream ros = new DataOutputStream(stream);
		ros.writeInt(displayModel.getRfbWidth());
		ros.writeInt(displayModel.getRfbHeight());
		ros.writeByte(displayModel.getBytesPerPixel());
		requestFullUpdate = true;
		recordingOutputStream = ros;
	}

	void recordHeader(int id) throws IOException {
		if (recordingOutputStream != null) {
			recordingOutputStream.writeLong(System.currentTimeMillis());
			recordingOutputStream.writeInt(id);
		}
	}

	public ProtocolWriter getOutputStream() {
		return out;
	}

	public void setUseExtendedDesktopSize(boolean useExtendedDesktopSize) {
		this.useExtendedDesktopSize = useExtendedDesktopSize;
	}
}