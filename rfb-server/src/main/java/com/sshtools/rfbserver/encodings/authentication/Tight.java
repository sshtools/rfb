/**
 * RFB Server - Remote Frame Buffer (VNC Server) implementation. This is the base module if you want to create a VNC server. It takes a layered driver approach to add native specific features (which is recommened as the cross-platform default "Robot" driver is very slow).
 *
 * See the vncserver module for a concrete server implementation that has some native performance improvements for some platforms.
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
package com.sshtools.rfbserver.encodings.authentication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.RFBAuthenticator;
import com.sshtools.rfbserver.RFBClient;

public class Tight extends AbstractAuth {
	final static Logger LOG = LoggerFactory.getLogger(RFBClient.class);
	private List<RFBAuthenticator> authenticationMethods = new ArrayList<RFBAuthenticator>();
	private List<TightCapability> serverToClientCapabilities = new ArrayList<TightCapability>();
	private List<TightCapability> clientToServerCapabilities = new ArrayList<TightCapability>();

	public Tight() {
		super(RFBConstants.CAP_AUTH_TIGHT);
		// S -> C
		addServerToClientCapability(RFBConstants.CAP_FTSCSRLY);
		addServerToClientCapability(RFBConstants.CAP_FTSFLRLY);
		addServerToClientCapability(RFBConstants.CAP_FTSM5RLY);
		addServerToClientCapability(RFBConstants.CAP_FTSFURLY);
		addServerToClientCapability(RFBConstants.CAP_FTSUDRLY);
		addServerToClientCapability(RFBConstants.CAP_FTSUERLY);
		addServerToClientCapability(RFBConstants.CAP_FTSFDRLY);
		addServerToClientCapability(RFBConstants.CAP_FTSDDRLY);
		addServerToClientCapability(RFBConstants.CAP_FTSDERLY);
		addServerToClientCapability(RFBConstants.CAP_FTSMDRLY);
		addServerToClientCapability(RFBConstants.CAP_FTSFTRLY);
		addServerToClientCapability(RFBConstants.CAP_FTSFMRLY);
		addServerToClientCapability(RFBConstants.CAP_FTSDSRLY);
		addServerToClientCapability(RFBConstants.CAP_FTLRFRLY);
		addServerToClientCapability(RFBConstants.CAP_CUS_EOCU);
		// C -> S
		addClientToServerCapability(RFBConstants.CAP_VDFREEZ);
		addClientToServerCapability(RFBConstants.CAP_FTCCSRST);
		addClientToServerCapability(RFBConstants.CAP_FTCFLRST);
		addClientToServerCapability(RFBConstants.CAP_FTCM5RST);
		addClientToServerCapability(RFBConstants.CAP_FTCFURST);
		addClientToServerCapability(RFBConstants.CAP_FTCUDRST);
		addClientToServerCapability(RFBConstants.CAP_FTCUERST);
		addClientToServerCapability(RFBConstants.CAP_FTCFDRST);
		addClientToServerCapability(RFBConstants.CAP_FTCDDRST);
		addClientToServerCapability(RFBConstants.CAP_FTCMDRST);
		addClientToServerCapability(RFBConstants.CAP_FTCFRRST);
		addClientToServerCapability(RFBConstants.CAP_FTCFMRST);
		addClientToServerCapability(RFBConstants.CAP_FTCDSRST);
		addClientToServerCapability(RFBConstants.CAP_CUC_ENCU);
		// addClientToServerCapability(RFBConstants.CAP_FILE_CREATE_DIR_REQUEST);
		// addClientToServerCapability(RFBConstants.CAP_FILE_DOWNLOAD_CANCEL);
		// addClientToServerCapability(RFBConstants.CAP_FILE_UPLOAD_FAILED);
		// addClientToServerCapability(RFBConstants.CAP_FILE_DOWNLOAD_REQUEST);
		// addClientToServerCapability(RFBConstants.CAP_FILE_LIST_REQUEST);
		// addClientToServerCapability(RFBConstants.CAP_FILE_UPLOAD_DATA);
		// addClientToServerCapability(RFBConstants.CAP_FILE_UPLOAD_REQUEST);
		//
		// addServerToClientCapability(RFBConstants.CAP_FILE_DOWNLOAD_DATA);
		// addServerToClientCapability(RFBConstants.CAP_FILE_LIST_DATA);
		// addServerToClientCapability(RFBConstants.CAP_FILE_UPLOAD_CANCEL);
		// addServerToClientCapability(RFBConstants.CAP_FILE_DOWNLOAD_FAILED);
	}

	public void addServerToClientCapability(TightCapability cap) {
		serverToClientCapabilities.add(cap);
	}

	public void addClientToServerCapability(TightCapability cap) {
		clientToServerCapabilities.add(cap);
	}

	public List<TightCapability> getServerToClientCapabilities() {
		return serverToClientCapabilities;
	}

	public List<TightCapability> getClientToServerCapabilities() {
		return clientToServerCapabilities;
	}

	public boolean process(RFBClient rfbClient) throws AuthenticationException {
		try {
			LOG.info("Processing Tight authentication");
			ProtocolWriter output = rfbClient.getOutput();
			// Tunnel types (currently zero)
			output.writeUInt32(0);
			output.flush();
			// Auth types
			output.writeUInt32(authenticationMethods.size());
			if (!authenticationMethods.isEmpty()) {
				for (RFBAuthenticator cap : authenticationMethods) {
					if (cap.getCapability() != getCapability()) {
						LOG.info("Offering authentication via " + cap);
						cap.getCapability().write(output);
					}
				}
			}
			output.flush();
			// Selected type
			int selectedAuthentication = rfbClient.getInput().readInt();
			LOG.info("Chosen type " + selectedAuthentication);
			// Find the authentication
			RFBAuthenticator authenticator = getAuthenticationMethod(selectedAuthentication);
			if (authenticator == null) {
				throw new AuthenticationException("No authenticator with code of " + selectedAuthentication);
			}
			LOG.info("Using " + authenticator.getCapability().getSignature());
			// Authentication itself
			authenticator.process(rfbClient);
			return true;
		} catch (IOException ioe) {
			throw new AuthenticationException("I/O error during authentication.");
		}
	}

	public RFBAuthenticator getAuthenticationMethod(int code) {
		for (RFBAuthenticator a : authenticationMethods)
			if (a.getSecurityType() == code)
				return a;
		return null;
	}

	public List<RFBAuthenticator> getAuthenticationMethods() {
		return authenticationMethods;
	}

	public List<Integer> getSubAuthTypes() {
		return Arrays.asList(RFBConstants.SCHEME_CONNECT_FAILED, RFBConstants.SCHEME_NO_AUTHENTICATION,
				RFBConstants.SCHEME_VNC_AUTHENTICATION);
	}

	public void postAuthentication(RFBClient rfbClient) throws IOException {
		List<TightCapability> enabledEncodingsAsCapabilities = rfbClient.getEncoder().getAvailableEncodingsAsCapabilities();
		rfbClient.getOutput().writeShort(serverToClientCapabilities.size());
		rfbClient.getOutput().writeShort(clientToServerCapabilities.size());
		rfbClient.getOutput().writeShort(enabledEncodingsAsCapabilities.size());
		rfbClient.getOutput().writeShort(0);
		LOG.info("Server to client caps. ..");
		writeCaps(rfbClient, serverToClientCapabilities);
		LOG.info("Client to server caps. ..");
		writeCaps(rfbClient, clientToServerCapabilities);
		LOG.info("Encoding caps. ..");
		writeCaps(rfbClient, enabledEncodingsAsCapabilities);
		rfbClient.getOutput().flush();
	}

	protected void writeCaps(RFBClient client, List<TightCapability> caps) throws IOException {
		for (TightCapability c : caps) {
			LOG.info("Offering capability " + c);
			c.write(client.getOutput());
		}
	}
}
