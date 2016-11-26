package com.sshtools.rfbserver.windows.jni;

public class DisplayHook {

	static {
		System.loadLibrary("displayhook");
	}
	
	public final static DisplayHook INSTANCE = new DisplayHook();

	public native boolean register(DisplayCallback target);
	public native void loop();
}
