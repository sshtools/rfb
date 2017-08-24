package com.sshtools.rfbserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbserver.encodings.RFBServerEncoding;
import com.sshtools.rfbserver.protocol.RFBEncoder;
import com.sshtools.rfbserver.protocol.Reply;

public class FrameBufferUpdate extends Reply<List<UpdateRectangle<Object>>> {
	final static Logger LOG = Logger.getLogger(FrameBufferUpdate.class.getName());
	private RFBEncoder encoder;
	private PixelFormat pixelFormat;

	public FrameBufferUpdate(PixelFormat pixelFormat, RFBEncoder encoder) {
		super(RFBConstants.SMSG_FRAMEBUFFER_UPDATE);
		data = new ArrayList<>();
		this.pixelFormat = pixelFormat;
		this.encoder = encoder;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void write(ProtocolWriter dout) throws IOException {
		dout.write(0); // areas
		dout.writeShort(data.size());
		for (UpdateRectangle<Object> area : data) {
			int encId = area.getEncoding();
			RFBServerEncoding<Object> enc = null;
			if (encId != -1) {
				enc = (RFBServerEncoding<Object>) encoder.getEnabledEncoding(encId);
			}
			if (enc == null) {
				encId = encoder.getPreferredEncoding();
				enc = (RFBServerEncoding<Object>) encoder.getEnabledEncoding(encId);
			}
			// if (LOG.isDebugEnabled()) {
			if (!enc.isPseudoEncoding()) {
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("Area:" + area + " " + enc.getType().getSignature() + " " + enc.getType() + " for " + area);
				}
			}
			// }
			dout.writeShort(area.getArea().x);
			dout.writeShort(area.getArea().y);
			dout.writeShort(area.getArea().width);
			dout.writeShort(area.getArea().height);
			enc.encode(area, dout, pixelFormat, encoder.getClient());
		}
	}
}