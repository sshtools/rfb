package com.sshtools.rfbrecorder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import com.sshtools.rfb.DummyDisplay;
import com.sshtools.rfb.RFBAuthenticationException;
import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfb.RFBEventHandler;
import com.sshtools.rfb.RFBSocketTransport;
import com.sshtools.rfb.RFBTransport;

public class RFBRecorder implements RFBEventHandler {
	private RFBTransport transport;
	private RFBContext context;
	private RFBDisplay display;

	public RFBRecorder(RFBContext context, RFBTransport transport, File outFile)
			throws IOException {
		this.context = context;
		this.transport = new RecordingTransport(context, outFile, transport);
		display = new DummyDisplay(context);
	}

	public void start() throws IOException, RFBAuthenticationException {
		System.out.println("Initialising session");
		display.initialiseSession(transport, context, this);
		System.out.println("Starting protocol");
		display.getEngine().startRFBProtocol();
	}

	public static void main(String[] args) throws Exception {
		RFBContext context = new RFBContext();
		RFBTransport transport = new RFBSocketTransport(args[0],
				Integer.valueOf(args[1]));
		RFBRecorder recorder = new RFBRecorder(context, transport, new File(
				args[2]));
		recorder.start();
	}

	public void connected() {
		System.out.println("Connected");
	}

	public void disconnected() {
		System.out.println("Disconnected");
	}

	public void encodingChanged(RFBEncoding encoding) {
		System.out.println("Encoding changed to " + encoding);
	}

	public String passwordAuthenticationRequired() {
		System.out.print("Password: ");
		try {
			return new BufferedReader(new InputStreamReader(System.in))
					.readLine();
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	public void resized(int width, int height) {
		System.out.println("Display resized to " + width + "x" + height);
	}
}
