package com.sshtools.rfbserver.encodings;

public abstract class AbstractEncoding implements RFBServerEncoding {

    public int getCode() {
        return getType().getCode();
    }
}
