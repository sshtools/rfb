/**
 * 
 */
package com.sshtools.rfb;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageProducer;

import javax.swing.ImageIcon;

public class DummyDisplay implements RFBDisplay {

	private RFBDisplayModel displayModel;
	private ProtocolEngine engine;
	private RFBContext context;
	
	public DummyDisplay(RFBContext context) {
		this.context = context;
	}

	public Image createBufferImage(int width, int height) {
		return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	}

	public Image createImage(ImageProducer src) {
		return Toolkit.getDefaultToolkit().createImage(src);
	}

	@Override
	public RFBContext getContext() {
		return context;
	}

	@Override
	public Component getDisplayComponent() {
		return null;
	}

	@Override
	public RFBDisplayModel getDisplayModel() {
		return displayModel;
	}

	@Override
	public ProtocolEngine getEngine() {
		return engine;
	}

	@Override
	public boolean handleKeyEvent(KeyEvent evt) {
		return false;
	}

	@Override
	public void initialiseSession(RFBTransport transport,
			RFBContext context, RFBEventHandler prompt) {
		context.resetEncodings();
		displayModel = new RFBDisplayModel(this);
		displayModel.setContext(context);
		engine = new ProtocolEngine(this, transport, context, prompt,
			displayModel,

			new ImageIcon(this.getClass().getResource("/images/empty-cursor.png"))
				.getImage(), new ImageIcon(this.getClass().getResource("/images/dot-cursor.png")).getImage());
		engine.setStopCursor(new ImageIcon(this.getClass().getResource("/images/stop-cursor.png")).getImage());
		
	}

	@Override
	public void requestRepaint(int tm, int x, int y, int w, int h) {
	}

	@Override
	public void resizeComponent() {
	}

	@Override
	public void setUpdateRect(Rectangle updateRect) {
	}

	@Override
	public boolean imageUpdate(Image img, int infoflags, int x, int y,
			int width, int height) {
		return false;
	}

	@Override
	public void setCursor(Cursor defaultCursor) {
	}
	
}