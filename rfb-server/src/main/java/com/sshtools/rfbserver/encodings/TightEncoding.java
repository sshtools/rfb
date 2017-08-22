package com.sshtools.rfbserver.encodings;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
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
	protected void writeTightBasic(UpdateRectangle<?> update, ProtocolWriter dout, PixelFormat pixelFormat, ProtocolWriter writer,
			int[] tileBuf) throws IOException {
		// TODO no gradient filter yet
		dout.writeByte(OP_READ_FILTER_ID);
		ByteArrayOutputStream tmp = new ByteArrayOutputStream();
		DataOutputStream tmpDout = new DataOutputStream(tmp);
		boolean compress = true;
		if (pan.getSize() <= 256) {
			dout.write(OP_FILTER_PALETTE);
			tmpDout.writeByte(pan.getPalette().length - 1);
			for (int i = 0; i < pan.getPalette().length; i++)
				TightUtil.writeTightColor(pan.getPalette()[i], pixelFormat, tmpDout);
			if (pan.getSize() == 2) {
				int b = 0;
				int s = 7;
				for (int i = 0; i < tileBuf.length; i++) {
					if (tileBuf[i] == pan.getPalette()[1]) {
						b |= 1 << s;
					}
					s--;
					if (s < 0) {
						tmpDout.writeByte(b);
						b = 0;
						s = 7;
					}
				}
				if (s != 0)
					tmpDout.writeByte(b);
			} else {
				for (int i = 0; i < tileBuf.length; i++) {
					tmpDout.writeByte(pan.lookup(tileBuf[i]));
				}
			}
		} else {
			compress = writeTightRaw((UpdateRectangle<BufferedImage>) update, dout, pixelFormat, tmpDout);
		}
		// TODO zlib streams... how do they work?
		byte[] data = tmp.toByteArray();
		if (data.length >= 12 && compress) {
			byte[] compressed = compress(data).toByteArray();
			LOG.info(String.format("Compressing %d to %d bytes.", data.length, compressed.length));
			data = compressed;
		} else {
			LOG.info(String.format("Not compressing %d bytes.", data.length));
		}
		writer.writeCompactLen(data.length);
		writer.write(data);
	}

	protected boolean writeTightRaw(UpdateRectangle<BufferedImage> update, ProtocolWriter dout, PixelFormat pixelFormat,
			DataOutputStream tmpDout) throws IOException {
		dout.write(OP_FILTER_RAW);
		if (TightUtil.isTightNative(pixelFormat)) {
			DataBufferInt dataBuffer = (DataBufferInt) update.getData().getData().getDataBuffer();
			for (int i : dataBuffer.getData()) {
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
