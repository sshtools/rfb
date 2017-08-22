package test;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.swing.JFrame;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfb.RFBAuthenticationException;
import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfb.RFBEventHandler;
import com.sshtools.rfb.RFBTransport;
import com.sshtools.rfb.swing.SwingRFBDisplay;
import com.sshtools.rfb.swing.SwingRFBToolkit;
import com.sshtools.rfbcommon.RFBConstants;

@SuppressWarnings("serial")
public class Test extends JFrame implements RFBEventHandler {
	final static Logger LOG = LoggerFactory.getLogger(Test.class);
	private SwingRFBDisplay display;
	private RFBContext context;
	private RFBTransport transport;

	public Test(final Socket socket) {
		super("Remote Desktop");
		context = new RFBContext();
		// context.setUseCopyRect(false);
		context.setJpegQuality(0);
		// context.setPixelFormat(RFBContext.PIXEL_FORMAT_8_BIT);
		 context.setPreferredEncoding(RFBConstants.ENC_TIGHT_PNG);
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
	}

	public void connect() throws IOException, RFBAuthenticationException {
		LOG.info("Connecting to desktop");
		display.getEngine().startRFBProtocol();
	}

	@Override
	public String passwordAuthenticationRequired() {
		LOG.info("Password authentication requested");
		return "flipper";
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
	public void resized(int width, int height) {
		LOG.info(String.format("Desktop resized to %d x %d", width, height));
		display.setPreferredSize(new Dimension(width, height));
		pack();
	}

	@Override
	public void encodingChanged(RFBEncoding currentEncoding) {
	}

	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		new SwingRFBToolkit();
		Socket s = new Socket(args[0], 5900);
		Test ad = new Test(s);
		ad.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		ad.setVisible(true);
		ad.connect();
	}
}
