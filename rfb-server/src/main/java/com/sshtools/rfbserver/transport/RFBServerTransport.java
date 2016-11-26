package com.sshtools.rfbserver.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface RFBServerTransport {
	InputStream getInputStream() throws IOException;

	OutputStream getOutputStream() throws IOException;

	void stop();

	boolean isDisconnect(IOException e);
}
