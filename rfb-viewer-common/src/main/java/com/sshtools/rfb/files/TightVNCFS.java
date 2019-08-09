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
package com.sshtools.rfb.files;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBFS;
import com.sshtools.rfb.SecurityType;
import com.sshtools.rfb.auth.Tight;
import com.sshtools.rfbcommon.DefaultRFBFile;
import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.RFBFile;
import com.sshtools.rfbcommon.TightCapability;

/**
 * RFB file transfer implementation that is compatible with TightVNC file
 * transfer.
 */
public class TightVNCFS implements RFBFS {
	private ProtocolEngine protocolEngine;

	private final static TightCapability[] REQUIRED_SERVER_CAPS = new TightCapability[] {
			RFBConstants.CAP_FTSCSRLY, RFBConstants.CAP_FTSFLRLY,
			RFBConstants.CAP_FTSM5RLY, RFBConstants.CAP_FTSFURLY,
			RFBConstants.CAP_FTSUDRLY, RFBConstants.CAP_FTSUERLY,
			RFBConstants.CAP_FTSFDRLY, RFBConstants.CAP_FTSDDRLY,
			RFBConstants.CAP_FTSDERLY, RFBConstants.CAP_FTSMDRLY,
			RFBConstants.CAP_FTSFTRLY, RFBConstants.CAP_FTSFMRLY,
			RFBConstants.CAP_FTSDSRLY, RFBConstants.CAP_FTLRFRLY };

	private final static TightCapability[] REQUIRED_CLIENT_CAPS = new TightCapability[] {
			RFBConstants.CAP_FTCCSRST, RFBConstants.CAP_FTCFLRST,
			RFBConstants.CAP_FTCM5RST, RFBConstants.CAP_FTCFURST,
			RFBConstants.CAP_FTCUDRST, RFBConstants.CAP_FTCUERST,
			RFBConstants.CAP_FTCFDRST, RFBConstants.CAP_FTCDDRST,
			RFBConstants.CAP_FTCMDRST, RFBConstants.CAP_FTCFRRST,
			RFBConstants.CAP_FTCFMRST, RFBConstants.CAP_FTCDSRST };

	private BlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(1000, true);

	public TightVNCFS(ProtocolEngine protocolEngine) {
		this.protocolEngine = protocolEngine;
	}

	@Override
	public boolean isActive() {
	    Tight tight = null;
	    for(SecurityType t : protocolEngine.getSecurityTypes()) {
	        if(t instanceof Tight) {
	            tight = (Tight)t;
	            break;
	        }
	    }
	    if(tight == null) {
	        return false;
	    }
	    
		boolean active = true;
		for (int i = 0; i < REQUIRED_SERVER_CAPS.length; i++) {
			if (!tight.getServerCapabilities().contains(REQUIRED_SERVER_CAPS[i])) {
				active = false;
				break;
			}
		}
		for (int i = 0; i < REQUIRED_CLIENT_CAPS.length; i++) {
			if (!tight.getClientCapabilities().contains(REQUIRED_CLIENT_CAPS[i])) {
				active = false;
				break;
			}
		}
		return active;
	}

	@Override
	public synchronized boolean mkdir(String filename) throws IOException {
		ProtocolWriter outputStream = protocolEngine.getOutputStream();
		synchronized (outputStream) {
			outputStream.writeInt(RFBConstants.CAP_FTCMDRST_CODE);
			outputStream.writeTerminatedString(filename);
			outputStream.flush();
		}
		return (Boolean) waitTillReceived();
	}

	@Override
	public boolean rm(String filename) throws IOException {
		ProtocolWriter outputStream = protocolEngine.getOutputStream();
		synchronized (outputStream) {
			outputStream.writeInt(RFBConstants.CAP_FTCFRRST_CODE);
			outputStream.writeTerminatedString(filename);
			outputStream.flush();
		}
		return (Boolean) waitTillReceived();
	}

	@Override
	public void mv(String oldName, String newName) throws IOException {
		ProtocolWriter outputStream = protocolEngine.getOutputStream();
		synchronized (outputStream) {
			outputStream.writeInt(RFBConstants.CAP_FTCFMRST_CODE);
			outputStream.writeTerminatedString(oldName);
			outputStream.writeTerminatedString(newName);
			outputStream.flush();
		}
		waitTillReceived();
	}

	@Override
	public InputStream receive(String path, long offset) throws IOException {
		final ProtocolWriter outputStream = protocolEngine.getOutputStream();
		synchronized (outputStream) {
			outputStream.writeInt(RFBConstants.CAP_FTCFDRST_CODE);
			outputStream.writeTerminatedString(path);
			outputStream.writeLong(offset);
			outputStream.flush();
		}
		waitTillReceived();
		return new InputStream() {
			@Override
			public int read() throws IOException {
				byte[] b = new byte[1];
				int r = read(b);
				return r == -1 ? r : (int) b[0];
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				synchronized (outputStream) {
					outputStream.writeInt(RFBConstants.CAP_FTCDDRST_CODE);
					outputStream.write(1);
					outputStream.writeUInt32(len);
					outputStream.flush();
				}
				Object reply = waitTillReceived();
				if (reply instanceof Block) {
					Block block = (Block) reply;
					System.arraycopy(block.data, 0, b, off,
							block.uncompressedSize);
					return block.uncompressedSize;
				} else {
					// EOF
					return -1;
				}
			}
		};
	}

	@Override
	public OutputStream send(String path, boolean overwrite, long offset)
			throws IOException {
		final ProtocolWriter outputStream = protocolEngine.getOutputStream();
		synchronized (outputStream) {
			outputStream.writeInt(RFBConstants.CAP_FTCFURST_CODE);
			outputStream.writeTerminatedString(path);
			outputStream.write(overwrite ? 0x01 : 0);
			outputStream.writeLong(offset);
			outputStream.flush();
		}
		waitTillReceived();
		return new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				write(new byte[] { (byte) b });
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				synchronized (outputStream) {
					System.err
							.println("WRITE BLOCK OF " + len + " from " + off);
					outputStream.writeInt(RFBConstants.CAP_FTCUDRST_CODE);
					outputStream.write(0);
					outputStream.writeInt(len);
					outputStream.writeInt(len);
					outputStream.write(b, off, len);
					outputStream.flush();
				}
				waitTillReceived();
			}

			@Override
			public void close() throws IOException {
				synchronized (outputStream) {
					outputStream.writeInt(RFBConstants.CAP_FTCUERST_CODE);
					outputStream.writeShort(0);
					outputStream.writeLong(0); // TODO last mod. Do we need to
												// in VFS content
					outputStream.flush();
				}
				waitTillReceived();
			}
		};
	}

	@Override
	public synchronized RFBFile[] list(String filename) throws IOException {
		ProtocolWriter outputStream = protocolEngine.getOutputStream();
		synchronized (outputStream) {
			outputStream.writeInt(RFBConstants.CAP_FTCFLRST_CODE);
			outputStream.write(0);
			outputStream.writeTerminatedString(filename);
			outputStream.flush();
		}
		return (RFBFile[]) waitTillReceived();
	}

	@Override
	public synchronized RFBFile stat(String filename) throws IOException {
		String dir = filename;
		String base = null;
		if (!dir.equals("/")) {
			int idx = dir.lastIndexOf('/');
			base = dir.substring(idx + 1);
			if (idx > 0) {
				dir = dir.substring(0, idx);
			} else {
				dir = "/";
			}
			for (RFBFile l : list(dir)) {
				if (l.getName().equals(base)) {
					return l;
				}
			}
		} else {
			return new DefaultRFBFile(true, 0, "/", 0, 0, 0, 0);
		}
		return null;
	}

	@Override
	public boolean handleReply(int code) throws IOException {
		if (code != RFBConstants.SMSG_TIGHT_FILETRANSFER) {
			return false;
		}
		ProtocolReader inputStream = protocolEngine.getInputStream();
		int op = (code << 24) | (inputStream.read() << 16)
				| (inputStream.read() << 8) | inputStream.read();
		switch (op) {
		case RFBConstants.CAP_FTSFLRLY_CODE:
			readDir();
			break;
		case RFBConstants.CAP_FTSFTRLY_CODE:
		case RFBConstants.CAP_FTSMDRLY_CODE:
		case RFBConstants.CAP_FTSFMRLY_CODE:
		case RFBConstants.CAP_FTSFURLY_CODE:
		case RFBConstants.CAP_FTSUDRLY_CODE:
		case RFBConstants.CAP_FTSUERLY_CODE:
		case RFBConstants.CAP_FTSFDRLY_CODE:
			received(Boolean.TRUE);
			break;
		case RFBConstants.CAP_FTSDSRLY_CODE:
			String message = inputStream.readSizedUTF8();
			received(new IOException(message));
			break;
		case RFBConstants.CAP_FTSDDRLY_CODE:
			receiveBlock();
			break;
		case RFBConstants.CAP_FTSDERLY_CODE:
			endReceive();
			break;
		default:
			throw new IOException(
					"Was expecting a different file system result (got " + op
							+ " " + Integer.toHexString(op) + ")");
		}
		return true;
	}

	private void endReceive() throws IOException {
		DataInputStream inputStream = protocolEngine.getInputStream();
		inputStream.read(); // flags
		received(inputStream.readLong());
	}

	private void receiveBlock() throws IOException {
		DataInputStream inputStream = protocolEngine.getInputStream();
		Block b = new Block();
		b.compress = inputStream.read();
		b.compressedSize = inputStream.readInt();
		b.uncompressedSize = inputStream.readInt();
		b.data = new byte[b.compressedSize];
		inputStream.readFully(b.data);
		received(b);
	}

	private void readDir() throws IOException {
		List<RFBFile> l = new ArrayList<RFBFile>();
		DataInputStream inputStream = protocolEngine.getInputStream();
		inputStream.read(); // compression
		inputStream.readInt(); // compresse  size
		int uncompressedSize = inputStream.readInt();
		byte[] buf = new byte[uncompressedSize];
		inputStream.readFully(buf);

		ByteArrayInputStream bin = new ByteArrayInputStream(buf);
		ProtocolReader pr = new ProtocolReader(bin);
		try {
		int files = pr.readInt();
		for (int i = 0; i < files; i++) {
			long size = pr.readLong();
			long lastMod = pr.readLong();
			short flags = pr.readShort();
			String name = pr.readSizedUTF8();
			l.add(new DefaultRFBFile((flags & 0x1) != 0, size, name, flags, 0,
					0, lastMod));
		}
		}
		finally {
			pr.close();
		}

		// TODO compression
		received(l.toArray(new RFBFile[0]));
	}

	private void received(Object value) {
		try {
			queue.put(value);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private Object waitTillReceived() throws IOException {
		try {
			Object val = queue.take();
			if (val instanceof IOException) {
				throw new IOException((IOException) val);
			}
			return val;
		} catch (InterruptedException ie) {
			throw new InterruptedIOException();
		}

	}

	class Block {
		int compress;
		int compressedSize;
		int uncompressedSize;
		byte[] data;
	}
}
