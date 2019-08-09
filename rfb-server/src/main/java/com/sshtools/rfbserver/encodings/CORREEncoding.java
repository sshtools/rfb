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
package com.sshtools.rfbserver.encodings;

import java.io.DataOutputStream;
import java.io.IOException;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;

public class CORREEncoding extends AbstractRREEncoding {

    public CORREEncoding() {
        super();
    }

    public TightCapability getType() {
        return RFBConstants.CAP_ENC_CORRE;
    }

    public boolean isPseudoEncoding() {
        return false;
    }

    @Override
    protected void writeSubrect(DataOutputStream dout, PixelFormat pixelFormat, SubRect s) throws IOException {
        writePixel(dout, pixelFormat, s.pixel);
        dout.write(s.x);
        dout.write(s.y);
        dout.write(s.w);
        dout.write(s.h);
    }
}
