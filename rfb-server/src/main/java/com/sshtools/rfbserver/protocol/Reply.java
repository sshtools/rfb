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
package com.sshtools.rfbserver.protocol;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.sshtools.rfbcommon.ProtocolWriter;

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

    public abstract void write(ProtocolWriter dout) throws IOException;

}