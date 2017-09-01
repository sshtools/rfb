package com.sshtools.rfbserver.files.tight;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.ProtocolReader;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.RFBFile;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.files.RFBServerFS;
import com.sshtools.rfbserver.protocol.ProtocolExtension;

public class TightFileTransferProtocolExtension implements ProtocolExtension {

    final static Logger LOG = LoggerFactory.getLogger(RFBClient.class);
    private RFBServerFS fs;
    private RFBClient client;
    private ProtocolWriter output;
    private ProtocolReader input;
    private OutputStream receiveTarget;
    private InputStream sendTarget;
    private String sendingPath;
    private String receivePath;

    public boolean handle(int msg, RFBClient rfbClient) throws IOException {
        client = rfbClient;

        fs = client.getFs();
        output = client.getOutput();
        input = client.getInput();

        //
        int ignore = input.read();
        short code = input.readShort();

        //
        int actualCode = (msg << 24 & 0xff000000) | (ignore << 16 & 0x00ff0000) | code;
        switch (actualCode) {
            case RFBConstants.CAP_FTCCSRST_CODE:
                compressionSupport();
                break;
            case RFBConstants.CAP_FTCFLRST_CODE:
                fileListRequest();
                break;
            case RFBConstants.CAP_FTCMDRST_CODE:
                mkdir();
                break;
            case RFBConstants.CAP_FTCFRRST_CODE:
                rm();
                break;
            case RFBConstants.CAP_FTCFMRST_CODE:
                mv();
                break;
            case RFBConstants.CAP_FTCFURST_CODE:
                startReceive();
                break;
            case RFBConstants.CAP_FTCUDRST_CODE:
                receive();
                break;
            case RFBConstants.CAP_FTCUERST_CODE:
                endReceive();
                break;
            case RFBConstants.CAP_FTCFDRST_CODE:
                startSend();
                break;
            case RFBConstants.CAP_FTCDDRST_CODE:
                send();
                break;
            case RFBConstants.CAP_FTCDSRST_CODE:
                dirSize();
                break;
            case RFBConstants.CAP_FTSM5RLY_CODE:
                md5();
                break;
            default:
                throw new UnsupportedOperationException("No tight VNC file transfer op with code of "
                                + Long.toHexString(actualCode));
        }
        return true;
    }

    protected void dirSize() throws IOException {
        String path = input.readTerminatedString();
        if (!checkForFs(client)) {
            return;
        }
        long dirSize = 0;
        try {
            dirSize = getDirectorySize(path);
        } catch (IOException e) {
            error(e);
            return;
        }
        synchronized (client.getWriteLock()) {
            output.writeUInt32(RFBConstants.CAP_FTSDSRLY_CODE);
            output.writeLong(dirSize); // make this 1 when compression supported
            output.flush();
        }
    }

    private long getDirectorySize(String path) throws IOException {
        long ds = 0;
        for (RFBFile f : fs.list(path)) {
            if (f.isFolder()) {
                ds += getDirectorySize(path + "/" + f.getName());
            } else {
                ds += f.getSize();
            }
        }
        return ds;
    }

    protected void md5() throws IOException {
        String path = input.readTerminatedString();
        long offset = input.readLong();
        long len = input.readLong();
        if (!checkForFs(client)) {
            return;
        }
        try {
            InputStream in = fs.retrieve(path, offset);
            try {
                byte[] cs = createChecksum(in, len);
                synchronized (client.getWriteLock()) {
                    output.writeUInt32(RFBConstants.CAP_FTSM5RLY_CODE);
                    output.write(cs);
                    output.flush();
                }
            } finally {
                in.close();
            }
        } catch (Exception e) {
            error(e);
            return;
        }
    }

    public static byte[] createChecksum(InputStream fis, long len) throws Exception {
        byte[] buffer = new byte[Math.min((int) len, 1024)];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        long toRead = len;

        do {
            numRead = fis.read(buffer, 0, Math.min((int) toRead, buffer.length));
            if (numRead > 0) {
                toRead -= len;
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    protected void compressionSupport() throws IOException {
        // TODO support compression
        synchronized (client.getWriteLock()) {
            output.writeUInt32(RFBConstants.CAP_FTSCSRLY_CODE);
            output.write(0); // make this 1 when compression supported
            output.flush();
        }
    }

    protected void startSend() throws IOException {
        sendingPath = input.readTerminatedString();
        long offset = input.readLong();
        if (!checkForFs(client)) {
            return;
        }
		if(LOG.isDebugEnabled())
			LOG.debug("Sending " + sendingPath);
        if (sendTarget != null) {
            try {
                sendTarget.close();
            } catch (Exception e) {
            }
        }
        sendTarget = fs.retrieve(sendingPath, offset);
        synchronized (client.getWriteLock()) {
            output.writeUInt32(RFBConstants.CAP_FTSFDRLY_CODE);
            output.flush();
        }
    }

    protected void send() throws IOException {
        int compress = input.read();
        int dataSize = (int) input.readUInt32();

        if (!checkForFs(client)) {
            return;
        }

        // TODO support compression

        if (sendTarget == null) {
            error("No active download.");
            return;
        }

        byte[] b = new byte[dataSize];
        int read = 0;
        if (dataSize != 0) {
            try {
                read = sendTarget.read(b);
                if (read == -1) {
                    throw new EOFException();
                }
            } catch (EOFException e) {
                synchronized (client.getWriteLock()) {
                    output.writeUInt32(RFBConstants.CAP_FTSDERLY_CODE);
                    output.write(0);
                    RFBFile f = fs.get(sendingPath);
                    output.writeLong(f.getLastWriteTime());
                    output.flush();
                }
                return;
            }
        }

        synchronized (client.getWriteLock()) {
            output.writeUInt32(RFBConstants.CAP_FTSDDRLY_CODE);
            output.write(compress);
            output.writeUInt32(read);
            output.writeUInt32(read);
            output.write(b, 0, read);
            output.flush();
        }
    }

    protected void receive() throws IOException {
        int compress = input.read();
        int compressedSize = (int) input.readInt();
        int uncompressedSize = (int) input.readInt();

        if (!checkForFs(client)) {
            return;
        }

        // TODO support compression
        if (receiveTarget == null) {
            error("No active upload.");
            return;
        }

        byte[] b = new byte[uncompressedSize];
        if (LOG.isDebugEnabled()) {
            LOG.debug("Receiving " + b.length + " (client requested " + compressedSize + "/" + uncompressedSize + " @ " + compress);
        }
        input.readFully(b);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Writing " + b.length + " (client requested " + compressedSize + "/" + uncompressedSize + " @ " + compress);
        }
        receiveTarget.write(b);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Written " + b.length + " (client requested " + compressedSize + "/" + uncompressedSize + " @ " + compress);
        }

        synchronized (client.getWriteLock()) {
            output.writeUInt32(RFBConstants.CAP_FTSUDRLY_CODE);
            output.flush();
        }
    }

    protected void startReceive() throws IOException {
        receivePath = input.readTerminatedString();
        int flags = input.read();
        long offset = input.readLong();
        if (!checkForFs(client)) {
            return;
        }
        if (receiveTarget != null) {
            try {
                receiveTarget.close();
            } catch (Exception e) {
            }
        }
		if(LOG.isDebugEnabled())
			LOG.debug("Receiving " + receivePath);
        boolean overwrite = (flags & 0x1) != 0;
        receiveTarget = fs.receive(receivePath, overwrite, offset);
        synchronized (client.getWriteLock()) {
            output.writeUInt32(RFBConstants.CAP_FTSFURLY_CODE);
            output.flush();
        }
    }

    protected void endReceive() throws IOException {
        int flags = input.readShort();
        long lastMod = input.readLong();
        if (!checkForFs(client)) {
            return;
        }
        if (receiveTarget == null) {
            error("Not uploading.");
            return;
        }
		if(LOG.isDebugEnabled())
			LOG.debug("Ending receive");
        try {
            receiveTarget.close();
            RFBFile f = fs.get(receivePath);
            f.setLastWriteTime(lastMod);
            synchronized (client.getWriteLock()) {
                output.writeUInt32(RFBConstants.CAP_FTSUERLY_CODE);
                output.flush();
            }
        } catch (IOException ioe) {
            error(ioe);
            return;
        }
    }

    protected void mv() throws IOException {

        String oldPath = input.readTerminatedString();
        String newPath = input.readTerminatedString();
        if (!checkForFs(client)) {
            return;
        }
        try {
    		if(LOG.isDebugEnabled())
    			LOG.debug("Moving " + oldPath + " to " + newPath);
            fs.mv(oldPath, newPath);
        } catch (IOException ioe) {
            error(ioe);
            return;
        }
        synchronized (client.getWriteLock()) {
            output.writeUInt32(RFBConstants.CAP_FTSFMRLY_CODE);
            output.flush();
        }

    }

    protected void rm() throws IOException {

        String path = input.readTerminatedString();
        if (!checkForFs(client)) {
            return;
        }
        try {
    		if(LOG.isDebugEnabled())
    			LOG.debug("Removing " + path);
            fs.rm(path);
        } catch (IOException ioe) {
            error(ioe);
            return;
        }
        synchronized (client.getWriteLock()) {
            output.writeUInt32(RFBConstants.CAP_FTSFTRLY_CODE);
            output.flush();
        }
    }

    protected void mkdir() throws IOException {
        String path = input.readTerminatedString();
        if (!checkForFs(client)) {
            return;
        }
        try {
            if (!fs.mkdir(path)) {
                throw new IOException("Folder creation refused.");
            }
        } catch (IOException ioe) {
            error(ioe);
            return;
        }
        synchronized (client.getWriteLock()) {
            output.writeUInt32(RFBConstants.CAP_FTSMDRLY_CODE);
            output.flush();
        }

    }

    protected void fileListRequest() throws IOException {
        // Request request
        int compression = input.read();
        String path = input.readTerminatedString();

        if (!checkForFs(client)) {
            return;
        }

        ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
        try {
            // Get files for underlying file systems
            RFBFile[] files = fs.list(path);

            // Build the compressable message
            ProtocolWriter pw = new ProtocolWriter(tempOut);
            pw.writeUInt32(files.length);
            for (RFBFile f : files) {
                pw.writeLong(f.getSize());
                pw.writeLong(f.getLastWriteTime());
                pw.writeShort(getFlagsForFile(f));
                pw.writeUTF8String(f.getName());
            }
        } catch (IOException ioe) {
            // Return ERROR
            error(ioe);
            return;
        }

        int uncompressedSize = tempOut.size();
        int compressedSize = uncompressedSize;
        byte[] compressedOut;
        ;
        if (compression != 0) {
            throw new UnsupportedOperationException();
        } else {
            compressedOut = tempOut.toByteArray();
        }

        synchronized (client.getWriteLock()) {
            output.writeUInt32(RFBConstants.CAP_FTSFLRLY_CODE);
            output.write(compression);
            output.writeUInt32(compressedSize);
            output.writeUInt32(uncompressedSize);
            output.write(compressedOut);
            output.flush();
        }

    }

    private boolean checkForFs(RFBClient rfbClient) throws IOException {
        // Check for file system
        RFBServerFS fs = rfbClient.getFs();
        if (fs == null) {
            error("No file system.");
            return false;
        }
        return true;
    }

    protected void error(Exception error) throws IOException {
        LOG.error("Failed.", error);
        writeError(error.getMessage() == null ? error.getClass().getName() : error.getMessage());
    }

    protected void error(String error) throws IOException {
        writeError(error);
    }

    private void writeError(String error) throws IOException, UnsupportedEncodingException {
        LOG.error("Failed. " + error);
        synchronized (client.getWriteLock()) {
            output.writeUInt32(RFBConstants.CAP_FTSDSRLY_CODE);
            output.writeUTF8String(error);
            output.flush();
        }
    }

    protected short getFlagsForFile(RFBFile file) {
        int flgs = 0;
        if (file.isFolder()) {
            flgs = flgs | 0x1;
        }
        if (file.isExecutable()) {
            flgs = flgs | 0x2;
        }
        return (short) flgs;
    }
}
