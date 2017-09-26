package com.sshtools.rfb.files;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfb.RFBFS;
import com.sshtools.rfbcommon.DefaultRFBFile;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.RFBFile;

public class UltraVNCFS implements RFBFS {
	private static final int RFB_MAX_PATH = 255;
	private ProtocolEngine protocolEngine;
	private Object wait = new Object();
	private Semaphore sem = new Semaphore(1);
	private boolean readingDirectory;
	private String cachedPath;
	// Requests
	private final static int RFB_DIR_CONTENT_REQUEST = 1;
	private final static int RFB_COMMAND = 10;
	// Request parameters
	private final static int RFB_DIR_CONTENT = 1;
	private final static int RFB_DIR_DRIVE_LIST = 2;
	// Command parameters
	private final static int RFB_DIR_CREATE = 1;
	// Received
	private final static int RFB_RECV_NONE = 0;
	private final static int RFB_DIR_PACKET = 2;
	private final static int RFB_RECV_DIRECTORY = 1;
	private final static int RFB_RECV_DRIVE_LIST = 3;
	
	private List<RFBFile> files = new ArrayList<RFBFile>();
	private Map<String, RFBFile> fileMap = new HashMap<String, RFBFile>();
	private Object received;

	public UltraVNCFS(ProtocolEngine protocolEngine) {
		this.protocolEngine = protocolEngine;
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public boolean mkdir(String filename) throws IOException {
		try {
			sem.acquire();
			writeDirRequest(RFB_COMMAND, RFB_DIR_CREATE, filename);
			sem.acquire();
			sem.release();
			return ((Boolean) received).booleanValue();
		} catch (InterruptedException ie) {
			InterruptedIOException interruptedIOException = new InterruptedIOException(
					"Interuppted waiting for reply.");
			interruptedIOException.initCause(ie);
			throw interruptedIOException;
		}
	}

	@Override
	public synchronized RFBFile[] list(String filename) throws IOException {
		try {
			sem.acquire();
			writeDirRequest(RFB_DIR_CONTENT_REQUEST, RFB_DIR_CONTENT, addTrailingSlash(filename));
			sem.acquire();
			sem.release();
			return (RFBFile[]) received;
		} catch (InterruptedException ie) {
			InterruptedIOException interruptedIOException = new InterruptedIOException(
					"Interuppted waiting for reply.");
			interruptedIOException.initCause(ie);
			throw interruptedIOException;
		}
	}

	private String addTrailingSlash(String filename) {
		if (!filename.endsWith("\\")) {
			filename += "\\";
		}
		return filename;
	}

	@Override
	public synchronized RFBFile stat(String filename) throws IOException {
		String path = convertFilename(filename);
		while (path.endsWith("\\") && path.length() > 1) {
			path = filename.substring(0, path.length() - 1);
		}
		int lidx = path.lastIndexOf("\\");
		if (lidx == -1) {
			throw new IOException("Not a file path");
		} else {
			String parentPath = addTrailingSlash(filename.substring(0, lidx));
			String basename = filename.substring(lidx + 1);
			// If the last path read was this path, then get the file from
			// the cache, otherwise read it
			if (!parentPath.equals(cachedPath) || !fileMap.containsKey(basename)) {
				for(RFBFile f : list(parentPath)) 
					fileMap.put(f.getName(), f);
			}
			return (RFBFile) fileMap.get(basename);
		}
	}

	// public synchronized RFBDrive[] listDrives() throws IOException {
	// writeDirRequest(RFB_DIR_CONTENT_REQUEST, RFB_DIR_DRIVE_LIST, null);
	// return (RFBDrive[]) waitTillReceived();
	// }

	public static final void writeInt(OutputStream out, int v) throws IOException {
		out.write((v & 0xFF000000) >>> 24);
		out.write((v & 0xFF0000) >>> 16);
		out.write((v & 0xFF00) >>> 8);
		out.write((v & 0xFF) >>> 0);
	}

	private String[] readTerminatedString(DataInputStream din) throws IOException {
		int length = din.readInt();
		byte[] arr = new byte[length];
		din.readFully(arr);
		List strings = new ArrayList();
		StringBuffer buf = new StringBuffer();
		char ch = ' ';
		for (int i = 0; i < arr.length; i++) {
			ch = (char) arr[i];
			if (ch == '\0') {
				strings.add(buf.toString());
				buf.setLength(0);
			} else {
				buf.append(ch);
			}
		}
		// if (buf.length() > 0 && ch != '') {
		// strings.add(buf.toString());
		// }
		return (String[]) strings.toArray(new String[strings.size()]);
	}

	private void writeDirRequest(int request, int param, String filename) throws IOException {
		DataOutputStream out = protocolEngine.getOutputStream();
		synchronized (out) {
			out.writeByte(RFBConstants.SMSG_FILE_TRANSFER);
			out.writeByte(request);
			out.writeByte(param);
			out.writeByte(0);
			byte[] bytes = convertFilename(filename).getBytes();
			writeInt(out, 0);
			writeInt(out, bytes.length);
			out.write(bytes);
			out.flush();
		}
	}

	private String convertFilename(String filename) {
		if (filename == null) {
			return "";
		}
		if (filename.length() > RFB_MAX_PATH) {
			throw new IllegalArgumentException("Filename too long " + filename);
		}
		if (filename.length() == 0) {
			return "\\";
		}
		return filename.replace('/', '\\') + "\0";
	}

	@Override
	public boolean rm(String processPath) throws IOException {
		return false;
	}

	@Override
	public void mv(String oldName, String newName) throws IOException {
	}

	@Override
	public boolean handleReply(int code) throws IOException {
		DataInputStream inputStream = protocolEngine.getInputStream();
		int type = inputStream.readUnsignedByte();
		int param = inputStream.readUnsignedByte();
		int contentParam = param;
		param = inputStream.readUnsignedByte();
		param = param << 8;
		contentParam = contentParam | param;
		if (type == RFB_DIR_DRIVE_LIST || type == RFB_DIR_PACKET) {
			readDirectory(contentParam);
		} else {
			return false;
//			throw new IOException("Was expecting a different file system result");
		}
		return true;
	}

	@Override
	public OutputStream send(String path, boolean overwrite, long offset) {
		// TODO Auto-generated method stub
		return null;
	}

	// private void readDriveList(int contentParam) throws IOException {
	// if (contentParam == RFB_RECV_DRIVE_LIST) {
	// DataInputStream din = protocolEngine.getInputStream();
	// din.readFully(new byte[4]);
	// String[] driveList = readTerminatedString(din);
	// List drives = new ArrayList();
	// for (int i = 0; i < driveList.length; i++) {
	// // There seems to be a blank drive returned from x11vnc?
	// if (!driveList[i].equals("")) {
	// drives.add(new RFBDrive(driveList[i]));
	// }
	// }
	// received((RFBDrive[]) drives.toArray(new RFBDrive[drives.size()]));
	// } else {
	// throw new IOException("Unknown drive list content param");
	// }
	// }

	private void received(Object value) {
		received = value;
		sem.release();
	}

	private void readDirectory(int contentParam) throws IOException {
		switch (contentParam) {
		// case RFB_RECV_DRIVE_LIST:
		// readDriveList(contentParam);
		// break;
		case RFB_RECV_DIRECTORY:
			if (readingDirectory) {
				readDirectoryListContent();
			} else {
				readingDirectory = startReadingDirectory();
			}
			break;
		case RFB_RECV_NONE:
			endReadingDirectory();
			readingDirectory = false;
			break;
		default:
			throw new IllegalArgumentException("Invalid operation for directory listing");
		}
	}

	private void readDirectoryListContent() throws IOException {
		DataInputStream in = protocolEngine.getInputStream();
		String fileName = "", alternateFileName = "";
		char cFileName, cAlternateFileName;
		in.readInt();
		// Read all into memory first
		int length = in.readInt();
		byte[] buf = new byte[length];
		in.readFully(buf);
		ByteArrayInputStream in2 = new ByteArrayInputStream(buf);
		in = new DataInputStream(in2);
		// Parse the buffer
		int fileAttributes = in.readInt();
		long creationTime = in.readLong();
		long lastAccessTime = in.readLong();
		long lastWriteTime = in.readLong();
		long fileSizeHigh = in.readInt();
		long fileSizeLow = in.readInt();
		long reserved = in.readLong();
		cFileName = (char) in.readUnsignedByte();
		while (cFileName != '\0') {
			fileName += cFileName;
			cFileName = (char) in.readUnsignedByte();
		}
		cAlternateFileName = (char) in.readByte();
		while (cAlternateFileName > 0 && in2.available() > 0) {
			alternateFileName += cAlternateFileName;
			cAlternateFileName = (char) in.readUnsignedByte();
		}
		boolean folder = false;
		if ((fileAttributes & 0x10000000) == 0x10000000) {
			folder = true;
		}
		RFBFile file = new DefaultRFBFile(folder, fileSizeHigh, fileName, fileAttributes, creationTime, lastAccessTime,
				lastWriteTime);
		files.add(file);
		fileMap.put(fileName, file);
	}

	private void endReadingDirectory() throws IOException {
		DataInputStream in = protocolEngine.getInputStream();
		in.readInt();
		int length = in.readInt();
		received(files.toArray(new RFBFile[files.size()]));
	}

	private boolean startReadingDirectory() throws IOException {
		files.clear();
		fileMap.clear();
		DataInputStream in = protocolEngine.getInputStream();
		in.readInt();
		int len = in.readInt();
		if (len == 0) {
			received(files.toArray(new RFBFile[files.size()]));
			return false;
		}
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < len; i++) {
			char ch = (char) in.readUnsignedByte();
			if (ch != '\0') {
				buf.append(ch);
			}
		}
		cachedPath = addTrailingSlash(buf.toString());
		return true;
	}

	interface RFBFileSystemMessage {
		Object run(int contentParam) throws IOException;
	}

	@Override
	public InputStream receive(String processPath, long filePointer) throws IOException {
		throw new UnsupportedOperationException();
	}
}
