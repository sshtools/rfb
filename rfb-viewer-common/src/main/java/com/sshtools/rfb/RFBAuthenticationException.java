/* HEADER */
package com.sshtools.rfb;

/**
 * Exception thrown if the authentication with a host fails for some reason
 * during connection.
 */
public class RFBAuthenticationException extends Exception {

	private static final long serialVersionUID = 1L;

	public RFBAuthenticationException() {
		super();
	}

	public RFBAuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}

	public RFBAuthenticationException(Throwable cause) {
		super(cause);
	}

	public RFBAuthenticationException(String message) {
		super(message);
	}
}
