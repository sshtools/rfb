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
//
// HookTest.cpp
//
#include <windows.h>
#include "com_sshtools_rfbserver_windows_jni_HookTest.h"

HINSTANCE hInst = NULL;
JavaVM * jvm = NULL;
jobject hookObj = NULL;
jmethodID processKeyID = NULL;
DWORD hookThreadId = 0;
HHOOK callWndHook = NULL;

BOOL APIENTRY DllMain(HINSTANCE _hInst, DWORD reason, LPVOID
		reserved) {
	switch (reason) {
		case DLL_PROCESS_ATTACH:
		printf("C++: DllMain - DLL_PROCESS_ATTACH.\n");
		hInst = _hInst;
		break;
		default:
		break;
	}

	return TRUE;
}

LRESULT CALLBACK LowLevelKeyboardProc(int nCode, WPARAM wParam, LPARAM lParam) {
	JNIEnv * env;
	KBDLLHOOKSTRUCT * p = (KBDLLHOOKSTRUCT *) lParam;

	if (jvm->AttachCurrentThread((void **) &env, NULL) >= 0) {
		switch (wParam) {
		case WM_KEYDOWN:
		case WM_SYSKEYDOWN:
			printf("C++: LowLevelKeyboardProc - Key pressed\n");
			env->CallVoidMethod(hookObj, processKeyID, p->vkCode, true);
			break;
		case WM_KEYUP:
		case WM_SYSKEYUP:
			printf("C++: LowLevelKeyboardProc - Key released\n");
			env->CallVoidMethod(hookObj, processKeyID, p->vkCode, false);
			break;
		default:
			break;
		}
	} else {
		printf(
				"C++: LowLevelKeyboardProc - Error on the attach current thread.\n");
	}

	return CallNextHookEx(NULL, nCode, wParam, lParam);
}

void MsgLoop() {
	MSG message;

	while (GetMessage(&message, NULL, 0, 0)) {
		TranslateMessage(&message);
		DispatchMessage(&message);
	}
}

LRESULT CALLBACK handleCallWndProc(int nCode, WPARAM wParam, LPARAM lParam) {
	if (nCode == HC_ACTION) {
			printf("C++: Message!\n");
	}
	return CallNextHookEx(callWndHook, nCode, wParam, lParam);
}

JNIEXPORT void JNICALL Java_com_sshtools_rfbserver_windows_jni_HookTest_registerHook
 (JNIEnv * env, jobject obj) {
	HHOOK hookHandle = SetWindowsHookEx(WH_KEYBOARD_LL,
			LowLevelKeyboardProc, hInst, 0);

	if (hookHandle == NULL) {
		printf("C++: Java_HookTest_registerHook - Hook failed!\n");
		return;
	}
	else {
		printf("C++: Java_HookTest_registerHook - Hook successful\n");
	}

	hookObj = env->NewGlobalRef(obj);
	jclass cls = env->GetObjectClass(hookObj);
	processKeyID = env->GetMethodID(cls, "processKey", "(IZ)V");
	env->GetJavaVM(&jvm);
	hookThreadId = GetCurrentThreadId();

	callWndHook = SetWindowsHookEx(WH_CALLWNDPROC, (HOOKPROC) handleCallWndProc,
			hInst, 0L);
	if (callWndHook == NULL) {
		printf("C++: Failed to set WH_CALLWNDPROC!\n");
		return;
	}

	MsgLoop();

	if (!UnhookWindowsHookEx(hookHandle))
	printf("C++: Java_HookTest_registerHook - Unhook failed\n");

	else
	printf("C++: Java_HookTest_registerHook - Unhook successful\n");
}

JNIEXPORT void JNICALL Java_com_sshtools_rfbserver_windows_jni_HookTest_unRegisterHook
  (JNIEnv * env, jobject obj) {
	if (hookThreadId == 0)
	return;

	printf("C++: Java_HookTest_unRegisterHook - call PostThreadMessage.\n");
	PostThreadMessage(hookThreadId, WM_QUIT, 0, 0L);
}
