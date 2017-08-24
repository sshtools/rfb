package com.sshtools.rfb;

import java.io.IOException;

public interface RFBEncoding {

  public int getType();
  
  public String getName();

  public boolean isPseudoEncoding();

  public void processEncodedRect(RFBDisplay<?,?> display, int x, int y, int width,
                                 int height, int encodingType) throws
      IOException;

}
