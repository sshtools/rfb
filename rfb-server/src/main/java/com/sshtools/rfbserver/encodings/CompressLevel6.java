package com.sshtools.rfbserver.encodings;


public class CompressLevel6 extends CompressLevel0 {

    public int getCode() {
        return getType().getCode() + 6;
    }

}
