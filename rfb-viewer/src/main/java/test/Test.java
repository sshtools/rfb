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

import com.sshtools.profile.AuthenticationException;
import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfb.RFBEventHandler;
import com.sshtools.rfb.RFBTransport;
import com.sshtools.rfb.swing.SwingRFBDisplay;
import com.sshtools.rfbcommon.RFBConstants;

@SuppressWarnings("serial")
public class Test extends JFrame implements RFBEventHandler {

	private SwingRFBDisplay display;
	private RFBContext context;
	private RFBTransport transport;

	public Test(final Socket socket) {
		super("Remote Desktop");

		context = new RFBContext();
//		context.setUseCopyRect(false);
		context.setJpegQuality(0);
//		context.setPixelFormat(RFBContext.PIXEL_FORMAT_8_BIT);
//		context.setPreferredEncoding(RFBConstants.ENC_ZRLE);
		transport = new RFBTransport() {

			public int getPort() {
				return socket.getLocalPort();
			}

			public OutputStream getOutputStream() throws IOException {
				return socket.getOutputStream();
			}

			public InputStream getInputStream() throws IOException {
				return socket.getInputStream();
			}

			public String getHostname() {
				return socket.getLocalAddress().getHostAddress();
			}

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

	public void connect() throws IOException, AuthenticationException {
		System.out.println("Connecting to desktop");
		display.getEngine().startRFBProtocol();
	}

	public String passwordAuthenticationRequired() {
		System.out.println("Password authentication requested");
		return "flipper";
	}

	public void connected() {
		System.out.println("Connected to desktop");
	}

	public void disconnected() {
		System.out.println("Disconnected from desktop");
	}

	public void resized(int width, int height) {
		System.out.println("Desktop resized to " + width + " x " + height);
		display.setPreferredSize(new Dimension(width, height));
		pack();
	}

	public void encodingChanged(RFBEncoding currentEncoding) {
	}

	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		Socket s = new Socket("localhost", 5900);
//		Socket s = new Socket("smallblue", 5900);
//		Socket s = new Socket("192.168.91.145", 5900);
//		Socket s = new Socket("192.168.91.141", 5900);
//		Socket s = new Socket("192.168.91.139", 5900);
//		Socket s = new Socket("192.168.91.142", 5900);
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
