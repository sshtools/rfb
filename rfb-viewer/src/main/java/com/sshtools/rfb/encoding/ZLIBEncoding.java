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

	public int getType() {
		return 6;
	}

	public boolean isPseudoEncoding() {
		return false;
	}

	public void processEncodedRect(RFBDisplay display, int x, int y, int width,
			int height, int encodingType) throws IOException {
		try {
			ProtocolEngine engine = display.getEngine();
			RFBDisplayModel model = display.getDisplayModel();
			int bytesPerPixel = model.getBitsPerPixel() / 8;
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
			int decomp = inflater.inflate(buf);
			doProcessRaw(display, x, y, width, height, buf);

			// byte[] buf = new byte[width * bytesPerPixel];
			// for (int i = y; i < y + height; i++) {
			// inflater.inflate(buf);
			// doProcessRaw(display, x, i, width, 1, buf);
			// }

			// model.transferUpdatedRect(x, y, width, height);
			// display.requestRepaint(display.getContext().getScreenUpdateTimeout(),
			// x, y, width, height);
		} catch (DataFormatException ex) {
			ex.printStackTrace();
			throw new IOException(ex.getMessage());
		}
	}

	public String getName() {
		return "ZLIB";
	}

}
