package com.sshtools.rfbserver.windows.jni;

//
// HookTest.java
//
public class HookTest {
	static {
		System.loadLibrary("HookTest");
	}

	void processKey(int key, boolean pressed) {
		System.out.println("Java: HookTest.processKey - key = " + key
				+ (pressed ? " pressed" : " released"));
	}

	native void registerHook();

	native void unRegisterHook();
}