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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbserver.DisplayDriver;
import com.sshtools.rfbserver.RFBServer;
import com.sshtools.rfbserver.RFBServerConfiguration;
import com.sshtools.rfbserver.drivers.CopyRectDisplayDriver;
import com.sshtools.rfbserver.drivers.DamageScannerDriver;
import com.sshtools.rfbserver.drivers.RobotDisplayDriver;
import com.sshtools.rfbserver.drivers.WindowOutlineDisplayDriver;
import com.sshtools.rfbserver.drivers.WindowedDisplayDriver;
import com.sshtools.rfbserver.encodings.authentication.VNC;
import com.sshtools.rfbserver.linux.X11DisplayDriver;
import com.sshtools.rfbserver.transport.RFBServerTransportFactory;
import com.sshtools.rfbserver.transport.ServerSocketRFBServerTransportFactory;
import com.sshtools.rfbserver.transport.SocketRFBServerTransportFactory;
import com.sshtools.rfbserver.windows.Win32DisplayDriver;

public class VNCServer implements RFBServerConfiguration {
	public enum Mode {
		listen, reverse
	}

	private static final char OPT_UPNP_EXTERNAL_PORT = 'x';
	private static final char OPT_HELP = '?';
	private static final char OPT_MODE = 'm';
	private static final char OPT_NO_NATIVE = 't';
	private static final char OPT_VIEWPORT = 'v';
	private static final char OPT_NO_SCAN = 'n';
	private static final char OPT_INDEXED_ = 'i';
	private static final char OPT_NO_COPY_RECT = 'C';
	private static final char OPT_DESKTOP_NAME = 'k';
	private static final char OPT_BACKLOG = 'b';
	private static final char OPT_PASSWORD = 'p';
	private static final char OPT_PASSWORD_FILE = 'f';
	private static final char OPT_NO_OUTLINE_WINDOW_MOVEMENT = 'o';
	private static final char OPT_LOG_LEVEL = 'l';
	static Logger LOG;
	private CommandLine cli;
	private DisplayDriver driver;
	private String desktopName = "SSHTools Java VNC Server";
	private Mode mode = Mode.listen;
	private RFBServerTransportFactory serverTransportFactory;
	private DisplayDriver underlyingDriver;
	private String address = "localhost";
	private int port = 5900;
	private int backlog;
	private RFBServer server;
	private char[] password;
	private int imageType = BufferedImage.TYPE_INT_ARGB;

	public VNCServer() {
	}

	protected void createOptions() {
	}

	protected void addOptions(Options options) {
		options.addOption(new Option(String.valueOf(OPT_HELP), "help", false, "Display help"));
		options.addOption(new Option(String.valueOf(OPT_MODE), "mode", true,
				"Connection mode. May either be 'listen' (the default), or 'reverse' for to connect to a VNC viewer running in listen mode"));
		options.addOption(new Option("e", "encodings", true, "Comma separated list of enabled encoding"));
		options.addOption(new Option(String.valueOf(OPT_NO_NATIVE), "nonative", false,
				"Do not use the native display driver, even if one is supported."));
		options.addOption(new Option(String.valueOf(OPT_NO_SCAN), "noscan", false,
				"Do not use the (slow) damage scanner, rely on native damage events (if supported)"));
		options.addOption(new Option(String.valueOf(OPT_NO_COPY_RECT), "nocopyrect", false,
				"Do not use the CopyRect driver for window movement (if supported)"));
		options.addOption(new Option(String.valueOf(OPT_LOG_LEVEL), "log", true, "Log level"));
		options.addOption(new Option(String.valueOf(OPT_NO_OUTLINE_WINDOW_MOVEMENT), "outline", false,
				"Disables the use of a 'wireframe' instead of the complete window contents when windows are moved or resized."));
		options.addOption(new Option(String.valueOf(OPT_UPNP_EXTERNAL_PORT), "upnpport", false,
				"The external port number to use if UPnP is enabled (defaults to same a listening port)"));
		options.addOption(new Option(String.valueOf(OPT_VIEWPORT), "viewport", true,
				"Serve either only a single monitor or an area of the entire desktop. To specify an area, use the format "
						+ "<X>,<Y>,<Width>,<Height>. If you have multiple monitors, you can use the shorthand format of <Monitor Number> "
						+ "instead."));
		options.addOption(new Option(String.valueOf(OPT_DESKTOP_NAME), "desktop", true,
				"The desktop name. Some viewers may display this. Can be any text"));
		options.addOption(
				new Option(String.valueOf(OPT_PASSWORD), "password", true, "The password that clients must authenticate with."));
		options.addOption(new Option(String.valueOf(OPT_PASSWORD_FILE), "passwordfile", true,
				"A file containing the password that clients must authenticate with."));
		options.addOption(new Option(String.valueOf(OPT_BACKLOG), "backlog", true,
				"Maximum number of incoming connections that are allowed. Only applies in 'listen' mode"));
	}

	private void start() throws Exception {
		server = new RFBServer(this, driver);
		if (password != null) {
			server.getSecurityHandlers().add(new VNC() {
				@Override
				protected char[] getPassword() {
					return password;
				}
			});
		}
		server.init(serverTransportFactory);
		server.start();
	}

	protected int parseArguments(String[] args) throws IOException {
		Options options = new Options();
		addOptions(options);
		Parser parser = new GnuParser();
		try {
			cli = parser.parse(options, args);
			// Debug level
			String level = "warn";
			if (cli.hasOption(OPT_LOG_LEVEL)) {
				level = cli.getOptionValue(OPT_LOG_LEVEL);
			}
			System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", level);
			LOG = LoggerFactory.getLogger(VNCServer.class);
			// Help?
			if (cli.hasOption(OPT_HELP)) {
				printHelp(options);
				return -1;
			}
			// Determine driver
			if (SystemUtils.IS_OS_UNIX && !cli.hasOption(OPT_NO_NATIVE)) {
				underlyingDriver = driver = new X11DisplayDriver();
			} else if (SystemUtils.IS_OS_WINDOWS && !cli.hasOption(OPT_NO_NATIVE)) {
				underlyingDriver = driver = new Win32DisplayDriver();
			} else {
				underlyingDriver = driver = new RobotDisplayDriver();
			}
			// Set view port
			if (cli.hasOption(OPT_VIEWPORT)) {
				WindowedDisplayDriver windowedDriver = new WindowedDisplayDriver(driver);
				try {
					String viewport = cli.getOptionValue(OPT_VIEWPORT);
					if (viewport.indexOf(',') != -1) {
						String[] rec = viewport.split(",");
						if (rec.length != 4) {
							throw new NumberFormatException();
						}
						windowedDriver.setArea(new Rectangle(Integer.parseInt(rec[0]), Integer.parseInt(rec[1]),
								Integer.parseInt(rec[2]), Integer.parseInt(rec[3])));
					} else {
						windowedDriver.setMonitor(Integer.parseInt(viewport));
					}
				} catch (NumberFormatException nfe) {
					throw new ParseException(
							"Viewport must either be a single monitor number or a string specifying the bounds of the viewport in the format <X>,<Y>,<Width>,<Height>");
				}
				driver = windowedDriver;
			}
			// Add the damage driver (catches any damage not detected by the
			// display
			// driver)
			if (!cli.hasOption(OPT_NO_SCAN)) {
				driver = new DamageScannerDriver(driver, true);
			}
			// Add the window outline driver
			if (!cli.hasOption(OPT_NO_OUTLINE_WINDOW_MOVEMENT)) {
				driver = new WindowOutlineDisplayDriver(driver);
			}
			// Add the CopyRect driver
			if (!cli.hasOption(OPT_NO_COPY_RECT)) {
				driver = new CopyRectDisplayDriver(driver);
			}
			// Listen mode
			if (cli.hasOption(OPT_MODE)) {
				try {
					mode = Mode.valueOf(cli.getOptionValue(OPT_MODE));
				} catch (Exception e) {
					throw new ParseException("Invalid mode. May be one of " + toCommaSeparatedString((Object[]) Mode.values()));
				}
			}
			switch (mode) {
			case reverse:
				serverTransportFactory = new SocketRFBServerTransportFactory();
			default:
				serverTransportFactory = new ServerSocketRFBServerTransportFactory();
				break;
			}
			serverTransportFactory.init(this);
			// Other options
			if (cli.hasOption(OPT_DESKTOP_NAME)) {
				desktopName = cli.getOptionValue(OPT_DESKTOP_NAME);
			}
			// Backlog
			if (cli.hasOption(OPT_BACKLOG)) {
				try {
					backlog = Integer.parseInt(cli.getOptionValue(OPT_BACKLOG));
				} catch (NumberFormatException nfe) {
					throw new ParseException("Invalid backlog.");
				}
			}
			// Password
			if (cli.hasOption(OPT_PASSWORD_FILE)) {
				BufferedReader in = new BufferedReader(new FileReader(cli.getOptionValue(OPT_PASSWORD_FILE)));
				try {
					password = in.readLine().toCharArray();
				} finally {
					in.close();
				}
			} else if (cli.hasOption(OPT_PASSWORD)) {
				password = cli.getOptionValue(OPT_PASSWORD).toCharArray();
			}
			// Parse remaining arguments
			String[] remainingArgs = cli.getArgs();
			if (remainingArgs.length > 1) {
				throw new ParseException("Expected at most a single argument containing [<address>][:port]");
			} else if (remainingArgs.length == 0) {
				address = "0.0.0.0";
				port = 5900;
			} else {
				try {
					// May just be a port number
					port = Integer.parseInt(remainingArgs[0]);
					address = "0.0.0.0";
				} catch (NumberFormatException nfe) {
					address = remainingArgs[0];
					port = 5900;
					int idx = address.indexOf(':');
					if (idx != -1) {
						try {
							port = Integer.parseInt(address.substring(idx + 1));
						} catch (NumberFormatException nfe2) {
							throw new ParseException("Invalid port number.");
						}
						address = address.substring(0, idx);
					}
				}
			}
			// Output some info about the options chosen
			LOG.info("Driver: " + driver);
			LOG.info("Transport: " + serverTransportFactory.getClass().getSimpleName());
			return 0;
		} catch (ParseException pe) {
			System.err.println(getClass().getName() + ": " + pe.getMessage() + " Use -? or --help for more information.");
			return 2;
		}
	}

	private void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(getClass().getSimpleName(), "A (mostly) pure Java VNC server", options, "SSHTools", true);
	}

	public static void main(String[] args) throws Exception {
		VNCServer server = new VNCServer();
		int result = server.parseArguments(args);
		if (result == 0) {
			server.start();
		} else if (result > 0) {
			System.exit(result);
		}
	}

	public int getPort() {
		return port;
	}

	public int getListenBacklog() {
		return backlog;
	}

	public String getAddress() {
		return address;
	}

	public String getDesktopName() {
		return desktopName;
	}

	private static String toCommaSeparatedString(Object... objects) {
		StringBuilder bui = new StringBuilder();
		for (Object o : objects) {
			if (bui.length() > 0) {
				bui.append(",");
			}
			bui.append(String.valueOf(o));
		}
		return bui.toString();
	}

	public int getImageType() {
		return imageType;
	}
}
