/**
 * RFB - Remote Frame Buffer (VNC) implementation.
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
package com.sshtools.rfb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.rfbcommon.RFBFile;

public interface RFBFS {

	boolean isActive();

	boolean mkdir(String filename) throws IOException;

	RFBFile[] list(String filename) throws IOException;

	boolean rm(String processPath) throws IOException;

	RFBFile stat(String filename) throws IOException;

	boolean handleReply(int type) throws IOException;

	void mv(String oldName, String newName) throws IOException;

	OutputStream send(String path, boolean overwrite, long offset) throws IOException;

	InputStream receive(String processPath, long filePointer) throws IOException;
}
