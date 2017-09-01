/* HEADER */
package com.sshtools.rfb;

/**
 * Callback interface that enables the RFB protocol to prompt the user for a
 * password.
 * 
 * @author Lee David Painter
 */
public interface RFBEventHandler {

	String passwordAuthenticationRequired();

	void connected();

	void disconnected();

	void remoteResize(int width, int height);

	void encodingChanged(RFBEncoding currentEncoding);
}