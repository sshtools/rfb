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
package com.sshtools.rfbserver.protocol;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbserver.EndContinuousUpdates;
import com.sshtools.rfbserver.RFBClient;

public class NewEncodingsProtocolExtension implements ProtocolExtension {
	final static Logger LOG = LoggerFactory.getLogger(NewEncodingsProtocolExtension.class);

	public boolean handle(int msg, RFBClient rfbClient) throws IOException {
		ProtocolReader din = rfbClient.getInput();
		RFBEncoder encoder = rfbClient.getEncoder();
		synchronized (encoder.getLock()) {
			din.read(); // padding
			int noOfEncodings = din.readUnsignedShort();
			encoder.clearEnabled();
			LOG.info("Setting new encodings");
			for (int i = 0; i < noOfEncodings; i++) {
				int enc = din.readInt();
				if (encoder.isAvailable(enc)) {
					encoder.enable(rfbClient, enc);
				}
			}
			// encoder.clearUpdates();
			// Client's must always be able to handle Raw
			if (!encoder.isEncodingEnabled(RFBConstants.ENC_RAW)) {
				encoder.enable(rfbClient, RFBConstants.ENC_RAW);
			}
			encoder.resetPointerShape();
			
			/** https://vncdotool.readthedocs.io/en/0.8.0/rfbproto.html#continuousupdates-pseudo-encoding 
			 *
			 * If continuous updates pseudo-encoding is requested, we have to immediately send 
			 * an 'End Continuous Updates' message to indicate we support continuous updates 
			 **/
			if(encoder.isEncodingEnabled(RFBConstants.ENC_CONTINUOUS_UPDATES)) {
				encoder.queueUpdate(new EndContinuousUpdates());
			}
			
			// Cursor shape updates may have been enabled or disabled,
			// so update either the shape or send some frame buffer
			// updates
			// rfbClient.pointerChange();
			return true;
		}
	}
}
