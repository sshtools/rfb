package com.sshtools.rfbserver.encodings.authentication;

import java.io.FilterInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.WrappedSocket;
import com.sshtools.rfbserver.RFBClient;

public class AnonTLS extends AbstractAuth {
	final static Logger LOG = LoggerFactory.getLogger(AnonTLS.class);

	public AnonTLS() {
		super(RFBConstants.SCHEME_TLS_AUTHENTICATION);
	}

	public void postAuthentication(RFBClient rfbClient) throws IOException {
	}

	public List<Integer> getSubAuthTypes() {
		return Arrays.asList(RFBConstants.SCHEME_CONNECT_FAILED, RFBConstants.SCHEME_NO_AUTHENTICATION,
				RFBConstants.SCHEME_VNC_AUTHENTICATION);
	}

	public boolean process(RFBClient rfbClient) throws AuthenticationException {
		try {
			// Make our I/O streams look like a Socket
			LOG.info("Creating SSL wrapped socket");
			final ProtocolReader underlyingInput = rfbClient.getInput();
			Socket wrapped = new WrappedSocket(underlyingInput, rfbClient.getOutput());
			InetSocketAddress endpoint = new InetSocketAddress("localhost", 0);
			wrapped.connect(endpoint);
			// Create an SSL socket from our wrapped socket
			SSLSocketFactory factory = ((SSLSocketFactory) SSLSocketFactory.getDefault());
			LOG.info("Creating SSL socket to " + endpoint.getHostName() + ":" + endpoint.getPort());
			SSLSocket sslSocket = (SSLSocket) factory.createSocket(wrapped, endpoint.getHostName(), endpoint.getPort(), true);
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
			sslSocket.setEnabledCipherSuites((String[]) enabled.toArray(new String[0]));
			// We are a server
			LOG.info("Setting client mode off");
			sslSocket.setUseClientMode(false);
			// Handshake
			// TODO - do we need to?
			LOG.info("Starting SSL handshake");
			sslSocket.startHandshake();
			// Upgrade the clients streams
			LOG.info("Swapping streams");
			rfbClient.setInput(new ProtocolReader(new FilterInputStream(sslSocket.getInputStream()) {
				/*
				 * TODO This filter stream is a work around for the fact the SSL
				 * input streams ALWAYS return ZERO on available(). Because we
				 * only ever want to know if there is SOME data (>0), then
				 * querying the underlying stream should be enough. The long
				 * term solution is to replace the I/O streams with NIO channels
				 * 
				 * http://stackoverflow.com/questions/26320624/how-to-tell-if-
				 * java-sslsocket-has-data-available
				 */
				@Override
				public int available() throws IOException {
					return underlyingInput.available();
				}
			}));
			rfbClient.setOutput(new ProtocolWriter(sslSocket.getOutputStream()));
			return false;
		} catch (IOException ioe) {
			AuthenticationException authenticationException = new AuthenticationException("I/O failed.");
			authenticationException.initCause(ioe);
			throw authenticationException;
		}
	}
}
