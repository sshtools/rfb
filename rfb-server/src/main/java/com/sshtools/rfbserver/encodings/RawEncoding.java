package com.sshtools.rfbserver.encodings;

import java.io.DataOutputStream;
import java.io.IOException;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public class RawEncoding extends AbstractRawEncoding {

    public RawEncoding() {
    }

    public TightCapability getType() {
        return RFBConstants.CAP_ENC_RAW;
    }

    public boolean isPseudoEncoding() {
        return false;
    }

    public void encode(UpdateRectangle<?> update, DataOutputStream dout, PixelFormat pixelFormat, RFBClient client)
                    throws IOException {
        rawEncode(update, dout, pixelFormat);
    }

}
