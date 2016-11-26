package com.sshtools.rfbserver.encodings;

import java.io.DataOutputStream;
import java.io.IOException;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public class ZLIBEncoding extends AbstractZLIBEncoding {

    /**
     * Minimum ZLIB update size (smaller than this there is no point in
     * compressing due to compression overhead)
     */
    private final static int VNC_ENCODE_ZLIB_MIN_COMP_SIZE = 17;

    public ZLIBEncoding() {
    }

    public TightCapability getType() {
        return RFBConstants.CAP_ENC_ZLIB;
    }

    public synchronized void encode(UpdateRectangle<?> update, DataOutputStream dout, PixelFormat pixelFormat, RFBClient client)
                    throws IOException {
        int width = update.getArea().width;
        int height = update.getArea().height;
        int bytesPerPixel = pixelFormat.getBitsPerPixel() / 8;

        if (width * height * bytesPerPixel < VNC_ENCODE_ZLIB_MIN_COMP_SIZE) {
            rawEncode(update, dout, pixelFormat);
            return;
        }
        super.encode(update, dout, pixelFormat, client);
    }

    public String getName() {
        return "ZLIB";
    }

}
