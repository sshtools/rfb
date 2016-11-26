package com.sshtools.rfbserver.encodings;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.ImageUtil;
import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.PixelFormatImageFactory;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.DisplayDriver.PointerShape;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public class CursorEncoding extends AbstractRawEncoding implements RFBServerEncoding {
    final static Logger LOG = LoggerFactory.getLogger(CursorEncoding.class);

    public final static byte[] FOREGROUND = { (byte) 0xff, (byte) 0xff, (byte) 0xff };
    public final static byte[] BACKGROUND = { (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    public CursorEncoding() {
    }

    public TightCapability getType() {
        return RFBConstants.CAP_ENC_RICH_CURSOR;
    }

    public boolean isPseudoEncoding() {
        return true;
    }

    public void encode(UpdateRectangle<?> update, DataOutputStream dout, PixelFormat pixelFormat, RFBClient client)
                    throws IOException {
        LOG.info("Sending default cursor shape update in " + pixelFormat);

        PointerShape pc = (PointerShape) update.getData();
        BufferedImage img = pc.getData();

        int height = update.getArea().height;
        int width = update.getArea().width;

        // The cursor will not be in the correct format, so create a new image 
        BufferedImage compatImg = new PixelFormatImageFactory(pixelFormat).create(img.getWidth(), img.getHeight());
        compatImg.getGraphics().drawImage(img, 0, 0, null);
        img = compatImg;

        // Get raw pixel data
        UpdateRectangle<BufferedImage> u = new UpdateRectangle<BufferedImage>(update.getDriver(),
                        new Rectangle(0, 0, width, height), getType().getCode());
        u.setData(img);
        byte[] pixelData = prepareEncode(u, pixelFormat);

        // Get mask
        int bytesPerRow = (width + 7) / 8;
        int bytesMaskData = bytesPerRow * height;
        ByteBuffer maskBuf = ByteBuffer.allocate(bytesMaskData);
        int bufSize = bytesPerRow * 8;
        BitSet maskRow = new BitSet(bufSize);
        int[] pix = new int[4];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.getRaster().getPixel(width - x - 1, y, pix);
                maskRow.set(x, pix[0] >= 1);
            }
            maskBuf.put(ImageUtil.toByteArray(maskRow, bytesPerRow));
        }
        byte[] array2 = maskBuf.array();

        //
        dout.writeInt(getType().getCode());
        dout.write(pixelData);
        dout.write(array2);
    }

}
