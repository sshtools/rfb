package com.sshtools.rfb.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBAuthenticationException;
import com.sshtools.rfb.SecurityType;
import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;

public class Tight implements SecurityType {
    final static Logger LOG = LoggerFactory.getLogger(Tight.class);

    private List<TightCapability> serverCapabilities = new ArrayList<TightCapability>();
    private List<TightCapability> clientCapabilities = new ArrayList<TightCapability>();
    private List<TightCapability> serverEncodings = new ArrayList<TightCapability>();

    @Override
	public int process(ProtocolEngine engine) throws RFBAuthenticationException, IOException {
        int authScheme = RFBConstants.SCHEME_NO_AUTHENTICATION;
        processTunnels(engine);
        authScheme = processAuth(engine, authScheme);
        return authScheme + 2;
    }

    public List<TightCapability> getServerCapabilities() {
        return serverCapabilities;
    }

    public List<TightCapability> getClientCapabilities() {
        return clientCapabilities;
    }

    public List<TightCapability> getServerEncodings() {
        return serverEncodings;
    }

    private int processAuth(ProtocolEngine engine, int authScheme) throws IOException {
        int numberOfAuthTypes = engine.getInputStream().readInt();
        if (numberOfAuthTypes != 0) {
            List<Integer> supportedServerTypes = new ArrayList<Integer>();
            for (int i = 0; i < numberOfAuthTypes; i++) {
                TightCapability c = new TightCapability(engine.getInputStream());
                supportedServerTypes.add(Integer.valueOf(c.getCode()));
            }
            authScheme = engine.getSecurityTypeFactory().selectScheme(supportedServerTypes);
            LOG.info("Client supports auth type " + authScheme);
            engine.getOutputStream().writeInt(authScheme);
        }
        engine.getOutputStream().flush();
        return authScheme;
    }

    @Override
	public int getType() {
        return RFBConstants.SCHEME_TIGHT_AUTHENTICATION;
    }

    @Override
    public String toString() {
        return "Tight";
    }

    @Override
	public void postServerInitialisation(ProtocolEngine engine) throws IOException {
        LOG.info("Reading Tight Capabilities");
        ProtocolReader in = engine.getInputStream();
        int serverMessages = in.readUnsignedShort();
        int clientMessages = in.readUnsignedShort();
        int encodings = in.readUnsignedShort();
        in.readUnsignedShort(); // pad
        for (int i = 0; i < serverMessages; i++) {
            TightCapability c = new TightCapability(in);
            serverCapabilities.add(c);
            LOG.info("    Server: " + c);
        }
        for (int i = 0; i < clientMessages; i++) {
            TightCapability c = new TightCapability(in);
            clientCapabilities.add(c);
            LOG.info("    Client: " + c);
        }
        for (int i = 0; i < encodings; i++) {
            TightCapability c = new TightCapability(in);
            serverEncodings.add(c);
            LOG.info("    Encoding: " + c);
        }
    }

    private void processTunnels(ProtocolEngine engine) throws IOException {
        int numberOfTunnels = engine.getInputStream().readInt();
        if (numberOfTunnels != 0) {
            int selectedTunnelType = -1;
            for (int i = 0; i < numberOfTunnels; i++) {
                TightCapability c = new TightCapability(engine.getInputStream());
                selectedTunnelType = c.getCode();
            }
            if (selectedTunnelType != 0) {
                throw new IOException("Unsupported tunnel type");
            }
            LOG.info("Client supports tunnel type " + selectedTunnelType);
            engine.getOutputStream().writeInt(selectedTunnelType);
        } else {
            LOG.info("Server supports no tunnels");
        }
    }

    @Override
	public List<Integer> getSubAuthTypes() {
        return Arrays.asList(RFBConstants.SCHEME_CONNECT_FAILED, RFBConstants.SCHEME_NO_AUTHENTICATION,
            RFBConstants.SCHEME_VNC_AUTHENTICATION);
    }
}
