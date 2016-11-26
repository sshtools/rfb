 

g++ -c -DBUILDING_DISPLAYHOOK_DLL -I "C:\Program Files\Java\jdk1.7.0_67\include" -I "C:\Program Files\Java\jdk1.7.0_67\include\win32" src\main\native\displayhook.cpp -o target\displayhook.o
g++ -shared  --enable-runtime-pseudo-reloc -o target\displayhook.dll target\displayhook.o -Wl,--kill-at -Wl,--out-implib,target\libdisplayhook.dll.a 

REM g++ -shared  --enable-runtime-pseudo-reloc -o displayhook.dll displayhook.o -Wl,--out-implib,libdisplayhook.dll.a
REM g++ -D_JNI_IMPLEMENTATION_  -enable-stdcall-fixup -Wl,-enable-auto-import -Wl,-enable-runtime-pseudo-reloc -shared -mthreads -Wl,--out-implib,target\libdisplayhook.a -o target\displayhook.dll target\displayhook.o 