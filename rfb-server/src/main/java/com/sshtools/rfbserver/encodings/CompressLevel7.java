package com.sshtools.rfbserver.encodings;


public class CompressLevel7 extends CompressLevel0 {

    public int getCode() {
        return getType().getCode() + 7;
    }

}
