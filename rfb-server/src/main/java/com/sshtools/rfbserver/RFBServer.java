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
package com.sshtools.rfbserver;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.RFBVersion;
import com.sshtools.rfbserver.drivers.DamageScannerDriver;
import com.sshtools.rfbserver.drivers.RobotDisplayDriver;
import com.sshtools.rfbserver.drivers.WindowedDisplayDriver;
import com.sshtools.rfbserver.files.RFBServerFS;
import com.sshtools.rfbserver.transport.RFBServerTransport;
import com.sshtools.rfbserver.transport.RFBServerTransportFactory;
import com.sshtools.rfbserver.transport.ServerSocketRFBServerTransportFactory;

public class RFBServer implements RFBClientContext {
    final static Logger LOG = LoggerFactory.getLogger(RFBServer.class);
    public final static RFBVersion RFB_VERSION = new RFBVersion(3, 8);

    private RFBServerTransportFactory transportFactory;
    private RFBServerConfiguration configuration;
    private DisplayDriver displayDriver;
    private boolean driverInited;
    private Map<RFBClient, RFBServerTransport> clients = new HashMap<RFBClient, RFBServerTransport>();
    private Object driverLock = new Object();
    private List<RFBAuthenticator> authenticators = new ArrayList<RFBAuthenticator>();
    private RFBServerFS serverFileSystem;
    private boolean stopping;

    public RFBServer(RFBServerConfiguration configuration, DisplayDriver displayDriver) {
        this.configuration = configuration;
        this.displayDriver = displayDriver;
    }

    public RFBServerFS getServerFileSystem() {
        return serverFileSystem;
    }

    public void setServerFileSystem(RFBServerFS serverFileSystem) {
        this.serverFileSystem = serverFileSystem;
    }

    public List<RFBAuthenticator> getSecurityHandlers() {
        return authenticators;
    }

    public void init(RFBServerTransportFactory transportFactory) throws Exception {
        this.transportFactory = transportFactory;
    }

    public RFBServerTransportFactory getTransportFactory() {
        return transportFactory;
    }

    public DisplayDriver getDisplayDriver() {
        return displayDriver;
    }

    public void start() throws IOException {
        if (transportFactory.isStarted()) {
            throw new IllegalStateException("Already started");
        }
        transportFactory.start();
        try {
            while (true) {
                try {
                    final RFBServerTransport transport = transportFactory.nextTransport();
                    if (transport == null) {
                        // Transport shutdown
                        break;
                    }
                    new Thread("Transport" + transport.hashCode()) {
                        public void run() {
                            try {
                                runTransport(transport);
                            } catch (Exception e) {
                                if (!(e instanceof IOException)
                                                || (!(e instanceof IOException) && !transport.isDisconnect((IOException) e))) {
                                    LOG.error("Connection failed.", e);
                                } else {
                                    LOG.info("Client disconnected normally");
                                }
                            } finally {
                                synchronized (driverLock) {
                                    if (clients.size() == 0 && driverInited) {
                                        displayDriver.destroy();
                                        driverInited = false;
                                    }
                                }
                            }
                        }
                    }.start();
                } catch (SocketException se) {
                    if (!stopping) {
                        throw se;
                    } else {
                        break;
                    }
                }
            }
        } finally {
            if (transportFactory.isStarted()) {
                stop();
            }
            stopping = false;
        }
    }

    public List<RFBClient> getClients() {
        return Collections.unmodifiableList(new ArrayList<RFBClient>(clients.keySet()));
    }

    public RFBVersion getVersion() {
        return RFB_VERSION;
    }

    public void stop() {
        if (transportFactory.isStarted()) {
            stopping = true;
            transportFactory.stop();
            for (RFBClient c : getClients()) {
                clients.get(c).stop();
            }
        } else {
            throw new IllegalStateException("Not started.");
        }
    }

    public static void main(String[] args) throws Exception {
        RFBServerTransportFactory fact = new ServerSocketRFBServerTransportFactory();
        RFBServerConfiguration config = new FixedRFBServerConfiguration();
        fact.init(config);
        RobotDisplayDriver x11Drivers = new RobotDisplayDriver();
        WindowedDisplayDriver wdd = new WindowedDisplayDriver(x11Drivers);
        wdd.setArea(new Rectangle(1920, 0, 800, 600));
        DamageScannerDriver dsd = new DamageScannerDriver(wdd, true);
        RFBServer server = new RFBServer(config, dsd);
        server.init(fact);
        server.start();
    }

    public RFBServerConfiguration getConfiguration() {
        return configuration;
    }

    public void runTransport(RFBServerTransport transport, Runnable... onUpdate) throws IOException {
        LOG.info("Got connection");
        RFBClient client = createClient();
        synchronized (driverLock) {
            if (!driverInited) {
                try {
                    displayDriver.init();
                    driverInited = true;
                } catch (Exception e) {
                    throw new IOException("Failed to init driver.", e);
                }
            }
            clients.put(client, transport);
        }
        try {
            client.run(transport, onUpdate);
        } finally {
            clients.remove(client);
            onClientDestroyed(client);
        }
    }

    protected void onClientDestroyed(RFBClient client) {
    }

    protected RFBClient createClient() {
        return new RFBClient(this, displayDriver);
    }

    public RFBAuthenticator getSecurityHandler(int code) {
        for (RFBAuthenticator a : authenticators) {
            if (a.getCapability() != null && a.getCapability().getCode() == code) {
                return a;
            }
        }
        return null;
    }

	public boolean isStarted() {
		return transportFactory != null && transportFactory.isStarted();
	}
}
