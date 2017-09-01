package com.sshtools.rfbplayer;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.peer.FramePeer;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JSlider;

import com.sshtools.rfb.RFBAuthenticationException;
import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfb.RFBEventHandler;
import com.sshtools.rfb.swing.SwingRFBDisplay;

@SuppressWarnings("serial")
public class RFBPlayer extends JFrame implements RFBEventHandler {

	private SwingRFBDisplay display;
	private JSlider slider;

	public RFBPlayer() {
		display = new SwingRFBDisplay();
		getContentPane().setLayout(new BorderLayout());
		slider = new JSlider(0, 100);
		getContentPane().add(slider, BorderLayout.SOUTH);
		getContentPane().add(display, BorderLayout.CENTER);
		setSize(800, 600);
		setVisible(true);
	}

	public void open(File file) throws IOException, RFBAuthenticationException {
		slider.setMaximum((int) (file.length() / 1024));
		RecordedTransport tr = new RecordedTransport(file);
		RFBContext ctx = tr.getContext();
		System.out.println("Initialising session");
		display.initialiseSession(tr, ctx, this);
		System.out.println("Starting protocol");
		display.getEngine().startRFBProtocol();
	}

	public static void main(String[] args) throws Exception {
		RFBPlayer p = new RFBPlayer();
		if (args.length > 0) {
			p.open(new File(args[0]));
		}
		p.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}

		});
	}

	public String passwordAuthenticationRequired() {
		return null;
	}

	public void connected() {
		System.out.println("Connected");
	}

	public void disconnected() {
		System.out.println("Disconnected");
	}

	public void remoteResize(int width, int height) {
		System.out.println("Resized " + width + "x" + height);
		Insets i = ((FramePeer) getPeer()).getInsets();
		width -= i.left + i.right;
		height -= i.top + i.bottom;
		setSize(width, height);
	}

	public void encodingChanged(RFBEncoding currentEncoding) {
		System.out.println("Encoding changed to " + currentEncoding);
	}
}
