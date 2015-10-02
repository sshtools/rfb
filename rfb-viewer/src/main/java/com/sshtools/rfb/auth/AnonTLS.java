package com.sshtools.rfb.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketOptions;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBAuthenticationException;
import com.sshtools.rfb.SecurityType;
import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;

public class AnonTLS implements SecurityType {
    final static Logger LOG = LoggerFactory.getLogger(ProtocolEngine.class);

    @Override
	public int process(ProtocolEngine engine) throws RFBAuthenticationException, IOException {

        // if (engine.getInputStream().readUnsignedByte() == 0) {
        // return 1;
        // }

        //
        // This is apparently 'anontls' mode (as x11vnc calls it). Introduced
        // for Vino server
        //
        // http://manpages.ubuntu.com/manpages/precise/man1/x11vnc.1.html
        //

        // CL used for testing ..

        // x11vnc -storepasswd password /home/tanktarta/.vncauth
        // x11vnc -rfbauth /home/tanktarta/.vncauth -clip xinerama0 -ssl
        // -vencrypt never -anontls support -ssltimeout 6000 -env
        // SSL_INIT_TIMEOUT=6000

        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance("TLS");
            // TODO TrustManager?
            ctx.init(new KeyManager[0], new TrustManager[] { new TrustManager() {
            } }, new SecureRandom());
        } catch (Exception e) {
            throw new RFBAuthenticationException("Failed to initialise SSL.", e);
        }

        // Wrap the existing streams in a Socket
        final ProtocolReader fIn = engine.getInputStream();
        final ProtocolWriter fOut = engine.getOutputStream();
        Socket socket = new IOStreamSocket(fIn, fOut);
        socket.connect(new InetSocketAddress(0));

        // Upgrade streams to TLS
        // SSLSocketFactory socketFactory = ctx.getSocketFactory();
        SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) socketFactory.createSocket(socket, "localhost", 0, true);

        // From 'CSecurityTLSBase' and 'CSecurity' from VNC java TLS patches
        // http://www.auto.tuwien.ac.at/~mkoegler/index.php/tlsvnc
        String[] supported;
        ArrayList<String> enabled = new ArrayList<String>();
        supported = sslSocket.getSupportedCipherSuites();
        for (int i = 0; i < supported.length; i++) {
            if (supported[i].matches(".*DH_anon.*")) {
                LOG.info("Enabling TLS " + supported[i]);
                enabled.add(supported[i]);
            }
        }
        sslSocket.setEnabledCipherSuites(enabled.toArray(new String[0]));

        LOG.info("Starting handshake");
        sslSocket.startHandshake();
        LOG.info("Started handshake, swapping streams");

        engine.setInputStream(new ProtocolReader(sslSocket.getInputStream()));
        engine.setOutputStream(new ProtocolWriter(sslSocket.getOutputStream()));
        LOG.info("Swapped streams");

        return engine.negotiateType().getType() + 2;
    }

    @Override
	public int getType() {
        return RFBConstants.SCHEME_TLS_AUTHENTICATION;
    }

    @Override
    public String toString() {
        return "AnonTLS";
    }

    /**
     * Wraps our I/O streams in a Socket for SSL's sake.
     */
    private final class IOStreamSocket extends Socket {
        private IOStreamSocket(final InputStream fIn, final OutputStream fOut) throws SocketException {
            super(new SocketImpl() {

                @Override
				public void setOption(int optID, Object value) throws SocketException {
                }

                @Override
				public Object getOption(int optID) throws SocketException {
                    // TODO make RFBTransport support this somehow?
                    switch (optID) {
                        case SocketOptions.TCP_NODELAY:
                            return Boolean.FALSE;
                        case SocketOptions.SO_LINGER:
                            return Boolean.FALSE;
                        default:
                            throw new UnsupportedOperationException("Unsupported option ID " + optID);
                    }
                }

                @Override
                protected void sendUrgentData(int data) throws IOException {
                    throw new UnsupportedOperationException();
                }

                @Override
                protected void listen(int backlog) throws IOException {
                    throw new UnsupportedOperationException();
                }

                @Override
                protected OutputStream getOutputStream() throws IOException {
                    return fOut;
                }

                @Override
                protected InputStream getInputStream() throws IOException {
                    return fIn;
                }

                @Override
                protected void create(boolean stream) throws IOException {
                }

                @Override
                protected void connect(SocketAddress address, int timeout) throws IOException {
                }

                @Override
                protected void connect(InetAddress address, int port) throws IOException {
                }

                @Override
                protected void connect(String host, int port) throws IOException {
                }

                @Override
                protected void close() throws IOException {
                }

                @Override
                protected void bind(InetAddress host, int port) throws IOException {
                }

                @Override
                protected int available() throws IOException {
                    return fIn.available();
                }

                @Override
                protected void accept(SocketImpl s) throws IOException {
                }
            });
        }
    }

    @Override
	public void postServerInitialisation(ProtocolEngine engine) throws IOException {
    }

    @Override
	public List<Integer> getSubAuthTypes() {
        return Arrays.asList(RFBConstants.SCHEME_CONNECT_FAILED, RFBConstants.SCHEME_NO_AUTHENTICATION,
            RFBConstants.SCHEME_VNC_AUTHENTICATION);
    }
}
