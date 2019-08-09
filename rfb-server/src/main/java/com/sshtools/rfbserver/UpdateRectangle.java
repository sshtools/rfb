/**
 * RFB Server - Remote Frame Buffer (VNC Server) implementation. This is the base module if you want to create a VNC server. It takes a layered driver approach to add native specific features (which is recommened as the cross-platform default "Robot" driver is very slow).
 *
 * See the vncserver module for a concrete server implementation that has some native performance improvements for some platforms.
 * Copyright © 2006 SSHTOOLS Limited (support@sshtools.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.sshtools.rfbserver;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class UpdateRectangle<T> {

    private Rectangle area;
    private int encoding;
    private DisplayDriver driver;
    private T data;

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