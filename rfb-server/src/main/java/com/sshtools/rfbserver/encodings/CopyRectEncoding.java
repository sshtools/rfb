package com.sshtools.rfbserver.encodings;

import java.awt.Point;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public class CopyRectEncoding extends AbstractRawEncoding {
    final static Logger LOG = LoggerFactory.getLogger(CopyRectEncoding.class);

    public CopyRectEncoding() {
    }

    public TightCapability getType() {
        return RFBConstants.CAP_ENC_COPYRECT;
    }

    public boolean isPseudoEncoding() {
        return false;
    }

    public void encode(UpdateRectangle<?> update, ProtocolWriter dout, PixelFormat pixelFormat, RFBClient client)
                    throws IOException {
        UpdateRectangle<Point> u = (UpdateRectangle<Point>) update;
        LOG.info("CopyRect of " + u.getData() + " to " + update.getArea());
        dout.writeInt(getType().getCode());
        dout.writeShort(u.getData().x);
        dout.writeShort(u.getData().y);
    }

}
