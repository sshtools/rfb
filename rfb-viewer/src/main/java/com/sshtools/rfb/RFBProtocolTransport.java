/* HEADER */
package com.sshtools.rfb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.virtualsession.VirtualSessionTransport;

public interface RFBProtocolTransport extends RFBTransport, VirtualSessionTransport {
  
  public InputStream getInputStream() throws IOException;
  public OutputStream getOutputStream() throws IOException;

}