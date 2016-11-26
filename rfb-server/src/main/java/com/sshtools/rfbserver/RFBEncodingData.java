package com.sshtools.rfbserver;

/**
 * Drivers may attach this to damage and window events, and it will be passed on
 * to the protocol encoders. They may use this to get contextual information
 * from the driver (if any). This was first introduced to support the CopyRect
 * driver.
 */
public interface RFBEncodingData {

}
