/*
 */
package com.sshtools.rfb;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.profile.AuthenticationException;
import com.sshtools.rfb.files.TightVNCFS;
import com.sshtools.rfb.files.UltraVNCFS;
import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.RFBVersion;

/**
 * This class provides the main thread for the protocol.
 * 
 * @author Lee David Painter
 */
public class ProtocolEngine implements KeyListener, MouseListener, MouseMotionListener, ClipboardOwner, Runnable {
    final static Logger LOG = LoggerFactory.getLogger(ProtocolEngine.class);
    protected static final int VNCR_FRAMEBUFFER_UPDATE = 1;

    final static int BUFFER_SIZE = 65536;

    private Image emptyCursor, dotCursor;
    private Image localCursorImage;
    private char[] initialPassword;
    private MouseEventDispatcher mouseEventDispatcher;
    private RFBDisplay display;
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
    private Point currentLocalCursorHotspot;
    private MonitorDataInputStream monitor;
    private Image stopCursor;
    private int cursorX, cursorY;
    private boolean requestedFullUpdate;
    private RFBFS fileSystem;
    private ProtocolReader in;
    private ProtocolWriter out;
    // private int[] supportedSecurityTypes;
    // private int selectedTunnelType;
    private SecurityType securityType;
    private List<SecurityType> securityTypes = new ArrayList<SecurityType>();
    private RFBVersion clientProtocolVersion = new RFBVersion(System.getProperty("rfb.version", RFBDisplay.VERSION_STRING));
    private SecurityTypeFactory securityTypeFactory;

    public ProtocolEngine(RFBDisplay display, RFBTransport transport, RFBContext context, RFBEventHandler prompt,
                          RFBDisplayModel displayModel, Image emptyCursor, Image dotCursor) {
        this.context = context;
        this.transport = transport;
        this.prompt = prompt;
        this.display = display;
        this.displayModel = displayModel;
        this.emptyCursor = emptyCursor;
        this.dotCursor = dotCursor;

        securityTypeFactory = new DefaultSecurityTypeFactory();
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

    public RFBDisplay getDisplay() {
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
                throw new IOException("The server reported an invalid authentication scheme! " + "scheme="
                                + String.valueOf(securityTypeCode));
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

    /**
     * Tell the server about supported encodings.
     * 
     * @param encs
     * @param len
     * @throws IOException
     */
    void setEncodings(int[] encs) throws IOException {
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
        displayModel.setRfbWidth(in.readUnsignedShort());
        displayModel.setRfbHeight(in.readUnsignedShort());
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
        if (displayModel.getRfbName().equalsIgnoreCase("libvncserver")
                        && context.getPreferredEncoding() == RFBContext.ENCODING_ZLIB) {
            System.out.println("WARNING: Enabling LibVNCServer / Zlib workaround, changing preferred encoding to Tight");
            context.setPreferredEncoding(RFBContext.ENCODING_TIGHT);
        }

        for(SecurityType t : securityTypes) {
            t.postServerInitialisation(this);
        }
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

    private void processProtocol() throws IOException, AuthenticationException {
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
            prompt.resized(displayModel.getRfbWidth(), displayModel.getRfbHeight());

            // Start protocol thread
            requestedFullUpdate = true;
            new Thread(this).start();
        } catch (IOException ioe) {
            if (!isDisconnecting)
                disconnect();
            throw ioe;
        }
    }

    public void run() {
        try {
            while (true) { // rely on the IOException to break out
                if (requestedFullUpdate) {
                    requestFramebufferUpdate(0, 0, displayModel.getRfbWidth(), displayModel.getRfbHeight(), false);
                    requestedFullUpdate = false;
                }
                int type = in.readUnsignedByte();
                switch (type) {
                    case RFBConstants.SMSG_FRAMEBUFFER_UPDATE:
                        recordHeader(VNCR_FRAMEBUFFER_UPDATE);
                        int numUpdates;
                        // ?
                        int foo = in.read();
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
                                    encoding.processEncodedRect(display, rect.getX(), rect.getY(), rect.getWidth(),
                                        rect.getHeight(), rect.getEncoding());
                                    if (monitor != null) {
                                        monitor.setMonitoring(false);
                                    }
                                    if (rect.getEncoding() == RFBContext.MASK_ENCODING_POINTERPOS
                                                    || rect.getEncoding() == RFBContext.MASK_ENCODING_XCURSOR
                                                    || rect.getEncoding() == RFBContext.MASK_ENCODING_RICHCURSOR) {
                                        cursorPosReceived = true;
                                        continue;
                                    }
                                    if (rect.getEncoding() == RFBContext.MASK_ENCODING_LAST_RECT
                                                    || rect.getEncoding() == RFBContext.MASK_ENCODING_NEW_SIZE)
                                        break;
                                } else {
                                    System.out.println("WARNING: Unknown encoding " + rect.getEncoding());
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
                        requestFramebufferUpdate(0, 0, displayModel.getRfbWidth(), displayModel.getRfbHeight(), !fullUpdateNeeded);
                        break;
                    case RFBConstants.SMSG_SET_COLORMAP:
                        readColourMap();
                    case RFBConstants.SMSG_BELL:
                        Toolkit.getDefaultToolkit().beep();
                        break;
                    case RFBConstants.SMSG_SERVER_CUT_TEXT:
                        String s = getServerCutText();
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s), ProtocolEngine.this);
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

    private void authenticate() throws IOException, AuthenticationException {
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
            throw new AuthenticationException("No matching security type.");
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
            LOG.info(String.format("Map %d to %s %s %s", i, Integer.toHexString(r), Integer.toHexString(g), Integer.toHexString(b)));
            displayModel.getColorMap().put(i, r << 16 | g << 8 | b);
        }
    }

    private void handleAuth(SecurityType securityType) throws IOException, AuthenticationException {
        SecurityType type = securityType;
        List<Integer> validSubAuths = null;
        while (true) {
            validSubAuths = type.getSubAuthTypes();
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

    private void sendAuthenticationError() throws AuthenticationException, IOException {
        String authenticationError = getAuthenticationError();
        LOG.error("Authentication error. " + authenticationError);
        throw new AuthenticationException(authenticationError);
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
            if (securityTypeFactory.isAvailable(serverType) && ( validSubAuths == null || (validSubAuths != null && validSubAuths.contains(serverType)))) {
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

    // private boolean isSupportedClientSecurityType(int type) {
    // return type == RFBConstants.SCHEME_VNC_AUTHENTICATION
    // || type == RFBConstants.SCHEME_TIGHT_AUTHENTICATION;
    // }

    public void lostOwnership(Clipboard owner, Transferable t) {
    }

    public void sendCtrlAltDel() throws IOException {
        if (isInputEnabled()) {
            final int modifiers = RFBDisplay.CTRL_MASK | RFBDisplay.ALT_MASK;
            KeyEvent ctrlAltDelEvent = new KeyEvent(display.getDisplayComponent(), KeyEvent.KEY_PRESSED, 0, modifiers,
                            KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED);
            postKeyboardEvent(ctrlAltDelEvent);
            ctrlAltDelEvent = new KeyEvent(display.getDisplayComponent(), KeyEvent.KEY_RELEASED, 0, modifiers, KeyEvent.VK_DELETE,
                            KeyEvent.CHAR_UNDEFINED);
            postKeyboardEvent(ctrlAltDelEvent);
        }
    }

    /**
     * @return
     */
    public boolean isProcessingEvents() {
        return isProcessingEvents;
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

    /**
     * Record a pointer event ready for sending
     * 
     * @param evt
     * @throws IOException
     */
    public synchronized void postPointerEvent(MouseEvent evt) throws IOException {
        int modifiers = evt.getModifiers();
        int mask2 = 2;
        int mask3 = 4;
        if (context.isReverseMouseButtons2And3()) {
            mask2 = 4;
            mask3 = 2;
        }
        int mask4 = 8;
        int mask5 = 16;

        if (evt.getID() == MouseEvent.MOUSE_WHEEL) {
            // #ifdef JAVA2
            java.awt.event.MouseWheelEvent we = (java.awt.event.MouseWheelEvent) evt;
            if (we.getWheelRotation() < 0) {
                pointerMask = mask4;
            } else {
                pointerMask = mask5;
            }
            // #endif
        } else if (evt.getID() == MouseEvent.MOUSE_PRESSED) {
            if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
                pointerMask = mask2;
                modifiers &= ~RFBDisplay.ALT_MASK;
            } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
                pointerMask = mask3;
                modifiers &= ~RFBDisplay.META_MASK;
            } else {
                pointerMask = 1;
            }
        } else if (evt.getID() == MouseEvent.MOUSE_RELEASED) {
            pointerMask = 0;
            if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
                modifiers &= ~RFBDisplay.ALT_MASK;
            } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
                modifiers &= ~RFBDisplay.META_MASK;
            }
        }
        int x = (int) (evt.getX() / displayModel.getXscale()) - displayModel.getImagex();
        int y = (int) (evt.getY() / displayModel.getYscale()) - displayModel.getImagey();
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        if (y >= displayModel.getRfbHeight()) {
            y = displayModel.getRfbHeight() - 1;
        }
        if (x >= displayModel.getRfbWidth()) {
            y = displayModel.getRfbWidth() - 1;
        }
        sendPointerEvent(modifiers, x, y);

        // Button up must be sent on mouse wheel
        if (evt.getID() == MouseEvent.MOUSE_WHEEL) {
            pointerMask &= ~mask4;
            pointerMask &= ~mask5;
            sendPointerEvent(modifiers, x, y);
        }

        // A bug? Without this my server (TightVNC 1.2.9 on Linux) doesnt seem
        // to send a cursor update back upon click
        // if (context.isCursorUpdatesRequested()) {
        // requestFramebufferUpdate(x, y, 1, 1, true);
        // }
    }

    private void sendPointerEvent(int modifiers, int x, int y) throws IOException {
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

    /**
     * Record a keyboard event ready for sending
     * 
     * @param evt
     * @throws IOException
     */
    public synchronized void postKeyboardEvent(KeyEvent evt) throws IOException {
        int keyChar = evt.getKeyChar();
        if (keyChar == 0) {
            keyChar = KeyEvent.CHAR_UNDEFINED;
        }
        int key = 0;
        if (keyChar == KeyEvent.CHAR_UNDEFINED) {
            keyChar = evt.getKeyCode();
            switch (keyChar) {
                case KeyEvent.VK_CONTROL:
                    key = evt.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT ? 0xffe4 : 0xffe3;
                    break;
                case KeyEvent.VK_SHIFT:
                    key = evt.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT ? 0xffe2 : 0xffe1;
                    break;
                case KeyEvent.VK_ALT:
                    key = evt.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT ? 0xffea : 0xffe9;
                    break;
                case KeyEvent.VK_META:
                    key = evt.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT ? 0xffe8 : 0xffe7;
                    break;
            }
        }
        boolean down = (evt.getID() == KeyEvent.KEY_PRESSED);
        if (key == 0) {
            if (evt.isActionKey()) {
                switch (evt.getKeyCode()) {
                    case KeyEvent.VK_HOME:
                        key = 0xFF50;
                        break;
                    case KeyEvent.VK_LEFT:
                        key = 0xFF51;
                        break;
                    case KeyEvent.VK_UP:
                        key = 0xFF52;
                        break;
                    case KeyEvent.VK_RIGHT:
                        key = 0xFF53;
                        break;
                    case KeyEvent.VK_DOWN:
                        key = 0xFF54;
                        break;
                    case KeyEvent.VK_PAGE_UP:
                        key = 0xFF55;
                        break;
                    case KeyEvent.VK_PAGE_DOWN:
                        key = 0xFF56;
                        break;
                    case KeyEvent.VK_END:
                        key = 0xFF57;
                        break;
                    case KeyEvent.VK_INSERT:
                        key = 0xFF63;
                        break;
                    case KeyEvent.VK_F1:
                        key = 0xFFBE;
                        break;
                    case KeyEvent.VK_F2:
                        key = 0xFFBF;
                        break;
                    case KeyEvent.VK_F3:
                        key = 0xFFC0;
                        break;
                    case KeyEvent.VK_F4:
                        key = 0xFFC1;
                        break;
                    case KeyEvent.VK_F5:
                        key = 0xFFC2;
                        break;
                    case KeyEvent.VK_F6:
                        key = 0xFFC3;
                        break;
                    case KeyEvent.VK_F7:
                        key = 0xFFC4;
                        break;
                    case KeyEvent.VK_F8:
                        key = 0xFFC5;
                        break;
                    case KeyEvent.VK_F9:
                        key = 0xFFC6;
                        break;
                    case KeyEvent.VK_F10:
                        key = 0xFFC7;
                        break;
                    case KeyEvent.VK_F11:
                        key = 0xFFC8;
                        break;
                    case KeyEvent.VK_F12:
                        key = 0xFFC9;
                        break;
                    default:
                        return;
                }
            } else {
                key = keyChar;
                if (key == KeyEvent.VK_DELETE) {
                    key = 0xFFFF;
                } else {
                    if (key < 0x20) {
                        if (evt.isControlDown()) {
                            key += 0x60;
                        } else {
                            switch (key) {
                                case KeyEvent.VK_BACK_SPACE:
                                    key = 0xFF08;
                                    break;
                                case KeyEvent.VK_TAB:
                                    key = 0xFF09;
                                    break;
                                case KeyEvent.VK_ENTER:
                                    key = 0xFF0D;
                                    break;
                                case KeyEvent.VK_ESCAPE:
                                    key = 0xFF1B;
                                    break;
                            }
                        }
                    }
                }
            }
        }
        eventBufferPos = 0;
        encodeModifierKeyEvents(evt.getModifiers());
        encodeKeyEvent(key, down);
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

    public void keyPressed(KeyEvent evt) {
        processLocalKeyEvent(evt);
    }

    public void keyReleased(KeyEvent evt) {
        processLocalKeyEvent(evt);
    }

    public void keyTyped(KeyEvent evt) {
        evt.consume();
    }

    public void mousePressed(MouseEvent evt) {
        processLocalMouseEvent(evt, false);
    }

    public void mouseReleased(MouseEvent evt) {
        processLocalMouseEvent(evt, false);
    }

    public void mouseMoved(MouseEvent evt) {
        processLocalMouseEvent(evt, true);
    }

    public void mouseDragged(MouseEvent evt) {
        processLocalMouseEvent(evt, true);
    }

    public void processLocalKeyEvent(KeyEvent evt) {

        if (isConnected() && isProcessingEvents()) {
            if (display.handleKeyEvent(evt)) {
                if (!inputEnabled) {
                    if ((evt.getKeyChar() == 'r' || evt.getKeyChar() == 'R') && evt.getID() == KeyEvent.KEY_PRESSED) {
                        try {
                            requestFramebufferUpdate(0, 0, displayModel.getRfbWidth(), displayModel.getRfbHeight(), false);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    synchronized (display) {
                        try {
                            postKeyboardEvent(evt);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            evt.consume();
        }
    }

    public void processLocalMouseEvent(MouseEvent evt, boolean moved) {
        if (isProcessingEvents()) {
            if (inputEnabled) {
                if (context.getMouseEventDelay() != 0
                                && (evt.getID() == MouseEvent.MOUSE_MOVED || evt.getID() == MouseEvent.MOUSE_DRAGGED)) {
                    if (mouseEventDispatcher == null || !mouseEventDispatcher.isAlive()) {
                        mouseEventDispatcher = new MouseEventDispatcher();
                        mouseEventDispatcher.start();
                    }
                    mouseEventDispatcher.dispatch(evt);
                } else {
                    if (moved) {
                        doMoveCursor(evt.getX(), evt.getY());
                    }
                    synchronized (display) {
                        try {
                            postPointerEvent(evt);
                        } catch (IOException e) {
                        }
                        display.notify();
                    }
                }
            }
        }
    }

    private void doMoveCursor(int x, int y) {
        Rectangle displayClip = new Rectangle(displayModel.getImagex(), displayModel.getImagey(), display.getDisplayComponent()
                        .getSize().width - (displayModel.getImagex() * 2), display.getDisplayComponent().getSize().height
                        - (displayModel.getImagey() * 2));
        boolean changeToLocal = false;
        if (x < displayClip.x) {
            x = displayClip.x;
            changeToLocal = true;
        } else if (x >= displayClip.x + displayClip.width) {
            x = displayClip.x + displayClip.width - 1;
            changeToLocal = true;
        }
        if (y < displayClip.y) {
            y = displayClip.y;
            changeToLocal = true;
        } else if (y >= displayClip.y + displayClip.height) {
            y = displayClip.y + displayClip.height - 1;
            changeToLocal = true;
        }
        if (changeToLocal) {
            setLocalCursor(null, -1, -1);
        } else {
            updateCursor((int) (((float) x - (float) displayModel.getImagex()) / displayModel.getXscale()),
                (int) (((float) y - (float) displayModel.getImagey()) / displayModel.getYscale()));
        }
    }

    public void updateCursor(int realx, int realy) {
        cursorX = realx;
        cursorY = realy;
        if (!inputEnabled) {
            if (stopCursor != null) {
                setLocalCursor(stopCursor, 16, 16);
            } else {
                display.setCursor(Cursor.getDefaultCursor());
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

    public void mouseClicked(MouseEvent evt) {
        display.getDisplayComponent().requestFocus();
    }

    public void mouseEntered(MouseEvent evt) {
    }

    public void mouseExited(MouseEvent evt) {
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

    public void startRFBProtocol() throws IOException, AuthenticationException {
        if (context.isAdaptive()) {
            setInputStream(new ProtocolReader(monitor = new MonitorDataInputStream(transport.getInputStream())));
        } else {
            monitor = null;
            setInputStream(new ProtocolReader(transport.getInputStream()));
        }
        setOutputStream(transport.getOutputStream());
        setInputEnabled(!context.isViewOnly());
        Component displayComponent = display.getDisplayComponent();
        if (displayComponent != null) {
            displayComponent.addMouseListener(this);
            displayComponent.addMouseMotionListener(this);
            // #ifdef JAVA2
            displayComponent.addMouseWheelListener(new WheelListener());
            // #endif
        }
        if (!context.isLocalCursorDisplayed()) {
            setLocalCursor(emptyCursor, 0, 0);
        }
        processProtocol();
    }

    public boolean isConnected() {
        return transport == null ? false : !isClosed;
    }

    public void setLocalCursor(final Image img, final int hotx, final int hoty) {
        // #ifdef JAVA1
        /*
         * setLocalCursorImpl(img, hotx , hoty);
         */
        // #else
        java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
            public Object run() {
                setLocalCursorImpl(img, hotx, hoty);
                return null;
            }
        });
        // #endif
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

    private void setLocalCursorImpl(Image img, int hotX, int hotY) {
        if ((localCursorImage == null && img != null) || (img == null && localCursorImage != null) || (img != localCursorImage)) {
            localCursorImage = img;
            try {
                if (localCursorImage != null) {
                    setCursorImage(localCursorImage, hotX, hotY);
                    currentLocalCursorHotspot = new Point(hotX, hotY);
                } else {
                    display.setCursor(Cursor.getDefaultCursor());
                    currentLocalCursorHotspot = null;
                }
            } catch (Throwable t) {
                // Not supported for some reason
                display.setCursor(Cursor.getDefaultCursor());
                currentLocalCursorHotspot = null;
            }
        }
    }

    private void setCursorImage(Image img, int hotX, int hotY) throws Exception {
        Point hotspot = new Point(hotX, hotY);
        int iw = img.getWidth(null);
        int ih = img.getHeight(null);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Method createCustomCursorMethod = toolkit.getClass().getMethod("createCustomCursor",
            new Class[] { Image.class, Point.class, String.class });
        Cursor cursor = (Cursor) (createCustomCursorMethod.invoke(toolkit, new Object[] { img, hotspot, "none" }));
        display.setCursor(cursor);
    }

    private boolean adapt() {
        boolean fullUpdate = false;
        long speed = monitor.getSpeed();
        RFBEncoding newEncoding = currentEncoding;
        if (speed > 3128) {
            newEncoding = context.getEncoding(RFBContext.ENCODING_HEXTILE);
        } else if (speed < 1496) {
            newEncoding = context.getEncoding(RFBContext.ENCODING_TIGHT);
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

    class MouseEventDispatcher extends Thread {
        MouseEvent lastEvent;
        int events;

        public void run() {
            while (true) {
                try {
                    Thread.sleep(context.getMouseEventDelay());
                    if (lastEvent != null) {
                        postPointerEvent(lastEvent);
                        if (context.isLocalCursorDisplayed()) {
                            setLocalCursor(displayModel.getCursor(), displayModel.getHotX(), displayModel.getHotY());
                        } else {
                            doMoveCursor(lastEvent.getX(), lastEvent.getY());
                        }
                        lastEvent = null;
                    }
                } catch (InterruptedException ie) {
                    if (lastEvent != null && events > context.getMouseEventThreshold()) {
                        try {
                            postPointerEvent(lastEvent);
                        } catch (IOException ioe) {
                            break;
                        }
                        // if (context.isLocalCursorDisplayed()) {
                        // setLocalCursor(displayModel.getCursor(),
                        // displayModel.hotX, displayModel.hotY);
                        // } else {
                        doMoveCursor(lastEvent.getX(), lastEvent.getY());
                        // setLocalCursor(emptyCursor, 0, 0);
                        // }
                        lastEvent = null;
                        events = 0;
                    } else {
                        if (context.isLocalCursorDisplayed()) {
                            setLocalCursor(displayModel.getCursor(), displayModel.getHotX(), displayModel.getHotY());
                        } else {
                            setLocalCursor(dotCursor, 2, 2);
                        }
                    }
                } catch (IOException ioe) {
                    break;
                }
            }
        }

        public void dispatch(MouseEvent evt) {
            this.lastEvent = evt;
            events++;
            interrupt();
        }
    }

    /**
     * @return
     */
    public RFBEncoding getCurrentEncoding() {
        return currentEncoding;
    }

    public void setStopCursor(Image stopCursor) {
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
        requestedFullUpdate = true;
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

    // #ifdef JAVA2
    class WheelListener implements java.awt.event.MouseWheelListener {
        public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
            processLocalMouseEvent(e, false);
        }
    }
    // #endif

}