@REM
@REM RFB Server (Windows Driver) - A JNA based driver for Windows,
@REM Copyright © 2006 SSHTOOLS Limited (support@sshtools.com)
@REM
@REM This program is free software: you can redistribute it and/or modify
@REM it under the terms of the GNU General Public License as published by
@REM the Free Software Foundation, either version 3 of the License, or
@REM (at your option) any later version.
@REM
@REM This program is distributed in the hope that it will be useful,
@REM but WITHOUT ANY WARRANTY; without even the implied warranty of
@REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
@REM GNU General Public License for more details.
@REM
@REM You should have received a copy of the GNU General Public License
@REM along with this program. If not, see <http://www.gnu.org/licenses/>.
@REM

 

g++ -c -DBUILDING_HOOKTEST_DLL -I "C:\Program Files\Java\jdk1.7.0_67\include" -I "C:\Program Files\Java\jdk1.7.0_67\include\win32" src\main\native\HookTest.cpp -o target\HookTest.o
REM g++ -shared  --enable-runtime-pseudo-reloc -o target\HookTest.dll target\HookTest.o -Wl,--kill-at -Wl,--out-implib,target\libHookTest.dll.a 
REM g++ -shared  -o target\HookTest.dll target\HookTest.o -Wl,--kill-at -Wl,--out-implib,target\libHookTest.dll.a

g++ -shared  -o target\HookTest.dll target\HookTest.o -Wl,--subsystem,windows -Wl,--kill-at -Wl,--out-implib,target\libHookTest.dll.a

