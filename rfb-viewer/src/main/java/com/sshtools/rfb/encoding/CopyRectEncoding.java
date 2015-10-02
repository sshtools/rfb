package com.sshtools.rfb.encoding;

import java.io.IOException;

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEncoding;

public class CopyRectEncoding implements RFBEncoding {

    public CopyRectEncoding() {
    }

    @Override
	public boolean isPseudoEncoding() {
        return false;
    }

    @Override
	public int getType() {
        return 1;
    }

    @Override
	public void processEncodedRect(RFBDisplay display, int x, int y, int width, int height, int encodingType) throws IOException {

        // Get the position of the area
        int posx = display.getEngine().getInputStream().readUnsignedShort();
        int posy = display.getEngine().getInputStream().readUnsignedShort();

        // Copy the area
        display.getDisplayModel().getGraphicBuffer().copyArea(posx, posy, width, height, x - posx, y - posy);

        // Request a repaint
        display.requestRepaint(display.getContext().getScreenUpdateTimeout(), x, y, width, height);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sshtools.rfb.RFBEncoding#getName()
     */
    @Override
	public String getName() {
        return "CopyRect";
    }

}