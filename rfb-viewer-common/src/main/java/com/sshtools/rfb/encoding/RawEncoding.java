package com.sshtools.rfb.encoding;

import java.io.DataInputStream;
import java.io.IOException;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBDisplayModel;

public class RawEncoding extends AbstractRawEncoding {

	public RawEncoding() {
	}

	@Override
	public int getType() {
		return 0;
	}

	@Override
	public boolean isPseudoEncoding() {
		return false;
	}

	@Override
	public void processEncodedRect(RFBDisplay display, int x, int y, int width, int height, int encodingType) throws IOException {
		ProtocolEngine engine = display.getEngine();
		RFBDisplayModel model = display.getDisplayModel();
		DataInputStream in = engine.getInputStream();
		int bpp = model.getBitsPerPixel();
		int bytes = (bpp / 8);
		byte[] buf = new byte[width * bytes * height];
		in.readFully(buf);
		doProcessRaw(display, x, y, width, height, buf);
		display.requestRepaint(display.getContext().getScreenUpdateTimeout(), x, y, width, height);
	}

	@Override
	public String getName() {
		return "Raw";
	}
}
