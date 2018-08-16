/**
 * VNC Server - A (mostly) pure Java VNC server based on the SSHTools RFB server components.
 *
 * This server currently contains a native driver Linux to greatly improve performance.
 *
 * Drivers for other platforms are in progress.
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
package com.sshtools.vncserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.UPNPResponseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbserver.transport.ServerSocketRFBServerTransportFactory;

public class UPnPServerSocketTransportFactory extends ServerSocketRFBServerTransportFactory {
	private final static int DISCOVERY_TIMEOUT = 5000;

	final static Logger LOG = LoggerFactory.getLogger(UPnPServerSocketTransportFactory.class);

	private boolean mapped;
	private int externalPort = -1;

	public UPnPServerSocketTransportFactory() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				if (mapped) {
					try {
						unmapUpnp();
					} catch (Exception e) {
						LOG.error("Failed to disable UPnP port mapping.", e);
					}
				}
			}
		});
	}

	public int getExternalPort() {
		return externalPort;
	}

	public void setExternalPort(int externalPort) {
		this.externalPort = externalPort;
	}

	@Override
	public void start() throws IOException {
		super.start();
		try {
			mapUpnp();
		} catch (Exception e) {
			LOG.error("Failed to enable UPnP mapping, viewers may not be able to connect from the internet.", e);
		}
	}

	@Override
	public void stop() {
		super.stop();
		try {
			unmapUpnp();
		} catch (Exception e) {
			LOG.error("Failed to disable UPnP port mapping.", e);
		}
	}

	private int getActualExternalPort() {
		return externalPort == -1 ? serverSocket.getLocalPort() : externalPort;
	}

	private void unmapUpnp() throws IOException, UPNPResponseException {
		if (mapped) {
			try {
				LOG.info("Attempting to remove UPnP port mapping, looking for UPnP capable routers");
				InternetGatewayDevice[] IGDs = InternetGatewayDevice.getDevices(DISCOVERY_TIMEOUT);
				if (IGDs != null) {
					InternetGatewayDevice testIGD = IGDs[0];
					LOG.info("Found device " + testIGD.getIGDRootDevice().getModelName());
					boolean unmapped = testIGD.deletePortMapping(null, serverSocket.getLocalPort(), "TCP");
					if (unmapped) {
						LOG.info("Port " + getActualExternalPort() + " unmapped");
					} else {
						LOG.warn("Failed to unmap UPnP port mapping on shutdown, you may need to manually remove the port forward for "
							+ getActualExternalPort());
					}
				}
			} finally {
				mapped = false;
			}
		}
	}

	private void mapUpnp() throws IOException, UPNPResponseException {
		if (mapped) {
			throw new IllegalStateException("Already mapped");
		}
		LOG.info("Looking for UPnP capable routers");
		InternetGatewayDevice[] IGDs = InternetGatewayDevice.getDevices(DISCOVERY_TIMEOUT);

		if (IGDs != null) {
			InternetGatewayDevice testIGD = IGDs[0];
			LOG.info("Found device " + testIGD.getIGDRootDevice().getModelName());
			InetAddress addr = serverSocket.getInetAddress();
			if (addr.isAnyLocalAddress()) {
				// Need a real address to forward to!
				LOG.info("   " + addr + " is a wildcard address, finding best address");
				addr = getBestAddress(addr);
			}

			if (addr.isAnyLocalAddress()) {
				LOG.info("Cannot find best address to use, UPnP will not be used");
			} else if (addr.isLoopbackAddress()) {
				// Need a real address to forward to!
				LOG.info("   " + addr.getAddress() + " is a loopback address, UPnP will not be used");
				addr = null;
			}

			if (addr != null) {
				LOG.info("Will map external port " + getActualExternalPort() + " to " + addr.getHostAddress() + ":"
					+ serverSocket.getLocalPort());
				mapped = testIGD.addPortMapping(configuration.getDesktopName(), null, serverSocket.getLocalPort(),
					getActualExternalPort(), addr.getHostAddress(), 0, "TCP");
				if (mapped) {
					LOG.info("Port " + getActualExternalPort() + " mapped to " + addr.getHostAddress() + ":"
						+ serverSocket.getLocalPort());
				} else {
					LOG.warn("Failed to map " + getActualExternalPort() + " to " + addr.getHostAddress() + ":"
						+ serverSocket.getLocalPort());
				}
			}
		}
	}

	private InetAddress getBestAddress(InetAddress addr) throws SocketException {
		Enumeration<NetworkInterface> networkIfs = NetworkInterface.getNetworkInterfaces();
		while (networkIfs.hasMoreElements()) {
			NetworkInterface networkIf = networkIfs.nextElement();
			if (!networkIf.isLoopback() && networkIf.isUp()) {
				LOG.info("Found a non-loopback address");
				boolean found = false;
				for (InterfaceAddress a : networkIf.getInterfaceAddresses()) {
					if (a.getAddress().getClass().equals(addr.getClass())) {
						LOG.info("Found address that is of same class");
						addr = a.getAddress();
						found = true;
					}
				}
				if (found) {
					break;
				}
			}
		}
		return addr;
	}
}
