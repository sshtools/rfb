package test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.BasicConfigurator;

import com.sshtools.rfb.RFBAuthenticationException;
import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfb.RFBEventHandler;
import com.sshtools.rfb.RFBTransport;
import com.sshtools.rfb.swing.JavaFXRFBDisplay;
import com.sshtools.rfb.swing.JavaFXRFBToolkit;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

@SuppressWarnings("serial")
public class Test extends Application implements RFBEventHandler {

	private JavaFXRFBDisplay display;
	private RFBContext context;
	private RFBTransport transport;
	private Stage stage;

	public static void main(String[] args) {
		BasicConfigurator.configure();
		new JavaFXRFBToolkit();
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		this.stage = primaryStage;

		Socket s;
		try {
			s = new Socket("blue", 5900);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		// Socket s = new Socket("smallblue", 5900);
		// Socket s = new Socket("192.168.91.145", 5900);
		// Socket s = new Socket("192.168.91.141", 5900);
		// Socket s = new Socket("192.168.91.139", 5900);
		// Socket s = new Socket("192.168.91.142", 5900);

		primaryStage.setTitle("JavaFX VNC test");
		AnchorPane root = new AnchorPane();

		context = new RFBContext();
		// context.setUseCopyRect(false);
		context.setJpegQuality(0);
		// context.setPixelFormat(RFBContext.PIXEL_FORMAT_8_BIT);
		// context.setPreferredEncoding(RFBConstants.ENC_ZRLE);
		transport = new RFBTransport() {

			@Override
			public int getPort() {
				return s.getLocalPort();
			}

			@Override
			public OutputStream getOutputStream() throws IOException {
				return s.getOutputStream();
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return s.getInputStream();
			}

			@Override
			public String getHostname() {
				return s.getLocalAddress().getHostAddress();
			}

			@Override
			public void close() throws IOException {
				s.close();
			}
		};

		display = new JavaFXRFBDisplay();
		display.initialiseSession(transport, context, this);
		root.getChildren().add(display.getDisplayComponent());

		primaryStage.setScene(new Scene(root));
		primaryStage.show();
	}

	public void connect() throws IOException, RFBAuthenticationException {
		System.out.println("Connecting to desktop");
		display.getEngine().startRFBProtocol();
	}

	@Override
	public String passwordAuthenticationRequired() {
		System.out.println("Password authentication requested");
		return "flipper";
	}

	@Override
	public void connected() {
		System.out.println("Connected to desktop");
	}

	@Override
	public void disconnected() {
		System.out.println("Disconnected from desktop");
	}

	@Override
	public void resized(int width, int height) {
		System.out.println("Desktop resized to " + width + " x " + height);
		display.getCanvas().setWidth(width);
		display.getCanvas().setHeight(height);
		stage.sizeToScene();
	}

	@Override
	public void encodingChanged(RFBEncoding currentEncoding) {
	}

}
