package com.sshtools.rfbserver.encodings;


public class CompressLevel8 extends CompressLevel0 {

    public int getCode() {
        return getType().getCode() + 8;
    }

}
