/* HEADER */
package com.sshtools.rfb.awt;

import java.applet.Applet;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.Vector;

import com.sshtools.profile.AuthenticationException;
import com.sshtools.profile.ProfileTransport;
import com.sshtools.profile.ResourceProfile;
import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfb.RFBEventHandler;
import com.sshtools.rfb.RFBProtocolTransport;
import com.sshtools.ui.awt.OptionDialog;
import com.sshtools.virtualsession.VirtualSession;
import com.sshtools.virtualsession.VirtualSessionListener;
import com.sshtools.virtualsession.VirtualSessionManager;

public class AWTRFBVirtualSession extends Panel implements VirtualSession, ClipboardOwner {

	private static final long serialVersionUID = 1L;

	private RFBDisplay display;
	private VirtualSessionManager virtualSessionManager;

	private RFBProtocolTransport transport;

	private Vector listeners;

	private ScrollPane desktopScrollPane;

	private Component displayComponent;

	private boolean resizeDesktop;

	private RFBContext context;

	public AWTRFBVirtualSession() {
		super(new GridLayout(1, 1));
		context = new RFBContext();
		listeners = new Vector();
		display = new AWTRFBDisplay();
		desktopScrollPane = new ScrollPane();
	}

	public void setResizeDesktop(boolean resizeDesktop) {
		this.resizeDesktop = resizeDesktop;
	}

	/**
	 * Get the display in use
	 */
	public RFBDisplay getDisplay() {
		return display;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.virtualsession.VirtualSession#reset()
	 */
	public void reset() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.virtualsession.VirtualSession#getTitle()
	 */
	public String getSessionTitle() {
		return "VNC";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.virtualsession.VirtualSession#isConnected()
	 */
	public boolean isConnected() {
		return display.getEngine() != null && display.getEngine().isConnected();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sshtools.virtualsession.VirtualSession#init(com.sshtools.virtualsession
	 * .VirtualSessionManager)
	 */
	public void init(VirtualSessionManager virtualSessionManager) {
		this.virtualSessionManager = virtualSessionManager;
		createDisplayComponent();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sshtools.virtualsession.VirtualSession#getVirtualSessionManager()
	 */
	public VirtualSessionManager getVirtualSessionManager() {
		return virtualSessionManager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.virtualsession.VirtualSession#disconnect(boolean)
	 */
	public void disconnect(boolean doDisconnect, Throwable exception) {
		try {
			if (doDisconnect && transport != null && transport.isConnected()) {
				transport.disconnect();
			}
		} catch (IOException ioe) {
		}
		if (display.getEngine().isConnected()) {
			display.getEngine().disconnect();
			fireDisconnected(exception);
			fireTitleChanged(getSessionTitle());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sshtools.virtualsession.VirtualSession#addVirtualSessionListener(
	 * com.sshtools.virtualsession.VirtualSessionListener)
	 */
	public void addVirtualSessionListener(VirtualSessionListener listener) {
		listeners.addElement(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sshtools.virtualsession.VirtualSession#removeVirtualSessionListener
	 * (com.sshtools.virtualsession.VirtualSessionListener)
	 */
	public void removeVirtualSessionListener(VirtualSessionListener listener) {
		listeners.removeElement(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sshtools.virtualsession.VirtualSession#connect(com.sshtools.profile
	 * .ProfileTransport)
	 */
	public void connect(ProfileTransport transport) {
		this.transport = (RFBProtocolTransport) transport;
		ResourceProfile profile = transport.getProfile();
		setVirtualSessionProperties(profile);
		try {
			display.getDisplayComponent().requestFocus();
			display.initialiseSession((RFBProtocolTransport) transport, context, new RFBEventHandler() {

				public String passwordAuthenticationRequired() {
					char[] pw = requestPassword();
					return pw == null ? null : new String(pw);
				}

				public void connected() {
					// createDisplayComponent();
				}

				public void disconnected() {
					disconnect(true, null);
				}

				public void resized(int width, int height) {
					if (desktopScrollPane != null) {
						if (resizeDesktop) {
							// Try to find a visible frame, or do
							// nothing if we are contained in an applet
							Container c = AWTRFBVirtualSession.this.getParent();
							while (c != null) {
								if (c instanceof Applet) {
									desktopScrollPane.invalidate();
									desktopScrollPane.validate();
									desktopScrollPane.repaint();
									break;
								}
								if (c instanceof Frame) {
									if (c.isVisible()) {
										desktopScrollPane.setSize(new Dimension(width + 2, height + 2));
										((Frame) c).pack();
									}
									break;
								}
								c = c.getParent();
							}
						} else {
							desktopScrollPane.invalidate();
							desktopScrollPane.validate();
							desktopScrollPane.repaint();
						}
					}
				}

				public void encodingChanged(RFBEncoding currentEncoding) {
					updateEncoding(currentEncoding);
				}

			});
			String userInfo = decode(profile.getURI().getUserinfo());
			if (!"".equals(profile.getApplicationProperty("RFB.Password", ""))) {
				display.getEngine().setInitialPassword(profile.getApplicationProperty("RFB.Password", "").toCharArray());
			} else {
				if (userInfo != null && profile.getURI().getScheme().equals("vnc")) {
					String pw = profile.getPassword();
					if (pw != null) {
						display.getEngine().setInitialPassword(pw.toCharArray());
					}

				}
			}
			display.getEngine().startRFBProtocol();
			display.getDisplayComponent().repaint();
			fireConnected();
			fireTitleChanged(getSessionTitle());
		} catch (IOException e) {
			disconnect(true, e);
			display.getDisplayModel().init();
			display.getDisplayComponent().repaint();
			OptionDialog.error(this, "Error", "Failed to connect.", e);
		} catch (AuthenticationException e) {
			disconnect(true, e);
			display.getDisplayModel().init();
			display.getDisplayComponent().repaint();
			OptionDialog.error(this, "Error", "Failed to connect.", e);
		}

		//
	}

	public void updateEncoding(RFBEncoding encoding) {

	}

	public char[] requestPassword() {
		return OptionDialog.promptForAuthentication(this, "Password", "VNC password: ");
	}

	private void setDisplayComponent(Component displayComponent) {
		if ((this.displayComponent != null && displayComponent == null)
			|| (this.displayComponent == null || displayComponent != null) || this.displayComponent != displayComponent) {
			this.displayComponent = displayComponent;
			invalidate();
			removeAll();
			if (displayComponent != null) {
				add(displayComponent);
			}
			validate();
			repaint();
		}
	}

	private void createDisplayComponent() {
		if (display != null) {
			if (context.getScaleMode() != RFBDisplay.NO_SCALING) {
				desktopScrollPane.invalidate();
				desktopScrollPane.removeAll();
				desktopScrollPane.validate();
				setDisplayComponent(display.getDisplayComponent());
			} else {
				desktopScrollPane.invalidate();
				desktopScrollPane.removeAll();
				desktopScrollPane.add(display.getDisplayComponent());
				desktopScrollPane.validate();
				setDisplayComponent(desktopScrollPane);
			}
		}
	}

	private void fireTitleChanged(String newTitle) {
		for (int i = listeners.size() - 1; i >= 0; i--) {
			((VirtualSessionListener) listeners.elementAt(i)).titleChanged(this, newTitle);
		}
	}

	private void fireConnected() {
		for (int i = listeners.size() - 1; i >= 0; i--) {
			((VirtualSessionListener) listeners.elementAt(i)).connected(this);
		}
	}

	private void fireDisconnected(Throwable exception) {
		for (int i = listeners.size() - 1; i >= 0; i--) {
			((VirtualSessionListener) listeners.elementAt(i)).disconnected(this, exception);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.virtualsession.VirtualSession#getTransport()
	 */
	public ProfileTransport getTransport() {
		return transport;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.datatransfer.ClipboardOwner#lostOwnership(java.awt.datatransfer
	 * .Clipboard, java.awt.datatransfer.Transferable)
	 */
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sshtools.sshterm.SshTermVirtualSession#setVirtualSessionProperties
	 * (com.sshtools.profile.ResourceProfile)
	 */
	public void setVirtualSessionProperties(ResourceProfile profile) {
		int lastScaleMode = context.getScaleMode();
		context.setProfile(profile);

		// If the scaling mode has changed, then rebuild the layout
		if (context.getScaleMode() != lastScaleMode || displayComponent == null) {
			createDisplayComponent();
		}
	}

	public static String decode(String s) {
		return decode(s, true);
	}

	public static String decode(String s, boolean decodePlus) {

		boolean needToChange = false;
		StringBuffer sb = new StringBuffer();
		int numChars = s.length();
		int i = 0;

		while (i < numChars) {
			char c = s.charAt(i);
			switch (c) {
			case '+':
				if (decodePlus) {
					sb.append(' ');
					i++;
					needToChange = true;
				} else {
					sb.append(c);
					i++;
				}
				break;
			case '%':

				try {

					byte[] bytes = new byte[(numChars - i) / 3];
					int pos = 0;

					while (((i + 2) < numChars) && (c == '%')) {
						bytes[pos++] = (byte) Integer.parseInt(s.substring(i + 1, i + 3), 16);
						i += 3;
						if (i < numChars)
							c = s.charAt(i);
					}

					if ((i < numChars) && (c == '%'))
						throw new IllegalArgumentException("Incomplete trailing escape"); //$NON-NLS-1$

					sb.append(new String(bytes, 0, pos));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Illegal hex character. " //$NON-NLS-1$
						+ e.getMessage());
				}
				needToChange = true;
				break;
			default:
				sb.append(c);
				i++;
				break;
			}
		}

		return (needToChange ? sb.toString() : s);
	}

	class ReturnStatus {
		int status;
	}
}