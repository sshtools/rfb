/**
 * Swing VNC Viewer - Simple Swing based VNC viewer based on SSHTools' RFB Components.
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
package com.sshtools.vnc.viewer.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfb.RFBAuthenticationException;
import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfb.RFBEventHandler;
import com.sshtools.rfb.RFBTransport;
import com.sshtools.rfb.swing.SwingRFBDisplay;
import com.sshtools.rfb.swing.SwingRFBToolkit;

@SuppressWarnings("serial")
public class VNCViewer extends JFrame implements RFBEventHandler {
	static Logger LOG;
	private static final char OPT_HELP = '?';
	private static final char OPT_LOG_LEVEL = 'l';
	private static final char OPT_NO_COPY_RECT = 'C';
	private static final char OPT_PASSWORD = 'p';
	private static final char OPT_PASSWORD_FILE = 'f';
	private String address = "localhost";
	private CommandLine cli;
	private char[] password;
	private int port = 5900;
	private RFBContext context;
	private RFBTransport transport;
	private SwingRFBDisplay display;
	private Socket socket;

	public VNCViewer() {
		super("Remote Desktop");
	}

	protected void addOptions(Options options) {
		options.addOption(new Option(String.valueOf(OPT_HELP), "help", false, "Display help"));
		options.addOption(new Option("e", "encodings", true, "Comma separated list of enabled encoding"));
		options.addOption(new Option(String.valueOf(OPT_NO_COPY_RECT), "nocopyrect", false,
				"Do not use the CopyRect driver for window movement (if supported)"));
		options.addOption(new Option(String.valueOf(OPT_LOG_LEVEL), "log", true, "Log level"));
		options.addOption(
				new Option(String.valueOf(OPT_PASSWORD), "password", true, "The password that clients must authenticate with."));
		options.addOption(new Option(String.valueOf(OPT_PASSWORD_FILE), "passwordfile", true,
				"A file containing the password that clients must authenticate with."));
	}

	protected void createOptions() {
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
			System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", level.toLowerCase());
			LOG = LoggerFactory.getLogger(VNCViewer.class);
			// Help?
			if (cli.hasOption(OPT_HELP)) {
				printHelp(options);
				return -1;
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
			return 0;
		} catch (ParseException pe) {
			System.err.println(getClass().getName() + ": " + pe.getMessage() + " Use -? or --help for more information.");
			return 2;
		}
	}

	public void connect() throws IOException, RFBAuthenticationException {
		LOG.info("Connecting to desktop");
		display.getEngine().startRFBProtocol();
	}

	@Override
	public String passwordAuthenticationRequired() {
		LOG.info("Password authentication requested");
		if (password == null) {
			Object opt = JOptionPane.showInputDialog(null, "Password is required to continue", "Password",
					JOptionPane.QUESTION_MESSAGE, null, null, null);
			return opt == null ? null : String.valueOf(opt);
		} else {
			return new String(password);
		}
	}

	@Override
	public void connected() {
		LOG.info("Connected to desktop");
	}

	@Override
	public void disconnected() {
		LOG.info("Disconnected from desktop");
	}

	@Override
	public void remoteResize(int width, int height) {
		LOG.info(String.format("Desktop resized to %d x %d", width, height));
		display.setPreferredSize(new Dimension(width, height));
		pack();
	}

	@Override
	public void encodingChanged(RFBEncoding currentEncoding) {
	}

	private void open() throws IOException, RFBAuthenticationException {
		context = new RFBContext();
		transport = new RFBTransport() {
			@Override
			public int getPort() {
				return socket.getLocalPort();
			}

			@Override
			public OutputStream getOutputStream() throws IOException {
				return socket.getOutputStream();
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return socket.getInputStream();
			}

			@Override
			public String getHostname() {
				return socket.getLocalAddress().getHostAddress();
			}

			@Override
			public void close() throws IOException {
				socket.close();
			}
		};
		display = new SwingRFBDisplay();
		display.initialiseSession(transport, context, this);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(display, BorderLayout.CENTER);
		setSize(800, 600);
		socket = new Socket(address, port);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		setVisible(true);
		connect();
	}

	private void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(getClass().getSimpleName(), "A pure Java VNC viewerr", options, "Provided by SSHTOOLS Limited.", true);
	}

	public static void main(String[] args) throws Exception {
		new SwingRFBToolkit();
		VNCViewer server = new VNCViewer();
		int result = server.parseArguments(args);
		if (result == 0) {
			server.open();
		} else if (result > 0) {
			System.exit(result);
		}
	}
}
