package com.sshtools.rfb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.rfbcommon.RFBFile;

public interface RFBFS {

	boolean isActive();

	boolean mkdir(String filename) throws IOException;

	RFBFile[] list(String filename) throws IOException;

	boolean rm(String processPath) throws IOException;

	RFBFile stat(String filename) throws IOException;

	boolean handleReply(int type) throws IOException;

	void mv(String oldName, String newName) throws IOException;

	OutputStream send(String path, boolean overwrite, long offset) throws IOException;

	InputStream receive(String processPath, long filePointer) throws IOException;
}
