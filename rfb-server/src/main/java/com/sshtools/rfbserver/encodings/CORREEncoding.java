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
