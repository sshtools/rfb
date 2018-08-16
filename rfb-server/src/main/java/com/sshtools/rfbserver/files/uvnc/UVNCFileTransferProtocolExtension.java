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
package com.sshtools.rfbserver.files.uvnc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.RFBFile;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.files.RFBServerFS;
import com.sshtools.rfbserver.protocol.ProtocolExtension;
import com.sshtools.rfbserver.protocol.RFBEncoder;

public class UVNCFileTransferProtocolExtension implements ProtocolExtension {
    // TODO move these to RFB constants

    // Requests
    private final static int RFB_DIR_CONTENT_REQUEST = 1;
    private final static int RFB_COMMAND = 10;
    // Request parameters
    private final static int RFB_DIR_CONTENT = 1;
    private final static int RFB_DIR_DRIVE_LIST = 2;
    // Command parameters
    private final static int RFB_DIR_CREATE = 1;
    // Received
    private final static int RFB_RECV_NONE = 0;
    private final static int RFB_DIR_PACKET = 2;
    private final static int RFB_RECV_DIRECTORY = 1;
    private final static int RFB_RECV_DRIVE_LIST = 3;

    final static Logger LOG = LoggerFactory.getLogger(RFBClient.class);
    private ProtocolReader din;
    private RFBServerFS fs;
    private RFBClient client;
    private RFBEncoder encoder;

    protected void fileTransferRequested() throws IOException, UnsupportedEncodingException {

        int request = din.read();
        int param = din.read();
        int notSure = din.read();
        int notSureAlso = din.readInt();

        LOG.info("SOMETHING ELSE " + notSure);
        LOG.info("AGAIN " + notSureAlso);

        // Filename
        String filename = din.readTerminatedString();

        if (request == RFB_COMMAND) {
            if (param == RFB_DIR_CREATE) {
                if (fs != null) {
                    if (!fs.mkdir(filename)) {
                        // ?
                    }
                } else {
                    // ??
                }
            }
        } else {
            if (param == RFB_DIR_DRIVE_LIST) {
                dirListDrives();
            } else if (param == RFB_DIR_CONTENT_REQUEST) {
                dirListDirectory(filename);
            } else {
                throw new UnsupportedEncodingException("Unknown file transfer parameter " + param);
            }
        }
    }

    //
    protected void dirListDrives() throws IOException {
        ListDrivesReply list = new ListDrivesReply();
        List<Character> drives = new ArrayList<Character>();
        for (char i = 'A'; i <= 'Z'; i++) {
            drives.add(i);
        }
        RFBFile[] roots = fs.getRoots();
        List<RFBDrive> d = new ArrayList<RFBDrive>();
        for (RFBFile r : roots) {
            char l = Character.toUpperCase(r.getName().charAt(0));
            if (!drives.contains(l)) {
                if (drives.isEmpty()) {
                    LOG.warn("Too many roots.");
                    break;
                }
                l = drives.get(drives.get(0));
            }
            d.add(new RFBDrive(l + ":", RFBDrive.UNKNOWN));
        }

        list.setData(d);
        synchronized (encoder.getLock()) {
            encoder.queueUpdate(list);
        }
//        client.sendQueuedReplies();
    }

    protected void dirListDirectory(String path) throws IOException {

        RFBFile[] listing = null;
        try {
            if (fs != null) {
                listing = fs.list(path);
            }
        } catch (IOException e) {
            LOG.error("Failed to list files. Returning empty list.", e);
        }
        synchronized (encoder.getLock()) {
            if (listing == null) {
                encoder.queueUpdate(new StartListDirectoryReply(path));
            } else {
                encoder.queueUpdate(new StartListDirectoryReply(path));
                for (RFBFile f : listing) {
                    encoder.queueUpdate(new ListDirectoryReply(f));
                }
                encoder.queueUpdate(new EndListDirectoryReply(listing.length));
            }
        }
//        client.sendQueuedReplies();
    }

    public boolean handle(int msg, RFBClient rfbClient) throws IOException {
        if (msg == RFBConstants.SMSG_FILE_TRANSFER) {

            encoder = rfbClient.getEncoder();
            client = rfbClient;
            din = rfbClient.getInput();
            fs = rfbClient.getFs();

            fileTransferRequested();
            return true;
        }
        return false;
    }
}
