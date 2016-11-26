package com.sshtools.rfbserver.encodings;

public class CompressLevel9 extends CompressLevel0 {
    public int getCode() {
        return getType().getCode() + 9;
    }

}
