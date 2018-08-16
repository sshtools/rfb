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
package com.sshtools.rfb.encoding;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBDisplayModel;
import com.sshtools.rfbcommon.RFBConstants;

public class ZLIBEncoding extends AbstractRawEncoding {
	private Inflater inflater;
	private byte[] buffer;
	private int bufferLength = 0;

	public ZLIBEncoding() {
	}

	@Override
	public int getType() {
		return RFBConstants.ENC_ZLIB;
	}

	@Override
	public boolean isPseudoEncoding() {
		return false;
	}

	@Override
	public void processEncodedRect(RFBDisplay<?,?> display, int x, int y, int width,
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
