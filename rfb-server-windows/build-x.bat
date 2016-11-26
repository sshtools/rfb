 

g++ -c -DBUILDING_HOOKTEST_DLL -I "C:\Program Files\Java\jdk1.7.0_67\include" -I "C:\Program Files\Java\jdk1.7.0_67\include\win32" src\main\native\HookTest.cpp -o target\HookTest.o
REM g++ -shared  --enable-runtime-pseudo-reloc -o target\HookTest.dll target\HookTest.o -Wl,--kill-at -Wl,--out-implib,target\libHookTest.dll.a 
REM g++ -shared  -o target\HookTest.dll target\HookTest.o -Wl,--kill-at -Wl,--out-implib,target\libHookTest.dll.a

g++ -shared  -o target\HookTest.dll target\HookTest.o -Wl,--subsystem,windows -Wl,--kill-at -Wl,--out-implib,target\libHookTest.dll.a

