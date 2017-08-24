package com.sshtools.rfbserver.encodings;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbcommon.TightUtil;
import com.sshtools.rfbserver.UpdateRectangle;

public class TightEncoding extends AbstractTightEncoding {
	final static Logger LOG = LoggerFactory.getLogger(TightEncoding.class);

	public TightCapability getType() {
		return RFBConstants.CAP_ENC_TIGHT;
	}

	@SuppressWarnings("unchecked")
	protected void writeTightBasic(UpdateRectangle<?> update, ProtocolWriter odout, PixelFormat pixelFormat, ProtocolWriter writer,
			int[] tileBuf) throws IOException {
		// TODO no gradient filter yet (dont use gradient anyway with jpeg)
		Rectangle area = update.getArea();
		boolean compress = true;
		int palSize = pan.getSize();
		ByteArrayOutputStream tmp = new ByteArrayOutputStream();
		ProtocolWriter dout = new ProtocolWriter(tmp);
		if (palSize <= 256) {
			odout.writeByte(OP_READ_FILTER_ID);
			odout.write(OP_FILTER_PALETTE);
			odout.writeByte(palSize - 1);
			for (int i = 0; i < palSize; i++)
				TightUtil.writeTightColor(pan.getPalette()[i], pixelFormat, odout);
			if (palSize == 2) {
				int dx, dy, n;
				int rowBytes = (area.width + 7) / 8;
				byte b;
				for (dy = 0; dy < area.height; dy++) {
					for (dx = 0; dx < rowBytes; dx++) {
						b = 0;
						for (n = 0; b < 8 && n + dx < area.getWidth(); n++)
							b |= tileBuf[(dy * area.width) + dx + n] == pan.getPalette()[1] ? 1 << (7 - n) : 0;
						dout.writeByte(b);
					}
				}
			} else {
				for (int i = 0; i < tileBuf.length; i++) {
					dout.writeByte(pan.lookup(tileBuf[i]));
				}
			}
		} else {
			compress = writeTightRaw((UpdateRectangle<BufferedImage>) update, odout, pixelFormat, dout, tileBuf);
		}
		byte[] data = tmp.toByteArray();
		if (data.length >= RFBConstants.TIGHT_MIN_BYTES_TO_COMPRESS && compress) {
			byte[] compressed = compress(data).toByteArray();
			data = compressed;
			writer.writeCompactLen(data.length);
		}
		writer.write(data);
	}

	protected boolean writeTightRaw(UpdateRectangle<BufferedImage> update, ProtocolWriter dout, PixelFormat pixelFormat,
			DataOutputStream tmpDout, int[] tileBuf) throws IOException {
		dout.writeByte(OP_COPY);
		if (TightUtil.isTightNative(pixelFormat)) {
			for (int i : tileBuf) {
				TightUtil.writeTightColor(i, pixelFormat, tmpDout);
			}
		} else
			tmpDout.write(prepareEncode(update, pixelFormat));
		return true;
	}

	protected void encodeImage(BufferedImage img, ByteArrayOutputStream bout, ProtocolWriter dout) throws IOException {
		dout.writeByte(OP_JPEG << 4);
		ImageIO.write(img, "JPEG", bout);
	}
}
