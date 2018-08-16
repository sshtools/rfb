/**
 * RFB - Remote Frame Buffer (VNC) implementation.
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
package com.sshtools.rfb;

import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageProducer;

import com.sshtools.rfb.RFBToolkit.RFBCursor;

public class DummyDisplay implements RFBDisplay<Component, KeyEvent> {

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
	public void initialiseSession(RFBTransport transport, RFBContext context, RFBEventHandler prompt) {
		context.resetEncodings();
		displayModel = new RFBDisplayModel(this);
		displayModel.setContext(context);
		engine = new ProtocolEngine(this, transport, context, prompt, displayModel,
				RFBToolkit.get().loadImage("/images/empty-cursor.png"),
				RFBToolkit.get().loadImage("/images/dot-cursor.png"));
		engine.setStopCursor(RFBToolkit.get().loadImage("/images/stop-cursor.png"));

	}

	@Override
	public void requestRepaint(int tm, int x, int y, int w, int h) {
	}

	@Override
	public void resizeComponent() {
	}

	@Override
	public void setCursor(RFBCursor defaultCursor) {
	}

	@Override
	public int[] getDisplayComponentSize() {
		return new int[] { displayModel.getRfbWidth(), displayModel.getRfbHeight() };
	}

	@Override
	public void setUpdateRect(RFBRectangle updateRect) {
	}

}