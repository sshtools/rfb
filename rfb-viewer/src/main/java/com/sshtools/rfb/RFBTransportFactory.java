package com.sshtools.rfb;

import java.io.IOException;

public interface RFBTransportFactory {

  public RFBTransport connect(String hostname, int port, String command) throws
      IOException;

  public void disconnect(RFBTransport transport, String command);

}