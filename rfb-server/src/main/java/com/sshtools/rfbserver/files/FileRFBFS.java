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
package com.sshtools.rfbserver.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.sshtools.rfbcommon.RFBFile;
import com.sshtools.rfbserver.files.uvnc.RFBDrive;

public class FileRFBFS implements RFBServerFS {

    final static Logger LOG = Logger.getLogger(FileRFBFS.class.getName());
    private File root;

    public FileRFBFS() {
        this(new File(System.getProperty("user.dir")));
    }

    public FileRFBFS(File root) {
        this.root = root;
    }

    public RFBFile[] getRoots() throws IOException {
        LOG.info("Listing drives");
        File[] roots = File.listRoots();
        List<RFBFile> drives = new ArrayList<RFBFile>();
        for (File f : roots) {
            FileRFBFile d = new FileRFBFile(f);
            LOG.info("Found drive " + d);
            drives.add(d);
        }
        return drives.toArray(new RFBFile[0]);
    }

    public RFBFile[] list(String path) throws IOException {
        List<RFBFile> files = new ArrayList<RFBFile>();
        File actual = getFileRelativeToRoot(path);
        LOG.info("Listing files in " + path + " (" + actual + ")");
        File[] listFiles = actual.listFiles();
        if (listFiles != null) {
            for (File f : listFiles) {
                FileRFBFile e = new FileRFBFile(f);
                LOG.info("Found file/folder " + e);
                files.add(e);
            }
        } else {
            LOG.severe("Failed to list " + path + ". Probably permissions.");
        }
        return files.toArray(new RFBFile[0]);
    }

    File getFileRelativeToRoot(String path) {
        path = path.replace('/', File.separatorChar);
        if (path.equals(File.separator)) {
            return root;
        } else {
            File f = new File(root, path.substring(1));
            // Check it is actually a child of root to prevent shennanigans with
            // paths
            boolean parented = false;
            try {
                File parentFile = f;
                do {
                    parentFile = parentFile.getCanonicalFile().getParentFile();
                    if (parentFile != null && parentFile.equals(root.getCanonicalFile())) {
                        parented = true;
                        break;
                    }
                } while (parentFile != null);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            if (!parented) {
                throw new IllegalArgumentException("Path wanders outside of root.");
            }
            return f;
        }
    }

    class FileRFBDrive extends RFBDrive {
        private File root;

        FileRFBDrive(File root, char drv) {
            setName(drv + ":");
            setType(RFBDrive.LOCAL_DISK);
            this.root = root;
        }
    }

    class FileRFBFile implements RFBFile {
        private File file;

        public FileRFBFile(File f) {
            this.file = f;
        }

        public boolean setLastWriteTime(long lastWriteTime) {
            return file.setLastModified(lastWriteTime);
        }

        public int getFileAttributes() {
            return 0;
        }

        public long getCreationTime() {
            return 0;
        }

        public long getLastAccessTime() {
            return 0;
        }

        public long getLastWriteTime() {
            return file.lastModified();
        }

        public boolean isFolder() {
            return file.isDirectory();
        }

        public long getSize() {
            return file.length();
        }

        public String getName() {
            return file.getName();
        }

        public String getAlternateName() {
            return file.getName();
        }

        public boolean isExecutable() {
            return file.canExecute();
        }
    }

    public boolean mkdir(String filename) throws IOException {
        File actual = getFileRelativeToRoot(filename);
        if (actual.exists()) {
            throw new IOException("Directory already exists.");
        }
        return actual.mkdirs();
    }

    public void rm(String path) throws IOException {
        File actual = getFileRelativeToRoot(path);
        if (!actual.exists()) {
            throw new IOException("Path does not exist.");
        }
        removeRecursive(Paths.get(actual.toURI()));
    }

    public static void removeRecursive(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                // try to delete the file anyway, even if its attributes
                // could not be read, since delete-only access is
                // theoretically possible
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    // directory iteration failed; propagate exception
                    throw exc;
                }
            }
        });
    }

    public void mv(String oldPath, String newPath) throws IOException {
        LOG.info("Moving " + oldPath + " to " + newPath);
        File oldFile = getFileRelativeToRoot(oldPath);
        File newFile = getFileRelativeToRoot(newPath);
        if (!oldFile.renameTo(newFile)) {
            throw new IOException("Refused to rename file.");
        }
    }

    public OutputStream receive(String path, boolean overwrite, long offset) throws IOException {
        LOG.info("Receiving " + path + " from " + offset);
        File file = getFileRelativeToRoot(path);
        if (!overwrite && file.exists()) {
            throw new IOException("File already exists.");
        }
        final RandomAccessFile fas = new RandomAccessFile(file, "rw");
        fas.seek(offset);
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                fas.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                fas.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                fas.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public void close() throws IOException {
                fas.close();
            }

        };
    }

    public InputStream retrieve(String path, long offset) throws IOException {
        LOG.info("Retrieving " + path + " from " + offset);
        File file = getFileRelativeToRoot(path);
        FileInputStream fin = new FileInputStream(file);
        if (offset > 0) {
            fin.skip(offset);
        }
        return fin;
    }

    public RFBFile get(String sendingPath) throws IOException {
        return new FileRFBFile(getFileRelativeToRoot(sendingPath));
    }
}
