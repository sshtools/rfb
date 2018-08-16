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
package com.sshtools.rfbserver.windows;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.x.ExtUser32;
 
import com.sshtools.rfbserver.drivers.RobotDisplayDriver;
import com.sshtools.rfbserver.windows.jni.DisplayCallback;
import com.sshtools.rfbserver.windows.jni.DisplayHook;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef.HMODULE;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.HOOKPROC;
import com.sun.jna.platform.win32.WinUser.WINDOWINFO;

public class Win32DisplayDriver extends RobotDisplayDriver {

	final static ExtUser32 INSTANCE = ExtUser32.INSTANCE;

	final static Logger LOG = LoggerFactory.getLogger(Win32DisplayDriver.class);

	private static HHOOK TEST_callWndHook;
	private HHOOK callWndHook;
	private HHOOK getMessageHook;
	private HHOOK dialogsMessageHook;
	private Map<String, Window> windows = new HashMap<String, Window>();

	public static void main(String[] args) throws Exception {

		HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);
		System.out.println(hMod);
		// Get initital windows
		ExtUser32.WNDENUMPROC winenum = new ExtUser32.WNDENUMPROC() {
			public boolean callback(HWND hWnd, Pointer data) {
				// System.out.println(hWnd);

				WINDOWINFO winfo = new WINDOWINFO();
				if (!INSTANCE.GetWindowInfo(hWnd, winfo)) {
					throw new RuntimeException("Failed to get window info.");
				}
				// System.out.println(" Gotinfo");
				int titleLength = INSTANCE.GetWindowTextLength(hWnd);
				char[] title = new char[titleLength];
				INSTANCE.GetWindowText(hWnd, title, titleLength);
				// System.out.println(" Tit: " + new String(title));
				// System.out
				// .println(" Cor: " + coordsToRectangle(winfo.rcWindow));

				// TODO recurse?
				return true;
			}
		};
		if (!INSTANCE.EnumWindows(winenum, null)) {
			throw new Exception("Failed to list windows.");
		}

		if (!DisplayHook.INSTANCE.register(new DisplayCallback() {

			public void windowMoved(int hwnd, int x, int y, int width,
					int height) {
				System.out.println("moved: >> " + hwnd + " x: " + x + " y: " + y
						+ " w:" + width + " h: " + height);
			}

			public void windowCreated(int hwnd, int x, int y, int width,
					int height) {
				System.out.println("created: >> " + hwnd + " x: " + x + " y: " + y
						+ " w:" + width + " h: " + height);
				
			}

			public void windowDestroyed(int hwnd) {
				System.out.println("destroyed: " + hwnd);				
			}
		})) {
			throw new Exception("Failed to initialise display hooks");
		}
		;
		// DisplayHook.INSTANCE.test();
		JFrame f = new JFrame("Test");
		f.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().add(new JLabel("Hello!"), BorderLayout.CENTER);
		f.setVisible(true);

	}

	// @Override
	public void init() throws Exception {
		super.init();

		HOOKPROC lpfn;
		HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);

		// Get initital windows
		ExtUser32.WNDENUMPROC winenum = new ExtUser32.WNDENUMPROC() {
			public boolean callback(HWND hWnd, Pointer data) {
				Window window = addWindow(hWnd);
				// TODO recurse?
				return true;
			}
		};
		if (!INSTANCE.EnumWindows(winenum, null)) {
			throw new Exception("Failed to list windows.");
		}

		// CallWnd hook
		// lpfn = new ExtUser32.CallWndProc() {
		// public LRESULT callback(int nCode, WPARAM wParam, CWPSTRUCT lParam) {
		// if (nCode == ExtUser32.HC_ACTION) {
		// handleMessage(lParam.message, lParam.hwnd, lParam.wParam,
		// lParam.lParam);
		// }
		// return INSTANCE.CallNextHookEx(callWndHook, nCode, wParam,
		// lParam.getPointer());
		// }
		// };
		// callWndHook = INSTANCE.SetWindowsHookEx(ExtUser32.WH_CALLWNDPROC,
		// lpfn,
		// hMod, 0);
		// if (callWndHook == null) {
		// throw new Exception("Failed to SetWindowsHookEx");
		// }
		//
		// // Window messages
		// lpfn = new ExtUser32.GetMsgProc() {
		// public LRESULT callback(int nCode, WPARAM wParam, CWPSTRUCT lParam) {
		// if (nCode == ExtUser32.HC_ACTION) {
		// handleMessage(lParam.message, lParam.hwnd, lParam.wParam,
		// lParam.lParam);
		// }
		// return INSTANCE.CallNextHookEx(callWndHook, nCode, wParam,
		// lParam.getPointer());
		// }
		// };
		// getMessageHook = INSTANCE.SetWindowsHookEx(ExtUser32.WH_GETMESSAGE,
		// lpfn, hMod, 0);
		//
		// // Dialogs, menus and popup messages
		// lpfn = new ExtUser32.SysMsgProc() {
		// public LRESULT callback(int nCode, WPARAM wParam, CWPSTRUCT lParam) {
		// if (nCode == ExtUser32.HC_ACTION) {
		// handleMessage(lParam.message, lParam.hwnd, lParam.wParam,
		// lParam.lParam);
		// }
		// return INSTANCE.CallNextHookEx(callWndHook, nCode, wParam,
		// lParam.getPointer());
		// }
		// };
		// dialogsMessageHook = INSTANCE.SetWindowsHookEx(
		// ExtUser32.WH_SYSMSGFILTER, lpfn, hMod, 0);

	}

	private void handleMessage(int message, HWND hwnd, WPARAM wParam,
			LPARAM lParam) {
		switch (message) {
		case ExtUser32.WM_CREATE:
			windowCreate(hwnd);
			break;
		case ExtUser32.WM_DESTROY:
			windowDestroy(hwnd);
			break;
		case ExtUser32.WM_WINDOWPOSCHANGING:
			windowMoved(hwnd);
			break;
		}
	}

	private void windowDestroy(HWND hwnd) {
		Window window = windows.remove((Integer) hwnd.toNative());
		LOG.info("Destroyed window " + window.name);
		fireWindowClosed("", window.bounds);
		fireDamageEvent("", window.bounds, -1);
	}

	private void windowCreate(HWND hwnd) {
		Window window = addWindow(hwnd);
		fireWindowCreated(getWindowName(hwnd), window.bounds);
		fireDamageEvent("", window.bounds, -1);
	}

	private Window addWindow(HWND hwnd) {
		WINDOWINFO winfo = new WINDOWINFO();
		if (!INSTANCE.GetWindowInfo(hwnd, winfo)) {
			throw new RuntimeException("Failed to get window info.");
		}
		int titleLength = INSTANCE.GetWindowTextLength(hwnd);
		char[] title = new char[titleLength];
		INSTANCE.GetWindowText(hwnd, title, titleLength);
		Window window = new Window();
		window.name = getWindowName(hwnd);
		window.bounds = coordsToRectangle(winfo.rcWindow);
		windows.put(getWindowName(hwnd), window);
		LOG.info("Created window " + window.name);
		return window;
	}

	private String getWindowName(HWND hwnd) {
		Pointer pointer = (Pointer) hwnd.toNative();
		System.out.println("Window name of " + hwnd + " is " + pointer + ". ");
		return String.valueOf(hwnd);
	}

	private void windowMoved(HWND hwnd) {
		if (INSTANCE.IsWindowVisible(hwnd)) {
			RECT rect = new RECT();
			INSTANCE.GetWindowRect(hwnd, rect);
			Rectangle newBounds = coordsToRectangle(rect);
			Window window = windows.get(getWindowName(hwnd));
			Rectangle oldBounds = (Rectangle) window.bounds.clone();
			LOG.info("Actual window move" + newBounds + " from "
					+ window.bounds);
			window.bounds.setBounds(newBounds);
			fireWindowMoved(getWindowName(hwnd), oldBounds, newBounds);
		}
	}

	private static Rectangle coordsToRectangle(RECT rect) {
		Rectangle newBounds = new Rectangle(rect.left, rect.top, rect.right
				- rect.left, rect.bottom - rect.top);
		return newBounds;
	}

	class Window {
		String name;
		Rectangle bounds;
	}

	// public PointerShape getPointerShape() {
	// ExtUser32 u32 = ExtUser32.INSTANCE;
	// CURSORINFO cursorInfo = new CURSORINFO();
	// if(!u32.GetCursorInfo(cursorInfo)) {
	// LOG.warn("GetCusorInfo failed, reverting to default cursor");
	// return super.getPointerShape();
	// }
	// HCURSOR cursorHandle = cursorInfo.hCursor;
	// ICONINFO iconInfo = new ICONINFO();
	// if(!u32.GetIconInfo(cursorHandle, iconInfo)) {
	// LOG.warn("GetIconInfo failed, reverting to default cursor");
	// return super.getPointerShape();
	// }
	//
	// if (iconInfo.hbmMask == null) {
	// LOG.warn("No cursor mask, reverting to default cursor");
	// return super.getPointerShape();
	// }
	//
	// boolean isColorShape = (iconInfo.hbmColor != null);
	//
	// ExtGDI32 gdi = ExtGDI32.INSTANCE;
	// ExtGDI32.BITMAP bmMask = new ExtGDI32.BITMAP();
	// if(gdi.GetObject(iconInfo.hbmMask, bmMask.size(), bmMask.getPointer()) ==
	// 0) {
	// gdi.DeleteObject(iconInfo.hbmMask);
	// }

	// int width = bmMask.bmiHeader.biWidth;
	// int widthBytes = bmMask.bmiHeader.biB

	// BITMAP bmMask;
	// if (!GetObject(iconInfo.hbmMask, sizeof(BITMAP), (LPVOID)&bmMask)) {
	// DeleteObject(iconInfo.hbmMask);
	// return false;
	// }
	//
	// if (bmMask.bmPlanes != 1 || bmMask.bmBitsPixel != 1) {
	// DeleteObject(iconInfo.hbmMask);
	// return false;
	// }
	//
	// m_cursorShape.setHotSpot(iconInfo.xHotspot, iconInfo.yHotspot);
	//
	// int width = bmMask.bmWidth;
	// int height = isColorShape ? bmMask.bmHeight : bmMask.bmHeight/2;
	// int widthBytes = bmMask.bmWidthBytes;

	// PointerShape c = new PointerShape();
	// c.setWidth(iconInfo.s.width);
	// c.setHeight(s.height);
	// c.setHotX(iconInfo.xHotspot);
	// c.setHotY(iconInfo.yHotspot);
	// // c.setX(s.x);
	// // c.setY(s.y);
	//
	// PointerInfo pm = MouseInfo.getPointerInfo();
	// Point location = pm.getLocation();
	// c.setX(location.x);
	// c.setY(location.y);
	//
	// // Turn the ARGB cursor image into TYPE_INT_ARGB we work with
	// ByteBuffer buf = s.pixels.getPointer().getByteBuffer(0, s.width *
	// s.height * NativeLong.SIZE);
	// buf.order(ByteOrder.LITTLE_ENDIAN);
	// BufferedImage bim = new BufferedImage(s.width, s.height,
	// BufferedImage.TYPE_INT_ARGB);
	// WritableRaster raster = bim.getRaster();
	// for (int y = 0; y < s.height; y++) {
	// for (int x = 0; x < s.width; x++) {
	// long z = NativeLong.SIZE == 8 ? buf.getLong() : buf.getInt();
	// int b = (int) ((z >> 24) & 0xFF);
	// int a = (int) ((z >> 16) & 0xFF);
	// int g = (int) ((z >> 8) & 0xFF);
	// int r = (int) (z & 0xFF);
	// raster.setPixel(x, y, new int[] { a, r, g, b });
	// }
	// }
	// c.setData(bim);
	// return c;
	// }

}
