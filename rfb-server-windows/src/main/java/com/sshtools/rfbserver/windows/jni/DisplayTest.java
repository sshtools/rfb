/**
 * RFB Server (Windows Driver) - A JNA based driver for Windows,
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
package com.sshtools.rfbserver.windows.jni;

public class DisplayTest implements DisplayCallback {

	public static void main(String[] args) {
		DisplayTest dy = new DisplayTest();
		DisplayHook.INSTANCE.register(dy);
		DisplayHook.INSTANCE.loop();
	}

	public void windowMoved(int hwnd, int x, int y, int width, int height) {
	}

	public void windowCreated(int hwnd, int x, int y, int width, int height) {
	}

	public void windowDestroyed(int hwnd) {
	}
}
