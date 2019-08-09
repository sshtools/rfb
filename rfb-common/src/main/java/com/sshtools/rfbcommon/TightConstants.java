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

public interface TightConstants {
	final static int OP_FILL = 0x08;
	final static int OP_JPEG = 0x09;
	final static int OP_PNG  = 0x0A;
	final static int OP_COPY  = 0x00;
	
	final static int OP_READ_FILTER_ID = 0x40;
	
	final static int OP_FILTER_RAW = 0x00;
	final static int OP_FILTER_PALETTE = 0x01;
	final static int OP_FILTER_GRADIENT = 0x02;
}
