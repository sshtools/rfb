#include <stdio.h>
#include <stdlib.h>
#include <windows.h>
#include <iostream>
#include "displayhook.h"
#include "com_sshtools_rfbserver_windows_jni_DisplayHook.h"
# include <tchar.h>

//http://www.experts-exchange.com/Programming/Languages/Java/Q_23053697.html

#pragma data_seg(".HOOKDATA") //Shared data among all instances.// http://adamish.com/blog/archives/327 - callback to java from native// http://www.mingw.org/wiki/sampleDLL - mingw dlls

// cached refs for later callbacks
JavaVM * g_vm;
jobject g_obj;

// Callback methods
jmethodID g_windowMovedMethod;
jmethodID g_windowCreatedMethod;
jmethodID g_windowDestroyedMethod;

HWND rootWindow = 0;

HINSTANCE instance = NULL;
HHOOK callWndHook = NULL;
HHOOK sysMsgFilterHook = NULL;
HHOOK getMessageHook = NULL;
HHOOK cbtHook = NULL;

#pragma data_seg()
#pragma comment(linker, "/SECTION:.HOOKDATA,RWS")

LRESULT CALLBACK handleCbtProc(int nCode, WPARAM wParam, LPARAM lParam);
LRESULT CALLBACK handleCallWndProc(int nCode, WPARAM wParam, LPARAM lParam);
LRESULT CALLBACK handleGetMessageProc(int nCode, WPARAM wParam, LPARAM lParam);
LRESULT CALLBACK handleSysMsgFilterProc(int nCode, WPARAM wParam,
		LPARAM lParam);

/*
 * DLL entry point
 */
BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD reason, LPVOID lpvReserved) {
	switch (reason) {
	case DLL_PROCESS_ATTACH:
		std::cout << "DLL Attached" << hinstDLL << std::endl;
		instance = hinstDLL;
		return TRUE;
	case DLL_PROCESS_DETACH:
		std::cout << "DLL Detached" << std::endl;
		return TRUE;
	case DLL_THREAD_ATTACH:
		std::cout << "DLL THREAD Attached" << std::endl;
		return TRUE;
	case DLL_THREAD_DETACH:
		std::cout << "DLL THREAD Detached" << std::endl;
		return TRUE;
	default:
		return TRUE;
	}
}

/*
 * Register the object that will receive the callbacks.
 */
JNIEXPORT jboolean JNICALL Java_com_sshtools_rfbserver_windows_jni_DisplayHook_register(
		JNIEnv * env, jobject javaObj, jobject targetObj) {
	// Reference to VM
	env->GetJavaVM(&g_vm);

	// The object that receives the callbacks
	g_obj = env->NewGlobalRef(targetObj);

	// The class of the object that receives the callbacks
	jclass g_clazz = env->GetObjectClass(g_obj);
	if (g_clazz == NULL) {
		std::cout << "Failed to find class" << std::endl;
	}

	// The windowMoved callback
	g_windowMovedMethod = env->GetMethodID(g_clazz, "windowMoved", "(IIIII)V");
	if (env->ExceptionCheck()) {
		std::cout << "Failed to get windowMoved method." << std::endl;
		return false;
	}

	// The windowCreated callback
	g_windowCreatedMethod = env->GetMethodID(g_clazz, "windowCreated",
			"(IIIII)V");
	if (env->ExceptionCheck()) {
		std::cout << "Failed to get windowCreated method." << std::endl;
		return false;
	}

	// The windowDestroyed callback
	g_windowDestroyedMethod = env->GetMethodID(g_clazz, "windowDestroyed",
			"(I)V");
	if (env->ExceptionCheck()) {
		std::cout << "Failed to get windowDestroyed method." << std::endl;
		return false;
	}

	// Hook into CALLWNDPROC
//	HWND windowHandle = FindWindow(NULL, _T("Command Prompt"));
//	DWORD threadId = GetWindowThreadProcessId(windowHandle, NULL);
	//	callWndHook = SetWindowsHookEx(WH_CALLWNDPROC, (HOOKPROC) handleCallWndProc,
	//				NULL, threadId);
	callWndHook = SetWindowsHookEx(WH_CALLWNDPROC, (HOOKPROC) handleCallWndProc,
			instance, 0L);
	if (callWndHook == NULL) {
		std::cout << "Failed to set WH_CALLWNDPROC" << std::endl;
		return false;
	}

	// Hook into WH_CBT - needed???
	cbtHook = SetWindowsHookEx(WH_CBT, (HOOKPROC) handleCbtProc, instance, 0L);
	if (cbtHook == NULL) {
		std::cout << "Failed to set WH_CBT" << std::endl;
		return false;
	}

	// Hook into WH_GETMESSAGE
	getMessageHook = SetWindowsHookEx(WH_GETMESSAGE,
			(HOOKPROC) handleGetMessageProc, instance, 0L);
	if (getMessageHook == NULL) {
		std::cout << "Failed to set WH_GETMESSAGE" << std::endl;
		return false;
	}

	// Hook into WH_SYSMSGFILTER
	sysMsgFilterHook = SetWindowsHookEx(WH_SYSMSGFILTER,
			(HOOKPROC) handleSysMsgFilterProc, instance, 0L);
	if (sysMsgFilterHook == NULL) {
		std::cout << "Failed to set WH_SYSMSGFILTER" << std::endl;
		return false;
	}

	if (callWndHook != NULL && getMessageHook != NULL
			&& sysMsgFilterHook != NULL) {
		HDESK desk = OpenInputDesktop(0, TRUE, READ_CONTROL);
		SetThreadDesktop(desk);
		rootWindow = GetDesktopWindow();
	}

	return true;
}

JNIEXPORT void JNICALL Java_com_sshtools_rfbserver_windows_jni_DisplayHook_loop
(JNIEnv * env, jobject javaObject) {
	MSG msg;
	while(GetMessage(&msg, NULL, 0, 0) > 0)	{
		std::cout << "Message" << std::endl;
		TranslateMessage(&msg);
		DispatchMessage(&msg);
	}
}

/*
 * Invoke the Java method on the target object
 */
void windowMoved(HWND hWnd, RECT *bounds) {
	JNIEnv * g_env;
	int getEnvStat = g_vm->GetEnv((void **) &g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED) {
		std::cout << "Attaching current thread" << std::endl;
		if (g_vm->AttachCurrentThread((void **) &g_env, NULL) != 0) {
			std::cout << "Failed to attach" << std::endl;
		}
	} else if (getEnvStat == JNI_OK) {
		//
	} else if (getEnvStat == JNI_EVERSION) {
		std::cout << "GetEnv: version not supported" << std::endl;
	}

	g_env->CallVoidMethod(g_obj, g_windowMovedMethod, hWnd, bounds->left,
			bounds->top, bounds->right - bounds->left,
			bounds->bottom - bounds->top);
	if (g_env->ExceptionCheck()) {
		g_env->ExceptionDescribe();
	}
	g_vm->DetachCurrentThread();
}

/*
 * Invoke the Java method on the target object
 */
void windowCreated(HWND hWnd, RECT *bounds) {
	JNIEnv * g_env;
	int getEnvStat = g_vm->GetEnv((void **) &g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED) {
		std::cout << "Attaching current thread" << std::endl;
		if (g_vm->AttachCurrentThread((void **) &g_env, NULL) != 0) {
			std::cout << "Failed to attach" << std::endl;
		}
	} else if (getEnvStat == JNI_OK) {
		//
	} else if (getEnvStat == JNI_EVERSION) {
		std::cout << "GetEnv: version not supported" << std::endl;
	}
	g_env->CallVoidMethod(g_obj, g_windowCreatedMethod, hWnd, bounds->left,
			bounds->top, bounds->right - bounds->left,
			bounds->bottom - bounds->top);
	if (g_env->ExceptionCheck()) {
		g_env->ExceptionDescribe();
	}
	g_vm->DetachCurrentThread();
}

void windowDestroyed(HWND hWnd) {
	JNIEnv * g_env;
	int getEnvStat = g_vm->GetEnv((void **) &g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED) {
		if (g_vm->AttachCurrentThread((void **) &g_env, NULL) != 0) {
			std::cout << "Failed to attach" << std::endl;
		}
	} else if (getEnvStat == JNI_OK) {
		//
	} else if (getEnvStat == JNI_EVERSION) {
		std::cout << "GetEnv: version not supported" << std::endl;
	}
	g_env->CallVoidMethod(g_obj, g_windowDestroyedMethod, hWnd);
	if (g_env->ExceptionCheck()) {
		g_env->ExceptionDescribe();
	}
	g_vm->DetachCurrentThread();
}

BOOL handleMessage(UINT messageId, HWND hWnd, WPARAM wParam, LPARAM lParam) {

//	std::cout << "handleMessage(" << messageId << ")" << std::endl;
	switch (messageId) {
	case WM_DESTROY:
		windowDestroyed(hWnd);
		break;
	case WM_CREATE:
		WINDOWINFO xWI;
		xWI.cbSize = sizeof(xWI);
		if (!GetWindowInfo(hWnd, &xWI)) {
			std::cout << "Could not get window info" << std::endl;
		} else {
			windowCreated(hWnd, &xWI.rcWindow);
		}
		break;
	case WM_WINDOWPOSCHANGING:
		if (IsWindowVisible(hWnd)) {
			WINDOWINFO xWI;
			xWI.cbSize = sizeof(xWI);
			if (!GetWindowInfo(hWnd, &xWI)) {
				std::cout << "Could not get window info" << std::endl;
			} else {
				windowMoved(hWnd, &xWI.rcWindow);
			}
		}
		break;
	}
}

LRESULT CALLBACK handleCbtProc(int nCode, WPARAM wParam, LPARAM lParam) {
	if (nCode >= 0) {
		if (rootWindow != NULL) {
			MSG *msg = (MSG *) lParam;
			handleMessage(msg->message, msg->hwnd, msg->wParam, msg->lParam);
		}
	}
	return CallNextHookEx(cbtHook, nCode, wParam, lParam);
}

LRESULT CALLBACK handleSysMsgFilterProc(int nCode, WPARAM wParam,
		LPARAM lParam) {
	if (nCode >= 0) {
		if (rootWindow != NULL) {
			MSG *msg = (MSG *) lParam;
			handleMessage(msg->message, msg->hwnd, msg->wParam, msg->lParam);
		}
	}
	return CallNextHookEx(sysMsgFilterHook, nCode, wParam, lParam);
}

LRESULT CALLBACK handleGetMessageProc(int nCode, WPARAM wParam, LPARAM lParam) {
	if (nCode == HC_ACTION) {
		if (rootWindow != NULL) {
			MSG *msg = (MSG *) lParam;
			if (wParam & PM_REMOVE) {
				handleMessage(msg->message, msg->hwnd, msg->wParam,
						msg->lParam);
			}
		}
	}
	return CallNextHookEx(getMessageHook, nCode, wParam, lParam);
}

LRESULT CALLBACK handleCallWndProc(int nCode, WPARAM wParam, LPARAM lParam) {
	if (nCode == HC_ACTION) {
		if (rootWindow != NULL) {
			CWPSTRUCT *cwpStruct = (CWPSTRUCT *) lParam;
			handleMessage(cwpStruct->message, cwpStruct->hwnd,
					cwpStruct->wParam, cwpStruct->lParam);
		}
	}
	return CallNextHookEx(callWndHook, nCode, wParam, lParam);
}
