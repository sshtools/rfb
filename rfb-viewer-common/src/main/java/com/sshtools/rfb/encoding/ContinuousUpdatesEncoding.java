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
