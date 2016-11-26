/* HEADER */
package com.sshtools.rfb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface RFBProtocolTransport extends RFBTransport {

	@Override
	InputStream getInputStream() throws IOException;

	@Override
	OutputStream getOutputStream() throws IOException;

}