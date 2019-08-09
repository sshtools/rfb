/**
 * RFB - Remote Frame Buffer (VNC) implementation.
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
package com.sshtools.rfb.encoding;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfb.ProtocolEngine;
import com.sshtools.rfbcommon.RFBConstants;

public class TightPNGEncoding extends TightEncoding {
	final static Logger LOG = LoggerFactory.getLogger(ProtocolEngine.class);

	public TightPNGEncoding() {
	}

	@Override
	public int getType() {
		return RFBConstants.ENC_TIGHT_PNG;
	}

	@Override
	public String getName() {
		return "Tight PNG";
	}

	@Override
	public boolean isPseudoEncoding() {
		return false;
	}

	protected void doTight(int x, int y, int width, int height, int op) throws IOException {
		int type = op >> 4 & 0x0F;
		if (type == OP_PNG) {
			doGenericImage(x, y);
		} else {
			super.doTight(x, type, width, height, op);
		}
	}
}
