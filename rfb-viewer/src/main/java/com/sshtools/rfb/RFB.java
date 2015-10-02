package com.sshtools.rfb;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.sshtools.rfb.swing.SwingRFBDisplay;

public class RFB {

	public static void main(String[] args) throws IOException, RFBAuthenticationException {
		final JFrame f = new JFrame("__AVOID__");
		final SwingRFBDisplay swingRFBDisplay = new SwingRFBDisplay();
		f.getContentPane().add(swingRFBDisplay);
		f.pack();
		f.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent evt) {
				System.exit(0);
			}
		});
		f.setVisible(true);
		f.setTitle("__AVOID__");
		RFBContext context = new RFBContext();
//		context.setPixelFormat(RFBContext.PIXEL_FORMAT_8_BIT_INDEXED);
		context.setViewOnly(true);
		context.setUseCopyRect(true);

		// TODO when true and connecting to localhost, cursor update loops will happen
		context.setLocalCursorDisplayed(false);
		context.setCursorUpdatesRequested(true);

		// Doesn't work
		// context.setPixelFormat(RFBContext.PIXEL_FORMAT_8_BIT);
		context.setCursorUpdatesIgnored(false);
		context.setPreferredEncoding(RFBContext.ENCODING_HEXTILE);
		swingRFBDisplay.initialiseSession(new RFBSocketTransport("localhost", 6900), context, new RFBEventHandler() {

			@Override
			public void resized(final int width, final int height) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						Dimension dimension = new Dimension(width, height);
						swingRFBDisplay.setPreferredSize(dimension);
						f.pack();

					}
				});
			}

			@Override
			public String passwordAuthenticationRequired() {
				return JOptionPane.showInputDialog("Password");
			}

			@Override
			public void encodingChanged(RFBEncoding currentEncoding) {
				// TODO Auto-generated method stub

			}

			@Override
			public void disconnected() {
				// TODO Auto-generated method stub

			}

			@Override
			public void connected() {
				// TODO Auto-generated method stub

			}
		});
		swingRFBDisplay.getEngine().startRFBProtocol();
	}
}
