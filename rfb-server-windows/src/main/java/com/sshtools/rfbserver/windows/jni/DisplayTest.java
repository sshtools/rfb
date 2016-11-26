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
