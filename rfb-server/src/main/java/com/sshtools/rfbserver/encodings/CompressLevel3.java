package com.sshtools.rfbserver.encodings;


public class CompressLevel3 extends CompressLevel0 {

    public int getCode() {
        return getType().getCode() +3;
    }

}
