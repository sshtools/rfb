package com.sshtools.rfbserver.protocol;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbserver.RFBClient;

public class NewPixelFormatProtocolExtension implements ProtocolExtension {
    final static Logger LOG = LoggerFactory.getLogger(NewPixelFormatProtocolExtension.class);

    public boolean handle(int msg, RFBClient rfbClient) throws IOException {
        ProtocolReader din = rfbClient.getInput();
        PixelFormat pixelFormat = rfbClient.getPixelFormat();

        din.readFully(new byte[3]);

        // Clear out the current color map (if any)
        pixelFormat.getColorMap().clear();

        // Read in new format
        pixelFormat.read(din);

        // Some info
        boolean nativeFormat = pixelFormat.equals(rfbClient.getPreferredPixelFormat());
        if (nativeFormat) {
            LOG.info("Client expects pixels in same format as the source of this frame buffer, no conversion needed! "
                            + pixelFormat);
        } else {
            LOG.info("Client requested different format from server, conversion will occur on the server from "
                            + rfbClient.getPreferredPixelFormat() + " to " + pixelFormat);
        }

        // If the client wants indexed image, make sure we have a colour map
        // (either from the s
        if (!pixelFormat.isTrueColor()) {
            if (nativeFormat && !rfbClient.getPreferredPixelFormat().getColorMap().isEmpty()) {
                // Use the preferred colour map
                LOG.info("Using preferred colour map of the server");
                pixelFormat.getColorMap().putAll(rfbClient.getPreferredPixelFormat().getColorMap());
            } else {
                LOG.info("Creating a default colour map");

                // Create a default colour map
                // Create a 6x6x6 color cube
                int[] cmap = new int[256];
                int i = 0;
                for (int r = 0; r < 256; r += 51) {
                    for (int g = 0; g < 256; g += 51) {
                        for (int b = 0; b < 256; b += 51) {
                            cmap[i++] = (r << 16) | (g << 8) | b;
                        }
                    }
                }
                // And populate the rest of the cmap with gray values
                int grayIncr = 256 / (256 - i);

                // The gray ramp will be between 18 and 252
                int gray = grayIncr * 3;
                for (; i < 256; i++) {
                    cmap[i] = (gray << 16) | (gray << 8) | gray;
                    gray += grayIncr;
                }

                for (int idx = 0; idx < cmap.length; idx++) {
                    pixelFormat.getColorMap().put(idx, cmap[idx]);
                }
            }
            rfbClient.sendColourMapEntries();
        }

        return true;
    }

}
