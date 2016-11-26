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

public class Tight implements RFBAuthenticator {

    final static Logger LOG = LoggerFactory.getLogger(RFBClient.class);

    private List<RFBAuthenticator> authenticationMethods = new ArrayList<RFBAuthenticator>();
    private List<TightCapability> serverToClientCapabilities = new ArrayList<TightCapability>();
    private List<TightCapability> clientToServerCapabilities = new ArrayList<TightCapability>();

    public Tight() {// S -> C

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

    public TightCapability getCapability() {
        return RFBConstants.CAP_AUTH_TIGHT;
    }

    public List<TightCapability> getServerToClientCapabilities() {
        return serverToClientCapabilities;
    }

    public List<TightCapability> getClientToServerCapabilities() {
        return clientToServerCapabilities;
    }

    public int getSecurityType() throws AuthenticationException {
        return RFBConstants.SCHEME_TIGHT_AUTHENTICATION;
    }

    public boolean process(RFBClient rfbClient) throws AuthenticationException {
        try {
            LOG.info("Processing Tight authentication");
            ProtocolWriter output = rfbClient.getOutput();

            // Tunnel types (currently zero)
            output.writeInt(0);
            output.flush();

            // Auth types
            output.writeInt(authenticationMethods.size());
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
            RFBAuthenticator authenticator = rfbClient.getServer().getSecurityHandler(selectedAuthentication);
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