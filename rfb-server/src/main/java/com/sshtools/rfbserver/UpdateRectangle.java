package com.sshtools.rfbserver;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class UpdateRectangle<T> {

    private Rectangle area;
    private int encoding;
    private DisplayDriver driver;
    private T data;

    protected boolean important;

    public UpdateRectangle(DisplayDriver driver, Rectangle area, int encoding) {
        this.encoding = encoding;
        this.area = area;
        this.driver = driver;
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

    public T getData() {
        return data;
    }

    public boolean isImportant() {
        return important;
    }

    public UpdateRectangle<T> setImportant(boolean important) {
        this.important = important;
        return this;
    }

    public DisplayDriver getDriver() {
        return driver;
    }

    public Rectangle getArea() {
        return area;
    }

    public void setArea(Rectangle area) {
        this.area = area;
    }

    public int getEncoding() {
        return encoding;
    }

    public void setEncoding(int encoding) {
        this.encoding = encoding;
    }

    @Override
    public String toString() {
        return "UpdateRectangle [area=" + area + ", encoding=" + encoding + "]";
    }

}