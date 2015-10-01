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