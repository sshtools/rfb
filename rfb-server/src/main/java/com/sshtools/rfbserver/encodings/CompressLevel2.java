package com.sshtools.rfbserver.encodings;


public class CompressLevel2 extends CompressLevel0 {


    public int getCode() {
        return getType().getCode() + 2;
    }

}
