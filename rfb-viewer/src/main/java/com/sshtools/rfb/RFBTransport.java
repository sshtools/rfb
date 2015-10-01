/* HEADER */
package com.sshtools.rfb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Defines the attributes of a transport for the RFB protocol. Essentially
 * this allows RFB to run over any type of transport that can provide an
 * InputStream and OutputStream.
 *
 * @author Lee David Painter
 */
public interface RFBTransport {

  /**
       * Get the hostname of the remote computer. This is for informational purposes
   * only.
   * @return
   */
  public String getHostname();

  /**
   * The InputStream for reading RFB data.
   * @return
   * @throws IOException
   */
  public InputStream getInputStream() throws IOException;

  /**
   * The OutputStream to write RFB data.
   * @return
   * @throws IOException
   */
  public OutputStream getOutputStream() throws IOException;

  /**
   * Close the connection and terminate the RFB protocol.
   * @throws IOException
   */
  public void close() throws IOException;


  public int getPort();

}