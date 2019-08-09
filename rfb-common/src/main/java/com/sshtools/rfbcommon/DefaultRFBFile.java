/**
 * RFB Common - Remote Frame Buffer common code used both in client and server.
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
package com.sshtools.rfbcommon;

public class DefaultRFBFile implements RFBFile {
    private boolean folder;
    private long size;
    private String name;
    private int fileAttributes;
    private long creationTime;
    private long lastAccessTime;
    private long lastWriteTime;
    private String alternateName;
    private boolean executable;

    public DefaultRFBFile(boolean folder, long size, String name, int fileAttributes, long creationTime, long lastAccessTime,
                          long lastWriteTime) {
        super();
        this.folder = folder;
        this.size = size;
        this.name = name;
        this.fileAttributes = fileAttributes;
        this.creationTime = creationTime;
        this.lastAccessTime = lastAccessTime;
        this.lastWriteTime = lastWriteTime;
    }

    public void setExecutable(boolean executable) {
        this.executable = executable;
    }

    public int getFileAttributes() {
        return fileAttributes;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public long getLastWriteTime() {
        return lastWriteTime;
    }

    public boolean isFolder() {
        return folder;
    }

    public long getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "RFBFile [creationTime=" + creationTime + ", fileAttributes=" + fileAttributes + ", folder=" + folder
                        + ", lastAccessTime=" + lastAccessTime + ", lastWriteTime=" + lastWriteTime + ", name=" + name + ", size="
                        + size + "]";
    }

    protected void setFolder(boolean folder) {
        this.folder = folder;
    }

    protected void setSize(long size) {
        this.size = size;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected void setFileAttributes(int fileAttributes) {
        this.fileAttributes = fileAttributes;
    }

    protected void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    protected void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public boolean setLastWriteTime(long lastWriteTime) {
        this.lastWriteTime = lastWriteTime;
        return true;
    }

    protected void setAlternateName(String alternateName) {
        this.alternateName = alternateName;
    }

    public String getAlternateName() {
        return alternateName;
    }

    public boolean isExecutable() {
        return executable;
    }
}