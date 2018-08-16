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

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfbcommon.RFBConstants;

public class ContinuousUpdatesEncoding implements RFBEncoding {
	final static Logger LOG = LoggerFactory.getLogger(ContinuousUpdatesEncoding.class);

	public ContinuousUpdatesEncoding() {
	}

	@Override
	public int getType() {
		return RFBConstants.ENC_CONTINUOUS_UPDATES;
	}

	@Override
	public boolean isPseudoEncoding() {
		return true;
	}

	@Override
	public void processEncodedRect(RFBDisplay<?, ?> display, int x, int y, int width, int height, int encodingType)
			throws IOException {
		/*
		 * If we get this message at all then continuous updates are supported
		 */
		if (!display.getEngine().isContinuousUpdatesSupported()) {
			display.getEngine().setContinuousUpdatesSupported(true);
			if (display.getContext().isContinuousUpdates()) {
				LOG.info("Continuous updates are supported and enabled");
				display.getEngine().enableContinuousUpdates();
			} else {
				LOG.info("Continuous updates are supported by server, but this client is not configured to use them.");
			}
		} else {
			if (display.getContext().isContinuousUpdates()) {
				LOG.info("Turning off continuous updates");
				display.getContext().setContinuousUpdates(false);
			}
		}
	}

	@Override
	public String getName() {
		return "Continuous Updates";
	}
}
