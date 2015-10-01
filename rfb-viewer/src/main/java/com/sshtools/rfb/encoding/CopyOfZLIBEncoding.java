package com.sshtools.rfb.encoding;

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBDisplayModel;
import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbcommon.RFBConstants;

public class CopyOfZLIBEncoding extends AbstractRawEncoding {
	private Inflater inflater;

	protected ProtocolReader in;
	protected byte[] buffer;

	public CopyOfZLIBEncoding() {
	}

	public int getType() {
		return RFBConstants.ENC_ZLIB;
	}

	public boolean isPseudoEncoding() {
		return false;
	}

	public void processEncodedRect(RFBDisplay display, int x, int y, int width,
			int height, int encodingType) throws IOException {
		ProtocolEngine engine = display.getEngine();
		RFBDisplayModel model = display.getDisplayModel();
		in = engine.getInputStream();
		decompress(in.readInt(), width * height * model.getBytesPerPixel());
		doProcessRaw(display, x, y, width, height, buffer);
	}

	protected void decompress(int length, int unpackedLength)
			throws IOException {
		int reservedLength = length + unpackedLength;
		if (buffer == null || reservedLength < buffer.length) {
			buffer = new byte[reservedLength];
		}
		in.readFully(buffer, 0, length);
		if (inflater == null) {
			inflater = new Inflater();
		}
		inflater.setInput(buffer, 0, length);
		try {
			inflater.inflate(buffer, length, unpackedLength);
		} catch (DataFormatException e) {
			throw new IOException("Cannot inflate.", e);
		}
	}

	public String getName() {
		return "ZLIB";
	}

}
