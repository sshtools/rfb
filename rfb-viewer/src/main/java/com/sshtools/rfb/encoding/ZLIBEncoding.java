package com.sshtools.rfb.encoding;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBDisplayModel;

public class ZLIBEncoding extends AbstractRawEncoding {
	private Inflater inflater;
	private byte[] buffer;
	private int bufferLength = 0;

	public ZLIBEncoding() {
	}

	@Override
	public int getType() {
		return 6;
	}

	@Override
	public boolean isPseudoEncoding() {
		return false;
	}

	@Override
	public void processEncodedRect(RFBDisplay display, int x, int y, int width,
			int height, int encodingType) throws IOException {
		try {
			ProtocolEngine engine = display.getEngine();
			RFBDisplayModel model = display.getDisplayModel();
			DataInputStream in = engine.getInputStream();
			int length = in.readInt();
			if (buffer == null || length > bufferLength) {
				bufferLength = length;
				buffer = new byte[bufferLength];
			}
			in.readFully(buffer, 0, length);
			if (inflater == null) {
				inflater = new Inflater();
			}
			inflater.setInput(buffer, 0, length);


			int bufspace = ( width * height * model.getBytesPerPixel() ) + length;
			byte[] buf = new byte[bufspace];
			inflater.inflate(buf);
			doProcessRaw(display, x, y, width, height, buf);
		} catch (DataFormatException ex) {
			ex.printStackTrace();
			throw new IOException(ex.getMessage());
		}
	}

	@Override
	public String getName() {
		return "ZLIB";
	}

}
