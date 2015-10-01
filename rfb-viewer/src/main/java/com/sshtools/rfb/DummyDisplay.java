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

import com.sshtools.ui.swing.ResourceIcon;

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

	public RFBContext getContext() {
		return context;
	}

	public Component getDisplayComponent() {
		return null;
	}

	public RFBDisplayModel getDisplayModel() {
		return displayModel;
	}

	public ProtocolEngine getEngine() {
		return engine;
	}

	public boolean handleKeyEvent(KeyEvent evt) {
		return false;
	}

	public void initialiseSession(RFBTransport transport,
			RFBContext context, RFBEventHandler prompt) {
		context.resetEncodings();
		displayModel = new RFBDisplayModel(this);
		displayModel.setContext(context);
		engine = new ProtocolEngine(this, transport, context, prompt,
			displayModel,

			new ResourceIcon(this.getClass(), "/images/empty-cursor.png")
				.getImage(), new ResourceIcon(this.getClass(),
				"/images/dot-cursor.png").getImage());
		engine.setStopCursor(new ResourceIcon(this.getClass(),
			"/images/stop-cursor.png").getImage());
		
	}

	public void requestRepaint(int tm, int x, int y, int w, int h) {
		// TODO Auto-generated method stub
		
	}

	public void resizeComponent() {
		// TODO Auto-generated method stub
		
	}

	public void setUpdateRect(Rectangle updateRect) {
		// TODO Auto-generated method stub
		
	}

	public boolean imageUpdate(Image img, int infoflags, int x, int y,
			int width, int height) {
		// TODO Auto-generated method stub
		return false;
	}

	public void setCursor(Cursor defaultCursor) {
	}
	
}