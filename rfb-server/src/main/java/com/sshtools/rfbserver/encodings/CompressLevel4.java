package com.sshtools.rfbserver.encodings;


public class CompressLevel4 extends CompressLevel0 {

    public int getCode() {
        return getType().getCode() + 4;
    }

}
