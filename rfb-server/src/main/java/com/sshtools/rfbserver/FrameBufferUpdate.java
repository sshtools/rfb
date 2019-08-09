/**
 * RFB Server - Remote Frame Buffer (VNC Server) implementation. This is the base module if you want to create a VNC server. It takes a layered driver approach to add native specific features (which is recommened as the cross-platform default "Robot" driver is very slow).
 *
 * See the vncserver module for a concrete server implementation that has some native performance improvements for some platforms.
 * Copyright © 2006 SSHTOOLS Limited (support@sshtools.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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