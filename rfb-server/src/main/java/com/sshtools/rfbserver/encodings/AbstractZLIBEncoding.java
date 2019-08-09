/**
 * RFB Server - Remote Frame Buffer (VNC Server) implementation. This is the base module if you want to create a VNC server. It takes a layered driver approach to add native specific features (which is recommened as the cross-platform default "Robot" driver is very slow).
 *
 * See the vncserver module for a concrete server implementation that has some native performance improvements for some platforms.
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
package com.sshtools.rfbserver.encodings;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZStream;
import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public abstract class AbstractZLIBEncoding extends AbstractRawEncoding<BufferedImage> {
	private int zlibCompressLevel = 5;
	private ZStream deflater;

	public AbstractZLIBEncoding() {
	}

	public int getLevel() {
		return zlibCompressLevel;
	}

	public void setZLibCompressLevel(int zlibCompressLevel) {
		this.zlibCompressLevel = zlibCompressLevel;
	}

	public boolean isPseudoEncoding() {
		return false;
	}

	public synchronized void encode(UpdateRectangle<BufferedImage> update, ProtocolWriter dout, PixelFormat pixelFormat,
			RFBClient client) throws IOException {
		boolean useJZLib = true;
		if (useJZLib) {
			jzlibEncode(update, dout, pixelFormat, client);
			return;
		}
		// VVV Doesn't work.. What is different between this an jzlib?
		@SuppressWarnings("unchecked")
		byte[] uncompressed = prepareEncode((UpdateRectangle<BufferedImage>) update, pixelFormat);
		int uncompressedLen = uncompressed.length;
		Deflater deflater = new Deflater(zlibCompressLevel, false);
		deflater.setInput(uncompressed);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(uncompressedLen);
		deflater.finish();
		byte[] buffer = new byte[1024];
		while (!deflater.finished()) {
			int count = deflater.deflate(buffer); // returns the generated
			outputStream.write(buffer, 0, count);
		}
		outputStream.flush();
		outputStream.close();
		deflater.end();
		byte[] data = outputStream.toByteArray();
		dout.writeUInt32(getType().getCode());
		dout.writeUInt32(data.length);
		dout.write(data);
		dout.flush();
	}

	public synchronized void jzlibEncode(UpdateRectangle<?> update, ProtocolWriter dout, PixelFormat pixelFormat,
			RFBClient client) throws IOException {
		byte[] uncompressed = prepareEncode((UpdateRectangle<BufferedImage>) update, pixelFormat);
		ByteArrayOutputStream compressOut = compress(uncompressed);
		// Write the actual message
		dout.writeUInt32(getType().getCode());
		byte[] out = compressOut.toByteArray();
		int length = out.length;
		dout.writeUInt32(length);
		dout.write(out);
	}

	protected ByteArrayOutputStream compress(byte[] uncompressed) throws IOException {
		int uncompressedLen = uncompressed.length;
		int maxCompressed = uncompressed.length + ((uncompressed.length + 99) / 100) + 12;
		byte[] tmpbuf = new byte[maxCompressed];
		if (deflater == null) {
			deflater = new ZStream();
			deflater.deflateInit(zlibCompressLevel);
		}
		ByteArrayOutputStream compressOut = new ByteArrayOutputStream(maxCompressed);
		deflater.next_in = uncompressed;
		deflater.next_in_index = 0;
		deflater.avail_in = uncompressedLen;
		int status;
		do {
			deflater.next_out = tmpbuf;
			deflater.next_out_index = 0;
			deflater.avail_out = maxCompressed;
			status = deflater.deflate(JZlib.Z_PARTIAL_FLUSH);
			switch (status) {
			case JZlib.Z_OK:
				compressOut.write(tmpbuf, 0, maxCompressed - deflater.avail_out);
				break;
			default:
				throw new IOException("compress: deflate returnd " + status);
			}
		} while (deflater.avail_out == 0);
		return compressOut;
	}
}
