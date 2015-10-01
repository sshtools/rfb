/* HEADER */
package com.sshtools.vnc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.sshtools.profile.AuthenticationException;
import com.sshtools.profile.ProfileException;
import com.sshtools.profile.ProfileTransport;
import com.sshtools.profile.ResourceProfile;
import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBProtocolTransport;
import com.sshtools.virtualsession.VirtualSession;

public class VNCProtocolTransport implements RFBProtocolTransport {
	// Private instance variables
	protected Socket socket;

	protected ResourceProfile profile;

	private boolean connectionPending;

	protected VirtualSession virtualSession;

	protected String transportDescription;

	protected boolean transportSecure;

	public VNCProtocolTransport() {
	}

	public String getHostname() {
		return getProfile() == null || getProfile().getURI() == null ? null : getProfile().getURI().getHost();
	}

	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}

	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}

	public void close() throws IOException {
		disconnect();
	}

	public int getPort() {
		return getProfile() == null ? -1 : getProfile().getURI().getPort();
	}

	public boolean connect(ResourceProfile profile, Object parentUIComponent) throws ProfileException, AuthenticationException {
		if (isConnected()) {
			throw new ProfileException("Already connected");
		}
		try {
			int port = profile.getURI().getPort();
			if (port < 1) {
				port = 5900;
			}
			transportDescription = "Socket";
			socket = new Socket(profile.getURI().getHost(), port);
			this.profile = profile;
			return true;
		} catch (IOException ioe) {
			try {
				disconnect();
			} catch (IOException ioe2) {
			}
			throw new ProfileException(ioe);
		}
	}

	public void disconnect() throws IOException {
		try {
			if (isConnected()) {
				socket.close();
			}
		} finally {
			socket = null;
		}
	}

	private void disconnectEmbeddedClient() {
		// TODO have a switch to allow re-use of ticket
		// try {
		// ((EmbeddedVPNClient)vpn).disconnect();
		// }
		// catch(Throwable t) {
		// }
	}

	public boolean isConnected() {
		return socket != null;
	}

	public Object getProvider() {
		return null;
	}

	public boolean isCloneVirtualSessionSupported() {
		return false;
	}

	public ResourceProfile getProfile() {
		return profile;
	}

	public String getHostDescription() {
		return getHostname();
	}

	public String getProtocolDescription() {
		return "VNC";
	}

	public boolean isProtocolSecure() {
		return false;
	}

	public String getTransportDescription() {
		return transportDescription;
	}

	public boolean isTransportSecure() {
		return transportSecure;
	}

	public ProfileTransport cloneVirtualSession(VirtualSession session) throws CloneNotSupportedException, ProfileException {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.profile.ProfileTransport#isConnectionPending()
	 */
	public boolean isConnectionPending() {
		return connectionPending;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.terminal.VirtualSessionTransport#init(com.sshtools.
	 * virtualsession.VirtualSession)
	 */
	public void init(VirtualSession virtualSession) {
		this.virtualSession = virtualSession;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.terminal.VirtualSessionTransport#getVirtualSession()
	 */
	public VirtualSession getVirtualSession() {
		return virtualSession;
	}
}
