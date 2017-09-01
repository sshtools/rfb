package com.sshtools.rfb.encoding;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.ScreenData;
import com.sshtools.rfbcommon.ScreenDetail;
import com.sshtools.rfbcommon.ScreenDimension;

public class ExtendedDesktopSizeEncoding implements RFBEncoding {
	final static Logger LOG = LoggerFactory.getLogger(ExtendedDesktopSizeEncoding.class);
	public final static int SERVER_SIDE_CHANGE = 0;
	public final static int CLIENT_SIDE_CHANGE = 1;
	public final static int OTHER_CLIENT_SIDE_CHANGE = 2;
	public final static int NO_ERROR = 0;
	public final static int ADMINISTRATIVELY_PROHIBITED = 1;
	public final static int OUT_OF_RESOURCES = 3;
	public final static int INVALID_LAYOUT = 4;

	public ExtendedDesktopSizeEncoding() {
	}

	@Override
	public int getType() {
		return RFBConstants.ENC_EXTENDED_FB_SIZE;
	}

	@Override
	public boolean isPseudoEncoding() {
		return true;
	}

	@Override
	public void processEncodedRect(RFBDisplay<?, ?> display, int x, int y, int width, int height, int encodingType)
			throws IOException {
		if (!display.getEngine().isUseExtendedDesktopSize()) {
			LOG.info("Using extended desktop size.");
			display.getEngine().setUseExtendedDesktopSize(true);
		}
		ProtocolReader in = display.getEngine().getInputStream();
		int originOfChange = x;
		int status = y;
		int noOfScreens = in.readUnsignedByte();
		in.skip(3);
		ScreenData sd = new ScreenData(new ScreenDimension(width, height));
		LOG.info(String.format("%d screens. Primary is %dx%d", noOfScreens, sd.getWidth(), sd.getHeight()));
		for (int i = 0; i < noOfScreens; i++) {
			ScreenDetail d = new ScreenDetail(in.readUInt32(), in.readUnsignedShort(), in.readUnsignedShort(),
					new ScreenDimension(in.readUnsignedShort(), in.readUnsignedShort()), in.readUInt32());
			sd.getDetails().add(d);
			LOG.info(String.format("    %2d [%10d] %dx%d@%d,%d (%d)", i + 1, d.getId(), d.getWidth(), d.getHeight(), d.getX(),
					d.getY(), d.getFlags()));
		}
		/* Ignore if the change originated from us */
		if (originOfChange != CLIENT_SIDE_CHANGE) {
			if (status != NO_ERROR) {
				LOG.warn("Server refused our desktop size change request.");
			} else {
				display.getDisplayModel().changeFramebufferSize(originOfChange, sd);
			}
		}
	}

	@Override
	public String getName() {
		return "Extended Desktop Size";
	}
}
