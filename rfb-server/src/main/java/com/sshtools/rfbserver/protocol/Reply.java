package com.sshtools.rfbserver.protocol;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Reply<T> {

    protected T data;
    protected int code;

    protected Reply(int code) {
        this.code = code;
    }

//    public abstract Reply<T> copy();

    protected void populate(Reply<T> t) {
        t.data = data;
        t.code = code;
    }

    public void setData(T data) {
        if (this.data != null && this.data instanceof Point && data != null && data instanceof BufferedImage) {
            try {
                throw new Exception();
            } catch (Exception e) {
                System.err.println("POINT CHANGED TO IMG");
                e.printStackTrace();
            }
        }
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Reply [data=" + data + ", code=" + code + "]";
    }

    public abstract void write(DataOutputStream dout) throws IOException;

}