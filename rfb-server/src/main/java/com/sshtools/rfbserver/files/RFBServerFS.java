package com.sshtools.rfbserver.files;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.rfbcommon.RFBFile;

public interface RFBServerFS {
    RFBFile[] getRoots() throws IOException;
    RFBFile[] list(String path) throws IOException;
    boolean mkdir(String filename) throws IOException;
    void rm(String path) throws IOException;
    void mv(String oldPath, String newPath) throws IOException;
    OutputStream receive(String path, boolean overwrite, long offset) throws IOException;
    InputStream retrieve(String path, long offset) throws IOException;
    RFBFile get(String sendingPath) throws IOException;
}
