package com.sshtools.rfbserver.transport;

import java.io.IOException;

import com.sshtools.rfbserver.RFBServerConfiguration;

public interface RFBServerTransportFactory {
	/**
	 * Get the next transport.  This should block until a transport
	 * is available. When there will be no more transports available,
	 * <code>null</code> will be returned. 
	 * 
	 * @return transport
	 * @throws IOException if transport cannot be obtained
	 */
	RFBServerTransport nextTransport() throws IOException;
	
	/**
	 * Initialise the transport factory with the configuration required.
	 * 
	 * @param configuration configuration
	 */
	void init(RFBServerConfiguration configuration);
	
	/**
	 * Get if the transport factory is started.
	 * 
	 * @return started
	 */
	boolean isStarted();

	/**
	 * Start accepting connections.
	 * 
	 * @throws IOException if no connections can be accepted
	 */
	void start() throws IOException;

	/**
	 * Stop accepting connections.
	 */
	void stop();
}
