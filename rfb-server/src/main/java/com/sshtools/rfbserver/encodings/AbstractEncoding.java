package com.sshtools.rfbserver.encodings;

public abstract class AbstractEncoding<D> implements RFBServerEncoding<D> {

    public int getCode() {
        return getType().getCode();
    }
}
