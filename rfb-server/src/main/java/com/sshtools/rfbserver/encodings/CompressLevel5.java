package com.sshtools.rfbserver.encodings;


public class CompressLevel5 extends CompressLevel0 {

    public int getCode() {
        return getType().getCode() + 5;
    }

}
