package com.sshtools.rfb.encoding;

import java.io.IOException;

import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEncoding;

public class CursorPositionEncoding implements RFBEncoding {
    public CursorPositionEncoding() {
    }

    /**
     * getType
     *
     * @return int
     * @todo Implement this com.sshtools.rfb.RFBEncoding method
     */
    @Override
	public int getType() {
        return 0xffffff18;
    }

    /**
     * isPseudoEncoding
     *
     * @return boolean
     * @todo Implement this com.sshtools.rfb.RFBEncoding method
     */
    @Override
	public boolean isPseudoEncoding() {
        return true;
    }

    /**
     * processEncodedRect
     *
     * @param display RFBDisplay
     * @param x int
     * @param y int
     * @param width int
     * @param height int
     * @param encodingType int
     * @throws IOException
     * @todo Implement this com.sshtools.rfb.RFBEncoding method
     */
    @Override
	public void processEncodedRect(RFBDisplay display, int x, int y, int width,
                                   int height, int encodingType) throws
            IOException {
        display.getDisplayModel().softCursorMove(x, y);
//                (int)(((float)x - (float)display.getDisplayModel().getImagex() ) / display.getDisplayModel().getXscale(),
//                (int)(((float)x - (float)display.getDisplayModel().getImagex() ) / display.getDisplayModel().getXscale(),
//                                ,y);
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.sshtools.rfb.RFBEncoding#getName()
     */
    @Override
	public String getName() {
        return "Cursor Position";
    }
}
