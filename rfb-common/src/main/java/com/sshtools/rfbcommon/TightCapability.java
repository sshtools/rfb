/**
 * RFB Common - Remote Frame Buffer common code used both in client and server.
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
/**
 * 
 */
package com.sshtools.rfbcommon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TightCapability {

    private int code;
    private String vendor;
    private String signature;

    public TightCapability(int code, String vendor, String signature) {
        super();
        this.code = code;
        this.vendor = vendor;
        this.signature = signature;
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(code);
        out.write(vendor.getBytes(), 0, 4);
        out.write(signature.getBytes(), 0, 8);
    }

    public TightCapability(DataInputStream in) throws IOException {
        code = in.readInt();
        byte[] buf = new byte[4];
        in.readFully(buf);
        vendor = new String(buf);
        buf = new byte[8];
        in.readFully(buf);
        signature = new String(buf);
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + code;
		result = prime * result
				+ ((signature == null) ? 0 : signature.hashCode());
		result = prime * result + ((vendor == null) ? 0 : vendor.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TightCapability other = (TightCapability) obj;
		if (code != other.code)
			return false;
		if (signature == null) {
			if (other.signature != null)
				return false;
		} else if (!signature.equals(other.signature))
			return false;
		if (vendor == null) {
			if (other.vendor != null)
				return false;
		} else if (!vendor.equals(other.vendor))
			return false;
		return true;
	}

	public int getCode() {
        return code;
    }

    public String getVendor() {
        return vendor;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public String toString() {
        return "TightCapability [code=" + code + ", signature=" + signature + ", vendor=" + vendor + "]";
    }
}