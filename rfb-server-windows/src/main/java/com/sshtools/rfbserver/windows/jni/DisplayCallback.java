package com.sshtools.rfbserver.windows.jni;

public interface DisplayCallback {
	void windowMoved(int hwnd, int x, int y, int width, int height);
	void windowCreated(int hwnd, int x, int y, int width, int height);
	void windowDestroyed(int hwnd);
}