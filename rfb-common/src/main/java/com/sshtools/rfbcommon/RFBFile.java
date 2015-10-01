/**
 * 
 */
package com.sshtools.rfbcommon;

public interface RFBFile {
    int getFileAttributes();

    long getCreationTime();

    long getLastAccessTime();

    long getLastWriteTime();

    boolean isFolder();

    long getSize();

    String getName();

    String getAlternateName();

    boolean isExecutable();

    boolean setLastWriteTime(long lastWriteTime);
}