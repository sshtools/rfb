// DesCipher - the DES encryption method
//
// The meat of this code is by Dave Zimmerman <dzimm@widget.com>, and is:
//
// Copyright (c) 1996 Widget Workshop, Inc. All Rights Reserved.
//
// Permission to use, copy, modify, and distribute this software
// and its documentation for NON-COMMERCIAL or COMMERCIAL purposes and
// without fee is hereby granted, provided that this copyright notice is kept
// intact.
//
// WIDGET WORKSHOP MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
// OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
// TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
// PARTICULAR PURPOSE, OR NON-INFRINGEMENT. WIDGET WORKSHOP SHALL NOT BE LIABLE
// FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
// DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
//
// THIS SOFTWARE IS NOT DESIGNED OR INTENDED FOR USE OR RESALE AS ON-LINE
// CONTROL EQUIPMENT IN HAZARDOUS ENVIRONMENTS REQUIRING FAIL-SAFE
// PERFORMANCE, SUCH AS IN THE OPERATION OF NUCLEAR FACILITIES, AIRCRAFT
// NAVIGATION OR COMMUNICATION SYSTEMS, AIR TRAFFIC CONTROL, DIRECT LIFE
// SUPPORT MACHINES, OR WEAPONS SYSTEMS, IN WHICH THE FAILURE OF THE
// SOFTWARE COULD LEAD DIRECTLY TO DEATH, PERSONAL INJURY, OR SEVERE
// PHYSICAL OR ENVIRONMENTAL DAMAGE ("HIGH RISK ACTIVITIES").  WIDGET WORKSHOP
// SPECIFICALLY DISCLAIMS ANY EXPRESS OR IMPLIED WARRANTY OF FITNESS FOR
// HIGH RISK ACTIVITIES.
//
//
// The rest is:
//
// Copyright (C) 1996 by Jef Poskanzer <jef@acme.com>.  All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/

package com.sshtools.rfbcommon;


/// The DES encryption method.
// <P>
// This is surprisingly fast, for pure Java.  On a SPARC 20, wrapped
// in Acme.Crypto.EncryptedOutputStream or Acme.Crypto.EncryptedInputStream,
// it does around 7000 bytes/second.
// <P>
// Most of this code is by Dave Zimmerman <dzimm@widget.com>, and is
// Copyright (c) 1996 Widget Workshop, Inc.  See the source file for details.
// <P>
// <A HREF="/resources/classes/Acme/Crypto/DesCipher.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see Des3Cipher
// @see EncryptedOutputStream
// @see EncryptedInputStream

public class AcmeDesCipher extends BlockCipher
    {

    // Constructor, string key.
    public AcmeDesCipher( String keyStr )
	{
	super( 8, 8 );
	setKey( keyStr );
	}

    // Constructor, byte-array key.
    public AcmeDesCipher( byte[] key )
	{
	super( 8, 8 );
	setKey( key );
	}

    // Key routines.

    private int[] encryptKeys = new int[32];
    private int[] decryptKeys = new int[32];

    /// Set the key.
    public void setKey( byte[] key )
	{
	// Have to tweak the parity bits so that each byte has odd parity.
	// This imposition of odd parity is required by the standard.
	// Note that DES wants the parity in the *low* bit.

	/*for ( int i = 0; i < 8; ++i )
	    if ( Acme.Utils.even( Acme.Utils.countOnes( key[i] ) ) )
		key[i] ^= 1;
*/
	deskey( key, true, encryptKeys );
	deskey( key, false, decryptKeys );
	}

    // Turn an 8-byte key into internal keys.
    private void deskey( byte[] keyBlock, boolean encrypting, int[] KnL )
	{
	int i, j, l, m, n;
	int[] pc1m = new int[56];
	int[] pcr = new int[56];
	int[] kn = new int[32];

	for ( j = 0; j < 56; ++j )
	    {
	    l = pc1[j];
	    m = l & 07;
	    pc1m[j] = ( (keyBlock[l >>> 3] & bytebit[m]) != 0 )? 1: 0;
	    }

	for ( i = 0; i < 16; ++i )
	    {
	    if ( encrypting )
		m = i << 1;
	    else
		m = (15-i) << 1;
	    n = m+1;
	    kn[m] = kn[n] = 0;
	    for ( j = 0; j < 28; ++j )
		{
		l = j+totrot[i];
		if ( l < 28 )
		    pcr[j] = pc1m[l];
		else
		    pcr[j] = pc1m[l-28];
		}
	    for ( j=28; j < 56; ++j )
		{
		l = j+totrot[i];
		if ( l < 56 )
		    pcr[j] = pc1m[l];
		else
		    pcr[j] = pc1m[l-28];
		}
	    for ( j = 0; j < 24; ++j )
		{
		if ( pcr[pc2[j]] != 0 )
		    kn[m] |= bigbyte[j];
		if ( pcr[pc2[j+24]] != 0 )
		    kn[n] |= bigbyte[j];
		}
	    }
	cookey( kn, KnL );
	}

    private void cookey( int[] raw, int KnL[] )
	{
	int raw0, raw1;
	int rawi, KnLi;
	int i;

	for ( i = 0, rawi = 0, KnLi = 0; i < 16; ++i )
	    {
	    raw0 = raw[rawi++];
	    raw1 = raw[rawi++];
	    KnL[KnLi]  = (raw0 & 0x00fc0000) <<   6;
	    KnL[KnLi] |= (raw0 & 0x00000fc0) <<  10;
	    KnL[KnLi] |= (raw1 & 0x00fc0000) >>> 10;
	    KnL[KnLi] |= (raw1 & 0x00000fc0) >>>  6;
	    ++KnLi;
	    KnL[KnLi]  = (raw0 & 0x0003f000) <<  12;
	    KnL[KnLi] |= (raw0 & 0x0000003f) <<  16;
	    KnL[KnLi] |= (raw1 & 0x0003f000) >>>  4;
	    KnL[KnLi] |= (raw1 & 0x0000003f);
	    ++KnLi;
	    }
	}


    // Block encryption routines.

    private int[] tempInts = new int[2];

    /// Encrypt a block of eight bytes.
    public void encrypt( byte[] clearText, int clearOff, byte[] cipherText, int cipherOff )
	{
	squashBytesToInts( clearText, clearOff, tempInts, 0, 2 );
	des( tempInts, tempInts, encryptKeys );
	spreadIntsToBytes( tempInts, 0, cipherText, cipherOff, 2 );
	}

    /// Decrypt a block of eight bytes.
    public void decrypt( byte[] cipherText, int cipherOff, byte[] clearText, int clearOff )
	{
	squashBytesToInts( cipherText, cipherOff, tempInts, 0, 2 );
	des( tempInts, tempInts, decryptKeys );
	spreadIntsToBytes( tempInts, 0, clearText, clearOff, 2 );
	}

    // The DES function.
    private void des( int[] inInts, int[] outInts, int[] keys )
	{
	int fval, work, right, leftt;
	int round;
	int keysi = 0;

	leftt = inInts[0];
	right = inInts[1];

	work   = ((leftt >>>  4) ^ right) & 0x0f0f0f0f;
	right ^= work;
	leftt ^= (work << 4);

	work   = ((leftt >>> 16) ^ right) & 0x0000ffff;
	right ^= work;
	leftt ^= (work << 16);

	work   = ((right >>>  2) ^ leftt) & 0x33333333;
	leftt ^= work;
	right ^= (work << 2);

	work   = ((right >>>  8) ^ leftt) & 0x00ff00ff;
	leftt ^= work;
	right ^= (work << 8);
	right  = (right << 1) | ((right >>> 31) & 1);

	work   = (leftt ^ right) & 0xaaaaaaaa;
	leftt ^= work;
	right ^= work;
	leftt  = (leftt << 1) | ((leftt >>> 31) & 1);

	for ( round = 0; round < 8; ++round )
	    {
	    work   = (right << 28) | (right >>> 4);
	    work  ^= keys[keysi++];
	    fval   = SP7[ work	       & 0x0000003f ];
	    fval  |= SP5[(work >>>  8) & 0x0000003f ];
	    fval  |= SP3[(work >>> 16) & 0x0000003f ];
	    fval  |= SP1[(work >>> 24) & 0x0000003f ];
	    work   = right ^ keys[keysi++];
	    fval  |= SP8[ work         & 0x0000003f ];
	    fval  |= SP6[(work >>>  8) & 0x0000003f ];
	    fval  |= SP4[(work >>> 16) & 0x0000003f ];
	    fval  |= SP2[(work >>> 24) & 0x0000003f ];
	    leftt ^= fval;
	    work   = (leftt << 28) | (leftt >>> 4);
	    work  ^= keys[keysi++];
	    fval   = SP7[ work	       & 0x0000003f ];
	    fval  |= SP5[(work >>>  8) & 0x0000003f ];
	    fval  |= SP3[(work >>> 16) & 0x0000003f ];
	    fval  |= SP1[(work >>> 24) & 0x0000003f ];
	    work   = leftt ^ keys[keysi++];
	    fval  |= SP8[ work	       & 0x0000003f ];
	    fval  |= SP6[(work >>>  8) & 0x0000003f ];
	    fval  |= SP4[(work >>> 16) & 0x0000003f ];
	    fval  |= SP2[(work >>> 24) & 0x0000003f ];
	    right ^= fval;
	    }

	right  = (right << 31) | (right >>> 1);
	work   = (leftt ^ right) & 0xaaaaaaaa;
	leftt ^= work;
	right ^= work;
	leftt  = (leftt << 31) | (leftt >>> 1);
	work   = ((leftt >>>  8) ^ right) & 0x00ff00ff;
	right ^= work;
	leftt ^= (work << 8);
	work   = ((leftt >>>  2) ^ right) & 0x33333333;
	right ^= work;
	leftt ^= (work << 2);
	work   = ((right >>> 16) ^ leftt) & 0x0000ffff;
	leftt ^= work;
	right ^= (work << 16);
	work   = ((right >>>  4) ^ leftt) & 0x0f0f0f0f;
	leftt ^= work;
	right ^= (work << 4);
	outInts[0] = right;
	outInts[1] = leftt;
	}


    // Tables, permutations, S-boxes, etc.

   /* private static byte[] bytebit = {
	(byte)0x80, (byte)0x40, (byte)0x20, (byte)0x10,
	(byte)0x08, (byte)0x04, (byte)0x02, (byte)0x01
	};*/
    private static byte[] bytebit = {
            (byte) 0x01, (byte) 0x02, (byte) 0x04, (byte) 0x08,
            (byte) 0x10, (byte) 0x20, (byte) 0x40, (byte) 0x80
        };

    private static int[] bigbyte = {
	0x800000, 0x400000, 0x200000, 0x100000,
	0x080000, 0x040000, 0x020000, 0x010000,
	0x008000, 0x004000, 0x002000, 0x001000,
	0x000800, 0x000400, 0x000200, 0x000100,
	0x000080, 0x000040, 0x000020, 0x000010,
	0x000008, 0x000004, 0x000002, 0x000001
	};
    private static byte[] pc1 = {
         (byte)56, (byte)48, (byte)40, (byte)32, (byte)24, (byte)16, (byte) 8,
      (byte) 0, (byte)57, (byte)49, (byte)41, (byte)33, (byte)25, (byte)17,
	 (byte) 9, (byte) 1, (byte)58, (byte)50, (byte)42, (byte)34, (byte)26,
      (byte)18, (byte)10, (byte) 2, (byte)59, (byte)51, (byte)43, (byte)35,
	 (byte)62, (byte)54, (byte)46, (byte)38, (byte)30, (byte)22, (byte)14,
      (byte) 6, (byte)61, (byte)53, (byte)45, (byte)37, (byte)29, (byte)21,
	 (byte)13, (byte) 5, (byte)60, (byte)52, (byte)44, (byte)36, (byte)28,
      (byte)20, (byte)12, (byte) 4, (byte)27, (byte)19, (byte)11, (byte)3
	};
    private static int[] totrot = {
        1, 2, 4, 6, 8, 10, 12, 14, 15, 17, 19, 21, 23, 25, 27, 28
	};

    private static byte[] pc2 = {
	(byte)13, (byte)16, (byte)10, (byte)23, (byte) 0, (byte) 4,
	          (byte) 2, (byte)27, (byte)14, (byte) 5, (byte)20, (byte) 9,
	(byte)22, (byte)18, (byte)11, (byte)3 , (byte)25, (byte) 7,
	          (byte)15, (byte) 6, (byte)26, (byte)19, (byte)12, (byte) 1,
	(byte)40, (byte)51, (byte)30, (byte)36, (byte)46, (byte)54,
	          (byte)29, (byte)39, (byte)50, (byte)44, (byte)32, (byte)47,
	(byte)43, (byte)48, (byte)38, (byte)55, (byte)33, (byte)52,
	          (byte)45, (byte)41, (byte)49, (byte)35, (byte)28, (byte)31,
	};

    private static int[] SP1 = {
        0x01010400, 0x00000000, 0x00010000, 0x01010404,
	0x01010004, 0x00010404, 0x00000004, 0x00010000,
	0x00000400, 0x01010400, 0x01010404, 0x00000400,
	0x01000404, 0x01010004, 0x01000000, 0x00000004,
	0x00000404, 0x01000400, 0x01000400, 0x00010400,
	0x00010400, 0x01010000, 0x01010000, 0x01000404,
	0x00010004, 0x01000004, 0x01000004, 0x00010004,
	0x00000000, 0x00000404, 0x00010404, 0x01000000,
	0x00010000, 0x01010404, 0x00000004, 0x01010000,
	0x01010400, 0x01000000, 0x01000000, 0x00000400,
	0x01010004, 0x00010000, 0x00010400, 0x01000004,
	0x00000400, 0x00000004, 0x01000404, 0x00010404,
	0x01010404, 0x00010004, 0x01010000, 0x01000404,
	0x01000004, 0x00000404, 0x00010404, 0x01010400,
	0x00000404, 0x01000400, 0x01000400, 0x00000000,
	0x00010004, 0x00010400, 0x00000000, 0x01010004
	};
    private static int[] SP2 = {
	0x80108020, 0x80008000, 0x00008000, 0x00108020,
	0x00100000, 0x00000020, 0x80100020, 0x80008020,
	0x80000020, 0x80108020, 0x80108000, 0x80000000,
	0x80008000, 0x00100000, 0x00000020, 0x80100020,
	0x00108000, 0x00100020, 0x80008020, 0x00000000,
	0x80000000, 0x00008000, 0x00108020, 0x80100000,
	0x00100020, 0x80000020, 0x00000000, 0x00108000,
	0x00008020, 0x80108000, 0x80100000, 0x00008020,
	0x00000000, 0x00108020, 0x80100020, 0x00100000,
	0x80008020, 0x80100000, 0x80108000, 0x00008000,
	0x80100000, 0x80008000, 0x00000020, 0x80108020,
	0x00108020, 0x00000020, 0x00008000, 0x80000000,
	0x00008020, 0x80108000, 0x00100000, 0x80000020,
	0x00100020, 0x80008020, 0x80000020, 0x00100020,
	0x00108000, 0x00000000, 0x80008000, 0x00008020,
	0x80000000, 0x80100020, 0x80108020, 0x00108000
	};
    private static int[] SP3 = {
	0x00000208, 0x08020200, 0x00000000, 0x08020008,
	0x08000200, 0x00000000, 0x00020208, 0x08000200,
	0x00020008, 0x08000008, 0x08000008, 0x00020000,
	0x08020208, 0x00020008, 0x08020000, 0x00000208,
	0x08000000, 0x00000008, 0x08020200, 0x00000200,
	0x00020200, 0x08020000, 0x08020008, 0x00020208,
	0x08000208, 0x00020200, 0x00020000, 0x08000208,
	0x00000008, 0x08020208, 0x00000200, 0x08000000,
	0x08020200, 0x08000000, 0x00020008, 0x00000208,
	0x00020000, 0x08020200, 0x08000200, 0x00000000,
	0x00000200, 0x00020008, 0x08020208, 0x08000200,
	0x08000008, 0x00000200, 0x00000000, 0x08020008,
	0x08000208, 0x00020000, 0x08000000, 0x08020208,
	0x00000008, 0x00020208, 0x00020200, 0x08000008,
	0x08020000, 0x08000208, 0x00000208, 0x08020000,
	0x00020208, 0x00000008, 0x08020008, 0x00020200
	};
    private static int[] SP4 = {
	0x00802001, 0x00002081, 0x00002081, 0x00000080,
	0x00802080, 0x00800081, 0x00800001, 0x00002001,
	0x00000000, 0x00802000, 0x00802000, 0x00802081,
	0x00000081, 0x00000000, 0x00800080, 0x00800001,
	0x00000001, 0x00002000, 0x00800000, 0x00802001,
	0x00000080, 0x00800000, 0x00002001, 0x00002080,
	0x00800081, 0x00000001, 0x00002080, 0x00800080,
	0x00002000, 0x00802080, 0x00802081, 0x00000081,
	0x00800080, 0x00800001, 0x00802000, 0x00802081,
	0x00000081, 0x00000000, 0x00000000, 0x00802000,
	0x00002080, 0x00800080, 0x00800081, 0x00000001,
	0x00802001, 0x00002081, 0x00002081, 0x00000080,
	0x00802081, 0x00000081, 0x00000001, 0x00002000,
	0x00800001, 0x00002001, 0x00802080, 0x00800081,
	0x00002001, 0x00002080, 0x00800000, 0x00802001,
	0x00000080, 0x00800000, 0x00002000, 0x00802080
	};
    private static int[] SP5 = {
	0x00000100, 0x02080100, 0x02080000, 0x42000100,
	0x00080000, 0x00000100, 0x40000000, 0x02080000,
	0x40080100, 0x00080000, 0x02000100, 0x40080100,
	0x42000100, 0x42080000, 0x00080100, 0x40000000,
	0x02000000, 0x40080000, 0x40080000, 0x00000000,
	0x40000100, 0x42080100, 0x42080100, 0x02000100,
	0x42080000, 0x40000100, 0x00000000, 0x42000000,
	0x02080100, 0x02000000, 0x42000000, 0x00080100,
	0x00080000, 0x42000100, 0x00000100, 0x02000000,
	0x40000000, 0x02080000, 0x42000100, 0x40080100,
	0x02000100, 0x40000000, 0x42080000, 0x02080100,
	0x40080100, 0x00000100, 0x02000000, 0x42080000,
	0x42080100, 0x00080100, 0x42000000, 0x42080100,
	0x02080000, 0x00000000, 0x40080000, 0x42000000,
	0x00080100, 0x02000100, 0x40000100, 0x00080000,
	0x00000000, 0x40080000, 0x02080100, 0x40000100
	};
    private static int[] SP6 = {
	0x20000010, 0x20400000, 0x00004000, 0x20404010,
	0x20400000, 0x00000010, 0x20404010, 0x00400000,
	0x20004000, 0x00404010, 0x00400000, 0x20000010,
	0x00400010, 0x20004000, 0x20000000, 0x00004010,
	0x00000000, 0x00400010, 0x20004010, 0x00004000,
	0x00404000, 0x20004010, 0x00000010, 0x20400010,
	0x20400010, 0x00000000, 0x00404010, 0x20404000,
	0x00004010, 0x00404000, 0x20404000, 0x20000000,
	0x20004000, 0x00000010, 0x20400010, 0x00404000,
	0x20404010, 0x00400000, 0x00004010, 0x20000010,
	0x00400000, 0x20004000, 0x20000000, 0x00004010,
	0x20000010, 0x20404010, 0x00404000, 0x20400000,
	0x00404010, 0x20404000, 0x00000000, 0x20400010,
	0x00000010, 0x00004000, 0x20400000, 0x00404010,
	0x00004000, 0x00400010, 0x20004010, 0x00000000,
	0x20404000, 0x20000000, 0x00400010, 0x20004010
	};
    private static int[] SP7 = {
	0x00200000, 0x04200002, 0x04000802, 0x00000000,
	0x00000800, 0x04000802, 0x00200802, 0x04200800,
	0x04200802, 0x00200000, 0x00000000, 0x04000002,
	0x00000002, 0x04000000, 0x04200002, 0x00000802,
	0x04000800, 0x00200802, 0x00200002, 0x04000800,
	0x04000002, 0x04200000, 0x04200800, 0x00200002,
	0x04200000, 0x00000800, 0x00000802, 0x04200802,
	0x00200800, 0x00000002, 0x04000000, 0x00200800,
	0x04000000, 0x00200800, 0x00200000, 0x04000802,
	0x04000802, 0x04200002, 0x04200002, 0x00000002,
	0x00200002, 0x04000000, 0x04000800, 0x00200000,
	0x04200800, 0x00000802, 0x00200802, 0x04200800,
	0x00000802, 0x04000002, 0x04200802, 0x04200000,
	0x00200800, 0x00000000, 0x00000002, 0x04200802,
	0x00000000, 0x00200802, 0x04200000, 0x00000800,
	0x04000002, 0x04000800, 0x00000800, 0x00200002
	};
    private static int[] SP8 = {
	0x10001040, 0x00001000, 0x00040000, 0x10041040,
	0x10000000, 0x10001040, 0x00000040, 0x10000000,
	0x00040040, 0x10040000, 0x10041040, 0x00041000,
	0x10041000, 0x00041040, 0x00001000, 0x00000040,
	0x10040000, 0x10000040, 0x10001000, 0x00001040,
	0x00041000, 0x00040040, 0x10040040, 0x10041000,
	0x00001040, 0x00000000, 0x00000000, 0x10040040,
	0x10000040, 0x10001000, 0x00041040, 0x00040000,
	0x00041040, 0x00040000, 0x10041000, 0x00001000,
	0x00000040, 0x10040040, 0x00001000, 0x00041040,
	0x10001000, 0x00000040, 0x10000040, 0x10040000,
	0x10040040, 0x10000000, 0x00040000, 0x10001040,
	0x00000000, 0x10041040, 0x00040040, 0x10000040,
	0x10040000, 0x10001000, 0x10001040, 0x00000000,
	0x10041040, 0x00041000, 0x00041000, 0x00001040,
	0x00001040, 0x00040040, 0x10000000, 0x10041000
	};

    }

    // BlockCipher - a block encryption template
//
// Copyright (C) 1996 by Jef Poskanzer <jef@acme.com>.  All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/

/// A block encryption template.
// <P>
// <A HREF="/resources/classes/Acme/Crypto/BlockCipher.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see Cipher
// @see StreamCipher
// @see EncryptedOutputStream
// @see EncryptedInputStream
// @see DesCipher
// @see CbcBlockCipher

abstract class BlockCipher extends Cipher
    {

    /// Constructor.
    public BlockCipher( int keySize, int blockSize )
        {
        super( keySize );
        this.blockSize = blockSize;
        }

    /// How big a block is.
    public int blockSize;

    /// Return how big a block is.
    public int blockSize()
        {
        return blockSize;
        }

    /// Encrypt a block of bytes.
    public abstract void encrypt( byte[] clearText, int clearOff, byte[] cipherText, int cipherOff );

    /// Decrypt a block of bytes.
    public abstract void decrypt( byte[] cipherText, int cipherOff, byte[] clearText, int clearOff );

    /// Encrypt a block of bytes.
    public void encrypt( byte[] clearText, byte[] cipherText )
        {
        encrypt( clearText, 0, cipherText, 0 );
        }

    /// Decrypt a block of bytes.
    public void decrypt( byte[] cipherText, byte[] clearText )
        {
        decrypt( cipherText, 0, clearText, 0 );
        }

    }

    // Cipher - an encryption template
//
// Copyright (C) 1996 by Jef Poskanzer <jef@acme.com>.  All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/


/// An encryption template.
// <P>
// <A HREF="/resources/classes/Acme/Crypto/Cipher.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see StreamCipher
// @see BlockCipher
// @see EncryptedOutputStream
// @see EncryptedInputStream

    abstract class Cipher extends CryptoUtils
        {

        /// Constructor.
        public Cipher( int keySize )
            {
            this.keySize = keySize;
            }

        /// How big a key is.  Keyless ciphers use 0.  Variable-length-key ciphers
        // also use 0.
        public int keySize;

        /// Return how big a key is.
        public int keySize()
            {
            return keySize;
            }

        /// Set the key from a block of bytes.
        public abstract void setKey( byte[] key );


        // Utility routines.

        /// Utility routine to set the key from a string.
        public void setKey( String keyStr )
            {
            setKey( makeKey( keyStr ) );
            }


        /// Utility routine to turn a string into a key of the right length.
        public byte[] makeKey( String keyStr )
            {
            byte[] key;
            if ( keySize == 0 )
                key = new byte[keyStr.length()];
            else
                key = new byte[keySize];
            int i, j;

            for ( j = 0; j < key.length; ++j )
                key[j] = 0;

            for ( i = 0, j = 0; i < keyStr.length(); ++i, j = (j+1) % key.length )
                key[j] ^= (byte) keyStr.charAt( i );

            return key;
            }

        }

        // CryptoUtils - some cryptography utilities
//
// Copyright (C) 1996 by Jef Poskanzer <jef@acme.com>.  All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/

/// Some cryptography utilities.
// <P>
// These are static methods used by a lot of the cryptography classes.
// Most of them operate on byte arrays, which we call blocks.
// They could be encapsulated in a "Block" class, but that would
// mean a big efficiency hit - method calls are a lot more
// expensive than array accesses.
// <P>
// <A HREF="/resources/classes/Acme/Crypto/CryptoUtils.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

        class CryptoUtils
            {

            /// Utility routine to fill a block with zeros.
            public static void zeroBlock( byte[] block, int off, int len )
                {
                for ( int i = off; i < off + len; ++i )
                    block[i] = 0;
                }

            /// Utility routine to fill a block with zeros.
            public static void zeroBlock( byte[] block )
                {
                zeroBlock( block, 0, block.length );
                }

            /// Utility routine to fill a block with random bytes.
            public static void randomBlock( byte[] block, int off, int len )
                {
                for ( int i = off; i < off + len; ++i )
                    block[i] = (byte) ( Math.random() * 256.0 );
                }

            /// Utility routine to fill a block with random bytes.
            public static void randomBlock( byte[] block )
                {
                randomBlock( block, 0, block.length );
                }

            /// Utility routine to XOR two blocks.
            public static void xorBlock( byte[] a, int aOff, byte[] b, int bOff, byte[] dst, int dstOff, int len )
                {
                for ( int i = 0; i < len; ++i )
                    dst[dstOff + i] = (byte) ( a[aOff + i] ^ b[bOff + i] );
                }

            /// Utility routine to XOR two blocks.
            public static void xorBlock( byte[] a, byte[] b, byte[] dst )
                {
                xorBlock( a, 0, b, 0, dst, 0, a.length );
                }

            /// Utility routine to copy one block to another.
            public static void copyBlock( byte[] src, int srcOff, byte[] dst, int dstOff, int len )
                {
                for ( int i = 0; i < len; ++i )
                    dst[dstOff + i] = src[srcOff + i];
                }

            /// Utility routine to copy one block to another.
            public static void copyBlock( byte[] src, byte[] dst )
                {
                copyBlock( src, 0, dst, 0, src.length );
                }

            /// Utility routine to check two blocks for equality.
            public static boolean equalsBlock( byte[] a, int aOff, byte[] b, int bOff, int len )
                {
                for ( int i = 0; i < len; ++i )
                    if ( a[aOff + i] != b[bOff + i] )
                        return false;
                return true;
                }

            /// Utility routine to check two blocks for equality.
            public static boolean equalsBlock( byte[] a, byte[] b )
                {
                return equalsBlock( a, 0, b, 0, a.length );
                }

            /// Utility routine fill a block with a given byte.
            public static void fillBlock( byte[] block, int blockOff, byte b, int len )
                {
                for ( int i = blockOff; i < blockOff + len; ++i )
                    block[i] = b;
                }

            /// Utility routine fill a block with a given byte.
            public static void fillBlock( byte[] block, byte b )
                {
                fillBlock( block, 0, b, block.length );
                }

            /// Squash bytes down to ints.
            public static void squashBytesToInts( byte[] inBytes, int inOff, int[] outInts, int outOff, int intLen )
                {
                for ( int i = 0; i < intLen; ++i )
                    outInts[outOff + i] =
                        ( ( inBytes[inOff + i * 4    ] & 0xff ) << 24 ) |
                        ( ( inBytes[inOff + i * 4 + 1] & 0xff ) << 16 ) |
                        ( ( inBytes[inOff + i * 4 + 2] & 0xff ) <<  8 ) |
                        ( ( inBytes[inOff + i * 4 + 3] & 0xff )       );
                }

            /// Spread ints into bytes.
            public static void spreadIntsToBytes( int[] inInts, int inOff, byte[] outBytes, int outOff, int intLen )
                {
                for ( int i = 0; i < intLen; ++i )
                    {
                    outBytes[outOff + i * 4    ] =
                        (byte) ( ( inInts[inOff + i] >>> 24 ) & 0xff );
                    outBytes[outOff + i * 4 + 1] =
                        (byte) ( ( inInts[inOff + i] >>> 16 ) & 0xff );
                    outBytes[outOff + i * 4 + 2] =
                        (byte) ( ( inInts[inOff + i] >>>  8 ) & 0xff );
                    outBytes[outOff + i * 4 + 3] =
                        (byte) ( ( inInts[inOff + i]        ) & 0xff );
                    }
                }

            /// Squash bytes down to ints, little-endian.
            public static void squashBytesToIntsLittle( byte[] inBytes, int inOff, int[] outInts, int outOff, int intLen )
                {
                for ( int i = 0; i < intLen; ++i )
                    outInts[outOff + i] =
                        ( ( inBytes[inOff + i * 4    ] & 0xff )       ) |
                        ( ( inBytes[inOff + i * 4 + 1] & 0xff ) <<  8 ) |
                        ( ( inBytes[inOff + i * 4 + 2] & 0xff ) << 16 ) |
                        ( ( inBytes[inOff + i * 4 + 3] & 0xff ) << 24 );
                }

            /// Spread ints into bytes, little-endian.
            public static void spreadIntsToBytesLittle( int[] inInts, int inOff, byte[] outBytes, int outOff, int intLen )
                {
                for ( int i = 0; i < intLen; ++i )
                    {
                    outBytes[outOff + i * 4    ] =
                        (byte) ( ( inInts[inOff + i]        ) & 0xff );
                    outBytes[outOff + i * 4 + 1] =
                        (byte) ( ( inInts[inOff + i] >>>  8 ) & 0xff );
                    outBytes[outOff + i * 4 + 2] =
                        (byte) ( ( inInts[inOff + i] >>> 16 ) & 0xff );
                    outBytes[outOff + i * 4 + 3] =
                        (byte) ( ( inInts[inOff + i] >>> 24 ) & 0xff );
                    }
                }

            /// Squash bytes down to shorts.
            public static void squashBytesToShorts( byte[] inBytes, int inOff, int[] outShorts, int outOff, int shortLen )
                {
                for ( int i = 0; i < shortLen; ++i )
                    outShorts[outOff + i] =
                        ( ( inBytes[inOff + i * 2    ] & 0xff ) << 8 ) |
                        ( ( inBytes[inOff + i * 2 + 1] & 0xff )      );
                }

            /// Spread shorts into bytes.
            public static void spreadShortsToBytes( int[] inShorts, int inOff, byte[] outBytes, int outOff, int shortLen )
                {
                for ( int i = 0; i < shortLen; ++i )
                    {
                    outBytes[outOff + i * 2    ] =
                        (byte) ( ( inShorts[inOff + i] >>> 8 ) & 0xff );
                    outBytes[outOff + i * 2 + 1] =
                        (byte) ( ( inShorts[inOff + i]       ) & 0xff );
                    }
                }

            /// Squash bytes down to shorts, little endian.
            public static void squashBytesToShortsLittle( byte[] inBytes, int inOff, int[] outShorts, int outOff, int shortLen )
                {
                for ( int i = 0; i < shortLen; ++i )
                    outShorts[outOff + i] =
                        ( ( inBytes[inOff + i * 2    ] & 0xff )      ) |
                        ( ( inBytes[inOff + i * 2 + 1] & 0xff ) << 8 );
                }

            /// Spread shorts into bytes, little endian.
            public static void spreadShortsToBytesLittle( int[] inShorts, int inOff, byte[] outBytes, int outOff, int shortLen )
                {
                for ( int i = 0; i < shortLen; ++i )
                    {
                    outBytes[outOff + i * 2    ] =
                        (byte) ( ( inShorts[inOff + i]       ) & 0xff );
                    outBytes[outOff + i * 2 + 1] =
                        (byte) ( ( inShorts[inOff + i] >>> 8 ) & 0xff );
                    }
                }

            /// Convert a block to a String representation.
            public static String toStringBlock( byte[] block, int off, int len )
                {
                String hexits = "0123456789abcdef";
                StringBuffer buf = new StringBuffer();
                for ( int i = off; i < off + len; ++i )
                    {
                    buf.append( hexits.charAt( ( block[i] >>> 4 ) & 0xf ) );
                    buf.append( hexits.charAt( block[i] & 0xf ) );
                    }
                return "[" + buf + "]";
                }

            /// Convert a block to a String representation.
            public static String toStringBlock( byte[] block )
                {
                return toStringBlock( block, 0, block.length );
                }

            }
