package com.sshtools.rfbcommon;

import java.util.Arrays;
import java.util.List;

public interface RFBConstants {
	
	// Keysysm
	public final static int RFBKEY_BACKSPACE = 0xff08;
	public final static int RFBKEY_TAB = 0xff09;
	public final static int RFBKEY_ENTER = 0xff0d;
	public final static int RFBKEY_ESCAPE = 0xff1b;
	public final static int RFBKEY_INSERT= 0xff63;
	public final static int RFBKEY_DELETE = 0xffff;
	public final static int RFBKEY_HOME = 0xff50;
	public final static int RFBKEY_END = 0xff57;
	public final static int RFBKEY_PGUP = 0xff55;
	public final static int RFBKEY_PGDN = 0xff56;
	public final static int RFBKEY_LEFT= 0xff51;
	public final static int RFBKEY_UP= 0xff52;
	public final static int RFBKEY_RIGHT= 0xff53;
	public final static int RFBKEY_DOWN = 0xff54;
	public final static int RFBKEY_F1 = 0xffbe;
	public final static int RFBKEY_F2 = 0xffbf;
	public final static int RFBKEY_F3 = 0xffc0;
	public final static int RFBKEY_F4 = 0xffc1;
	public final static int RFBKEY_F5 = 0xffc2;
	public final static int RFBKEY_F6 = 0xffc3;
	public final static int RFBKEY_F7 = 0xffc4;
	public final static int RFBKEY_F8 = 0xffc5;
	public final static int RFBKEY_F9 = 0xffc6;
	public final static int RFBKEY_F10 = 0xffc7;
	public final static int RFBKEY_F11 = 0xffc8;
	public final static int RFBKEY_F12 = 0xffc9;
	public final static int RFBKEY_SHIFT_LEFT = 0xffe1;
	public final static int RFBKEY_SHIFT_RIGHT = 0xffe2;
	public final static int RFBKEY_CTRL_LEFT = 0xffe3;
	public final static int RFBKEY_CTRL_RIGHT = 0xffe4;
	public final static int RFBKEY_META_LEFT = 0xffe5;
	public final static int RFBKEY_META_RIGHT = 0xffe6;
	public final static int RFBKEY_ALT_LEFT = 0xffe7;
	public final static int RFBKEY_ALT_RIGHT = 0xffe8;

	// Client messages
	public final static int CMSG_SET_PIXEL_FORMAT = 0;
	public final static int CMSG_FIX_COLORMAP = 1;
	public final static int CMSG_SET_ENCODINGS = 2;
	public final static int CMSG_REQUEST_FRAMEBUFFER_UPDATE = 3;
	public final static int CMSG_KEYBOARD_EVENT = 4;
	public final static int CMSG_POINTER_EVENT = 5;
	public final static int CMSG_CUT_TEXT = 6;
	// Server messages
	public final static int SMSG_FRAMEBUFFER_UPDATE = 0;
	public final static int SMSG_SET_COLORMAP = 1;
	public final static int SMSG_BELL = 2;
	public final static int SMSG_SERVER_CUT_TEXT = 3;
	public final static int SMSG_FILE_TRANSFER = 7;
	public final static int SMSG_TIGHT_FILETRANSFER = 252;
	// Authenticaton scheme return values

	public final static int SCHEME_CONNECT_FAILED = 0;
	public final static int SCHEME_NO_AUTHENTICATION = 1;
	public final static int SCHEME_VNC_AUTHENTICATION = 2;
	public static final int SCHEME_TIGHT_AUTHENTICATION = 16;
	public static final int SCHEME_TLS_AUTHENTICATION = 18;
	public static final int SCHEME_UNIX_AUTHENTICATION = 129; // Tight extension
																// only
	public static final int SCHEME_EXTERNAL_AUTHENTICATION = 130; // Tight
																	// extension
																	// only

	
	// Authentication result values
	public final static int AUTHENTICATION_OK = 0;
	public final static int AUTHENTICATION_FAILED = 1;
	public final static int AUTHENTICATION_TOO_MANY = 2;
	public final static int TIGHT_MIN_BYTES_TO_COMPRESS = 12;

	// Requests
	public final static int RFB_DIR_CONTENT_REQUEST = 1;
	public final static int RFB_COMMAND = 10;
	// Request parameters
	public final static int RFB_DIR_CONTENT = 1;
	public final static int RFB_DIR_DRIVE_LIST = 2;
	// Command parameters
	public final static int RFB_DIR_CREATE = 1;
	// Received
	public final static int RFB_RECV_NONE = 0;
	public final static int RFB_DIR_PACKET = 2;
	public final static int RFB_RECV_DIRECTORY = 1;
	public final static int RFB_RECV_DRIVE_LIST = 3;

	public static final String RFB_STANDARD_VENDOR = "STDV";
	public static final String RFB_TRIDIA_VNC_VENDOR = "TRDV";
	public static final String RFB_TIGHT_VNC_VENDOR = "TGHT";

	// ZRLE subencodings
	public static final int ZRLE_RAW = 0;
	public static final int ZRLE_SOLID = 1;
	public static final int ZRLE_PACKED_PALETTE = 2; // 2-16
	public static final int ZRLE_PLAIN_RLE = 128;
	public static final int ZRLE_PALETTE_RLE = 130; // 130-255

	// public static final int RFB_FILE_LIST_DATA = 130;
	// public static final String RFB_FILE_LIST_DATA_SIG = "FTS_LSDT";
	// public static final int RFB_FILE_DOWNLOAD_DATA = 131;
	// public static final String RFB_FILE_DOWNLOAD_DATA_SIG = "FTS_DNDT";
	// public static final int RFB_FILE_UPLOAD_CANCEL = 132;
	// public static final String RFB_FILE_UPLOAD_CANCEL_SIG = "FTS_UPCN";
	// public static final int RFB_FILE_DOWNLOAD_FAILED = 133;
	// public static final String RFB_FILE_DOWNLOAD_FAILED_SIG = "FTS_DNFL";
	// public static final int RFB_FILE_LIST_REQUEST = 130;
	// public static final String RFB_FILE_LIST_REQUEST_SIG = "FTC_LSRQ";
	// public static final int RFB_FILE_DOWNLOAD_REQUEST = 131;
	// public static final String RFB_FILE_DOWNLOAD_REQUEST_SIG = "FTC_DNRQ";
	// public static final int RFB_FILE_UPLOAD_REQUEST = 132;
	// public static final String RFB_FILE_UPLOAD_REQUEST_SIG = "FTC_UPRQ";
	// public static final int RFB_FILE_UPLOAD_DATA = 133;
	// public static final String RFB_FILE_UPLOAD_DATA_SIG = "FTC_UPDT";
	// public static final int RFB_FILE_DOWNLOAD_CANCEL = 134;
	// public static final String RFB_FILE_DOWNLOAD_CANCEL_SIG = "FTC_DNCN";
	// public static final int RFB_FILE_UPLOAD_FAILED = 135;
	// public static final String RFB_FILE_UPLOAD_FAILED_SIG = "FTC_UPFL";
	// public static final int RFB_FILE_CREATE_DIR_REQUEST = 136;
	// public static final String RFB_FILE_CREATE_DIR_REQUEST_SIG = "FTC_FCDR";

	// Encodings
	public static final String ENC_RAW_SIG = "RAW_____";
	public static final String ENC_COPYRECT_SIG = "COPYRECT";
	public static final String ENC_RRE_SIG = "RRE_____";
	public static final String ENC_CORRE_SIG = "CORRE___";
	public static final String ENC_HEXTILE_SIG = "HEXTILE_";
	public static final String ENC_ZLIB_SIG = "ZLIB____";
	public static final String ENC_ZRLE_SIG = "ZRLE____";
	public static final String ENC_TIGHT_SIG = "TIGHT___";
	public static final String ENC_ZLIBHEX_SIG = "ZLIBHEX_";
	public static final String ENC_POINTER_POS_SIG = "POINTPOS";
	public static final String ENC_COMPRESS_SIG = "COMPRLVL";
	public static final String ENC_X11_CURSOR_SIG = "X11CURSR";
	public static final String ENC_RICH_CURSOR_SIG = "RCHCURSR";
	public static final String ENC_LAST_RECT_SIG = "LASTRECT";
	public static final String ENC_NEW_FB_SIZE_SIG = "NEWFBSIZ";
	public static final String ENC_JPEG_QUALITY_SIG = "JPEGQLVL";

	public static final String NO_AUTH_SIG = "NOAUTH__";
	public static final String VNC_AUTH_SIG = "VNCAUTH_";
	public static final String UNIX_LOGIN_AUTH_SIG = "ULGNAUTH";
	public static final String EXTERNAL_AUTH_SIG = "XTRNAUTH";
	public static final String TIGHT_AUTH_SIG = "TGHTAUTH";

	//
	public final static int ENC_RAW = 0;
	public final static int ENC_COPYRECT = 1;
	public final static int ENC_RRE = 2;
	public final static int ENC_CORRE = 3;
	public final static int ENC_HEXTILE = 5;
	public final static int ENC_ZLIB = 6;
	public final static int ENC_TIGHT = 7;
	public final static int ENC_ZLIBHEX = 8;
	public final static int ENC_ZRLE = 16;

	public final static int ENC_POINTER_POS = 0xffffff18;
	public final static int ENC_RICH_CURSOR = 0xffffff11;
	public final static int ENC_X11_CURSOR = 0xffffff10;
	public final static int ENC_COMPRESS_LEVEL0 = 0xFFFFFF00;
	public final static int ENC_LAST_RECT = 0xFFFFFF20;
	public final static int ENC_NEW_FB_SIZE = 0xFFFFFF21;

	// Caps
	public static final TightCapability CAP_ENC_RAW = new TightCapability(
			ENC_RAW, RFB_STANDARD_VENDOR, ENC_RAW_SIG);
	public static final TightCapability CAP_ENC_COPYRECT = new TightCapability(
			ENC_COPYRECT, RFB_STANDARD_VENDOR, ENC_COPYRECT_SIG);
	public static final TightCapability CAP_ENC_RRE = new TightCapability(
			ENC_RRE, RFB_STANDARD_VENDOR, ENC_RRE_SIG);
	public static final TightCapability CAP_ENC_CORRE = new TightCapability(
			ENC_CORRE, RFB_STANDARD_VENDOR, ENC_CORRE_SIG);
	public static final TightCapability CAP_ENC_HEXTILE = new TightCapability(
			ENC_HEXTILE, RFB_STANDARD_VENDOR, ENC_HEXTILE_SIG);
	public static final TightCapability CAP_ENC_ZLIB = new TightCapability(
			ENC_ZLIB, RFB_TRIDIA_VNC_VENDOR, ENC_ZLIB_SIG);
	public static final TightCapability CAP_ENC_TIGHT = new TightCapability(
			ENC_TIGHT, RFB_TIGHT_VNC_VENDOR, ENC_TIGHT_SIG);
	public static final TightCapability CAP_ENC_ZLIBHEX = new TightCapability(
			ENC_ZLIBHEX, RFB_STANDARD_VENDOR, ENC_ZLIBHEX_SIG);
	public static final TightCapability CAP_ENC_ZRLE = new TightCapability(
			ENC_ZRLE, RFB_TRIDIA_VNC_VENDOR, ENC_ZRLE_SIG);

	public static final TightCapability CAP_ENC_COMPRESS = new TightCapability(
			ENC_COMPRESS_LEVEL0, RFB_STANDARD_VENDOR, ENC_COMPRESS_SIG);
	public static final TightCapability CAP_ENC_X11_CURSOR = new TightCapability(
			ENC_X11_CURSOR, RFB_STANDARD_VENDOR, ENC_X11_CURSOR_SIG);
	public static final TightCapability CAP_ENC_RICH_CURSOR = new TightCapability(
			ENC_RICH_CURSOR, RFB_STANDARD_VENDOR, ENC_RICH_CURSOR_SIG);
	public static final TightCapability CAP_ENC_POINTER_POS = new TightCapability(
			ENC_POINTER_POS, RFB_STANDARD_VENDOR, ENC_POINTER_POS_SIG);
	public static final TightCapability CAP_ENC_LAST_RECT = new TightCapability(
			ENC_LAST_RECT, RFB_STANDARD_VENDOR, ENC_LAST_RECT_SIG);
	public static final TightCapability CAP_ENC_NEW_FB_SIZE = new TightCapability(
			ENC_NEW_FB_SIZE, RFB_STANDARD_VENDOR, ENC_NEW_FB_SIZE_SIG);
	public static final TightCapability CAP_ENC_JPEG_QUALITY = new TightCapability(
			ENC_POINTER_POS, RFB_TIGHT_VNC_VENDOR, ENC_JPEG_QUALITY_SIG);

	public static final TightCapability CAP_AUTH_NONE = new TightCapability(
			SCHEME_NO_AUTHENTICATION, RFB_STANDARD_VENDOR, NO_AUTH_SIG);
	public static final TightCapability CAP_AUTH_VNC = new TightCapability(
			SCHEME_VNC_AUTHENTICATION, RFB_STANDARD_VENDOR, VNC_AUTH_SIG);
	public static final TightCapability CAP_AUTH_UNIX = new TightCapability(
			SCHEME_UNIX_AUTHENTICATION, RFB_STANDARD_VENDOR,
			UNIX_LOGIN_AUTH_SIG);
	public static final TightCapability CAP_AUTH_EXTERANL = new TightCapability(
			SCHEME_EXTERNAL_AUTHENTICATION, RFB_STANDARD_VENDOR,
			EXTERNAL_AUTH_SIG);
	public static final TightCapability CAP_AUTH_TIGHT = new TightCapability(
			SCHEME_TIGHT_AUTHENTICATION, RFB_TIGHT_VNC_VENDOR, TIGHT_AUTH_SIG);

	// S -> C
	// FTSCSRLY
	public final static int CAP_FTSCSRLY_CODE = 0xfc000101;
	public final static String CAP_FTSCSRLY_SIG = "FTSCSRLY";
	public final static TightCapability CAP_FTSCSRLY = new TightCapability(
			CAP_FTSCSRLY_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTSCSRLY_SIG);
	// FTSFLRLY
	public final static int CAP_FTSFLRLY_CODE = 0xfc000103;
	public final static String CAP_FTSFLRLY_SIG = "FTSFLRLY";
	public final static TightCapability CAP_FTSFLRLY = new TightCapability(
			CAP_FTSFLRLY_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTSFLRLY_SIG);
	// FTSM5RLY
	public final static int CAP_FTSM5RLY_CODE = 0xfc000105;
	public final static String CAP_FTSM5RLY_SIG = "FTSM5RLY";
	public final static TightCapability CAP_FTSM5RLY = new TightCapability(
			CAP_FTSM5RLY_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTSM5RLY_SIG);
	// FTSFURLY
	public final static int CAP_FTSFURLY_CODE = 0xfc000107;
	public final static String CAP_FTSFURLY_SIG = "FTSFURLY";
	public final static TightCapability CAP_FTSFURLY = new TightCapability(
			CAP_FTSFURLY_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTSFURLY_SIG);
	// FTSUDRLY
	public final static int CAP_FTSUDRLY_CODE = 0xfc000109;
	public final static String CAP_FTSUDRLY_SIG = "FTSUDRLY";
	public final static TightCapability CAP_FTSUDRLY = new TightCapability(
			CAP_FTSUDRLY_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTSUDRLY_SIG);
	// FTSUERLY
	public final static int CAP_FTSUERLY_CODE = 0xfc00010b;
	public final static String CAP_FTSUERLY_SIG = "FTSUERLY";
	public final static TightCapability CAP_FTSUERLY = new TightCapability(
			CAP_FTSUERLY_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTSUERLY_SIG);
	// FTSFDRLY
	public final static int CAP_FTSFDRLY_CODE = 0xfc00010d;
	public final static String CAP_FTSFDRLY_SIG = "FTSFDRLY";
	public final static TightCapability CAP_FTSFDRLY = new TightCapability(
			CAP_FTSFDRLY_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTSFDRLY_SIG);
	// FTSDDRLY
	public final static int CAP_FTSDDRLY_CODE = 0xfc00010f;
	public final static String CAP_FTSDDRLY_SIG = "FTSDDRLY";
	public final static TightCapability CAP_FTSDDRLY = new TightCapability(
			CAP_FTSDDRLY_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTSDDRLY_SIG);
	// FTSDERLY
	public final static int CAP_FTSDERLY_CODE = 0xfc000110;
	public final static String CAP_FTSDERLY_SIG = "FTSDERLY";
	public final static TightCapability CAP_FTSDERLY = new TightCapability(
			CAP_FTSDERLY_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTSDERLY_SIG);
	// FTSMDRLY
	public final static int CAP_FTSMDRLY_CODE = 0xfc000112;
	public final static String CAP_FTSMDRLY_SIG = "FTSMDRLY";
	public final static TightCapability CAP_FTSMDRLY = new TightCapability(
			CAP_FTSMDRLY_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTSMDRLY_SIG);
	// FTSFTRLY
	public final static int CAP_FTSFTRLY_CODE = 0xfc000114;
	public final static String CAP_FTSFTRLY_SIG = "FTSFTRLY";
	public final static TightCapability CAP_FTSFTRLY = new TightCapability(
			CAP_FTSFTRLY_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTSFTRLY_SIG);
	// FTSFMRLY
	public final static int CAP_FTSFMRLY_CODE = 0xfc000116;
	public final static String CAP_FTSFMRLY_SIG = "FTSFMRLY";
	public final static TightCapability CAP_FTSFMRLY = new TightCapability(
			CAP_FTSFMRLY_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTSFMRLY_SIG);
	// FTSDSRLY
	public final static int CAP_FTSDSRLY_CODE = 0xfc000118;
	public final static String CAP_FTSDSRLY_SIG = "FTSDSRLY";
	public final static TightCapability CAP_FTSDSRLY = new TightCapability(
			CAP_FTSDSRLY_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTSDSRLY_SIG);
	// FTLRFRLY
	public final static int CAP_FTLRFRLY_CODE = 0xfc000119;
	public final static String CAP_FTLRFRLY_SIG = "FTLRFRLY";
	public final static TightCapability CAP_FTLRFRLY = new TightCapability(
			CAP_FTLRFRLY_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTLRFRLY_SIG);
	// C -> S
	// VDFREEZ
	public final static int CAP_VDFREEZ_CODE = 0x98;
	public final static String CAP_VDFREEZ_SIG = "VD_FREEZ";
	public final static TightCapability CAP_VDFREEZ = new TightCapability(
			CAP_VDFREEZ_CODE, RFB_TIGHT_VNC_VENDOR, CAP_VDFREEZ_SIG);
	// FTCCSRST
	public final static int CAP_FTCCSRST_CODE = 0xfc000100;
	public final static String CAP_FTCCSRST_SIG = "FTCCSRST";
	public final static TightCapability CAP_FTCCSRST = new TightCapability(
			CAP_FTCCSRST_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTCCSRST_SIG);
	// FTCFLRST
	public final static int CAP_FTCFLRST_CODE = 0xfc000102;
	public final static String CAP_FTCFLRST_SIG = "FTCFLRST";
	public final static TightCapability CAP_FTCFLRST = new TightCapability(
			CAP_FTCFLRST_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTCFLRST_SIG);
	// FTCM5RST
	public final static int CAP_FTCM5RST_CODE = 0xfc000104;
	public final static String CAP_FTCM5RST_SIG = "FTCM5RST";
	public final static TightCapability CAP_FTCM5RST = new TightCapability(
			CAP_FTCM5RST_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTCM5RST_SIG);
	// FTCFURST
	public final static int CAP_FTCFURST_CODE = 0xfc000106;
	public final static String CAP_FTCFURST_SIG = "FTCFURST";
	public final static TightCapability CAP_FTCFURST = new TightCapability(
			CAP_FTCFURST_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTCFURST_SIG);
	// FTCUDRST
	public final static int CAP_FTCUDRST_CODE = 0xfc000108;
	public final static String CAP_FTCUDRST_SIG = "FTCUDRST";
	public final static TightCapability CAP_FTCUDRST = new TightCapability(
			CAP_FTCUDRST_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTCUDRST_SIG);
	// FTCUERST
	public final static int CAP_FTCUERST_CODE = 0xfc00010a;
	public final static String CAP_FTCUERST_SIG = "FTCUERST";
	public final static TightCapability CAP_FTCUERST = new TightCapability(
			CAP_FTCUERST_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTCUERST_SIG);
	// FTCFDRST
	public final static int CAP_FTCFDRST_CODE = 0xfc00010c;
	public final static String CAP_FTCFDRST_SIG = "FTCFDRST";
	public final static TightCapability CAP_FTCFDRST = new TightCapability(
			CAP_FTCFDRST_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTCFDRST_SIG);
	// FTCDDRST
	public final static int CAP_FTCDDRST_CODE = 0xfc00010e;
	public final static String CAP_FTCDDRST_SIG = "FTCDDRST";
	public final static TightCapability CAP_FTCDDRST = new TightCapability(
			CAP_FTCDDRST_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTCDDRST_SIG);
	// FTCMDRST
	public final static int CAP_FTCMDRST_CODE = 0xfc000111;
	public final static String CAP_FTCMDRST_SIG = "FTCMDRST";
	public final static TightCapability CAP_FTCMDRST = new TightCapability(
			CAP_FTCMDRST_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTCMDRST_SIG);
	// FTCFRRST
	public final static int CAP_FTCFRRST_CODE = 0xfc000113;
	public final static String CAP_FTCFRRST_SIG = "FTCFRRST";
	public final static TightCapability CAP_FTCFRRST = new TightCapability(
			CAP_FTCFRRST_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTCFRRST_SIG);
	// FTCFMRST
	public final static int CAP_FTCFMRST_CODE = 0xfc000115;
	public final static String CAP_FTCFMRST_SIG = "FTCFMRST";
	public final static TightCapability CAP_FTCFMRST = new TightCapability(
			CAP_FTCFMRST_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTCFMRST_SIG);
	// FTCDSRST
	public final static int CAP_FTCDSRST_CODE = 0xfc000117;
	public final static String CAP_FTCDSRST_SIG = "FTCDSRST";
	public final static TightCapability CAP_FTCDSRST = new TightCapability(
			CAP_FTCDSRST_CODE, RFB_TIGHT_VNC_VENDOR, CAP_FTCDSRST_SIG);

}
