package com.sshtools.rfbserver.encodings;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.UpdateRectangle;

public class TightPNGEncoding extends AbstractTightEncoding {
	final static Logger LOG = LoggerFactory.getLogger(TightPNGEncoding.class);

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_TIGHT_PNG;
	}

	@Override
	protected void writeTightBasic(UpdateRectangle<?> update, ProtocolWriter dout, PixelFormat pixelFormat, ProtocolWriter writer,
			int[] tileBuf) throws IOException {
		dout.writeByte(OP_PNG << 4);
		ByteArrayOutputStream bout = new ByteArrayOutputStream(
				(int) update.getArea().getWidth() * (int) update.getArea().getHeight());
		ImageIO.write((BufferedImage) update.getData(), "PNG", bout);
		byte[] data = bout.toByteArray();
		dout.writeCompactLen(data.length);
		dout.write(data);
	}

	@Override
	protected void encodeImage(BufferedImage img, ByteArrayOutputStream bout, ProtocolWriter dout) throws IOException {
		if (tightLevel == -1) {
			dout.writeByte(OP_PNG << 4);
			if (!ImageIO.write(img, "PNG", bout))
				throw new IOException("Cannot encode as PNG");
		} else {
			dout.writeByte(OP_JPEG << 4);
			if (!ImageIO.write(img, "JPEG", bout))
				throw new IOException("Cannot encode as JPEG");
		}
	}
}
