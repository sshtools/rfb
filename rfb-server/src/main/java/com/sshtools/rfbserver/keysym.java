package com.sshtools.rfbserver;

import java.awt.event.*;

/**
 * Translates UNIX keysym codes to/from Java virtual key codes.
 **/

public abstract class keysym {
	public static final int DeadGrave = 0xFE50;
	public static final int DeadAcute = 0xFE51;
	public static final int DeadCircumflex = 0xFE52;
	public static final int DeadTilde = 0xFE53;

	public static final int BackSpace = 0xFF08;
	public static final int Tab = 0xFF09;
	public static final int Linefeed = 0xFF0A;
	public static final int Clear = 0xFF0B;
	public static final int Return = 0xFF0D;
	public static final int Pause = 0xFF13;
	public static final int ScrollLock = 0xFF14;
	public static final int SysReq = 0xFF15;
	public static final int Escape = 0xFF1B;

	public static final int Delete = 0xFFFF;

	public static final int Home = 0xFF50;
	public static final int Left = 0xFF51;
	public static final int Up = 0xFF52;
	public static final int Right = 0xFF53;
	public static final int Down = 0xFF54;
	public static final int PageUp = 0xFF55;
	public static final int PageDown = 0xFF56;
	public static final int End = 0xFF57;
	public static final int Begin = 0xFF58;

	public static final int Select = 0xFF60;
	public static final int Print = 0xFF61;
	public static final int Execute = 0xFF62;
	public static final int Insert = 0xFF63;

	public static final int Cancel = 0xFF69;
	public static final int Help = 0xFF6A;
	public static final int Break = 0xFF6B;
	public static final int NumLock = 0xFF6F;

	public static final int KpSpace = 0xFF80;
	public static final int KpTab = 0xFF89;
	public static final int KpEnter = 0xFF8D;

	public static final int KpHome = 0xFF95;
	public static final int KpLeft = 0xFF96;
	public static final int KpUp = 0xFF97;
	public static final int KpRight = 0xFF98;
	public static final int KpDown = 0xFF99;
	public static final int KpPrior = 0xFF9A;
	public static final int KpPageUp = 0xFF9A;
	public static final int KpNext = 0xFF9B;
	public static final int KpPageDown = 0xFF9B;
	public static final int KpEnd = 0xFF9C;
	public static final int KpBegin = 0xFF9D;
	public static final int KpInsert = 0xFF9E;
	public static final int KpDelete = 0xFF9F;
	public static final int KpEqual = 0xFFBD;
	public static final int KpMultiply = 0xFFAA;
	public static final int KpAdd = 0xFFAB;
	public static final int KpSeparator = 0xFFAC;
	public static final int KpSubtract = 0xFFAD;
	public static final int KpDecimal = 0xFFAE;
	public static final int KpDivide = 0xFFAF;

	public static final int KpF1 = 0xFF91;
	public static final int KpF2 = 0xFF92;
	public static final int KpF3 = 0xFF93;
	public static final int KpF4 = 0xFF94;

	public static final int Kp0 = 0xFFB0;
	public static final int Kp1 = 0xFFB1;
	public static final int Kp2 = 0xFFB2;
	public static final int Kp3 = 0xFFB3;
	public static final int Kp4 = 0xFFB4;
	public static final int Kp5 = 0xFFB5;
	public static final int Kp6 = 0xFFB6;
	public static final int Kp7 = 0xFFB7;
	public static final int Kp8 = 0xFFB8;
	public static final int Kp9 = 0xFFB9;

	public static final int F1 = 0xFFBE;
	public static final int F2 = 0xFFBF;
	public static final int F3 = 0xFFC0;
	public static final int F4 = 0xFFC1;
	public static final int F5 = 0xFFC2;
	public static final int F6 = 0xFFC3;
	public static final int F7 = 0xFFC4;
	public static final int F8 = 0xFFC5;
	public static final int F9 = 0xFFC6;
	public static final int F10 = 0xFFC7;
	public static final int F11 = 0xFFC8;
	public static final int F12 = 0xFFC9;
	public static final int F13 = 0xFFCA;
	public static final int F14 = 0xFFCB;
	public static final int F15 = 0xFFCC;
	public static final int F16 = 0xFFCD;
	public static final int F17 = 0xFFCE;
	public static final int F18 = 0xFFCF;
	public static final int F19 = 0xFFD0;
	public static final int F20 = 0xFFD1;
	public static final int F21 = 0xFFD2;
	public static final int F22 = 0xFFD3;
	public static final int F23 = 0xFFD4;
	public static final int F24 = 0xFFD5;

	public static final int ShiftL = 0xFFE1;
	public static final int ShiftR = 0xFFE2;
	public static final int ControlL = 0xFFE3;
	public static final int ControlR = 0xFFE4;
	public static final int CapsLock = 0xFFE5;
	public static final int ShiftLock = 0xFFE6;
	public static final int MetaL = 0xFFE7;
	public static final int MetaR = 0xFFE8;
	public static final int AltL = 0xFFE9;
	public static final int AltR = 0xFFEA;

	public static void toVK(int keysym, int[] vk) {
		vk[1] = KeyEvent.KEY_LOCATION_STANDARD;
		switch (keysym) {
		case DeadGrave:
			vk[0] = KeyEvent.VK_DEAD_GRAVE;
			break;
		case DeadAcute:
			vk[0] = KeyEvent.VK_DEAD_ACUTE;
			break;
		case DeadCircumflex:
			vk[0] = KeyEvent.VK_DEAD_CIRCUMFLEX;
			break;
		case DeadTilde:
			vk[0] = KeyEvent.VK_DEAD_TILDE;
			break;

		case Tab:
			vk[0] = KeyEvent.VK_TAB;
			break;
		// No Java equivalent: case Linefeed: vk[0] = KeyEvent.;
		case Clear:
			vk[0] = KeyEvent.VK_CLEAR;
			break;
		case Return:
			vk[0] = KeyEvent.VK_ENTER;
			break;
		case Pause:
			vk[0] = KeyEvent.VK_PAUSE;
			break;
		case ScrollLock:
			vk[0] = KeyEvent.VK_SCROLL_LOCK;
			break;
		// No Java equivalent: case SysReq: vk[0] = KeyEvent.;
		case Escape:
			vk[0] = KeyEvent.VK_ESCAPE;
			break;

		case 0: // Some clients (the MF java viewer) send 0 instead of 0xFFFF
		case Delete:
			vk[0] = KeyEvent.VK_DELETE;
			break;

		case Home:
			vk[0] = KeyEvent.VK_HOME;
			break;
		case Left:
			vk[0] = KeyEvent.VK_LEFT;
			break;
		case Up:
			vk[0] = KeyEvent.VK_UP;
			break;
		case Right:
			vk[0] = KeyEvent.VK_RIGHT;
			break;
		case Down:
			vk[0] = KeyEvent.VK_DOWN;
			break;
		case PageUp:
			vk[0] = KeyEvent.VK_PAGE_UP;
			break;
		case PageDown:
			vk[0] = KeyEvent.VK_PAGE_DOWN;
			break;
		case End:
			vk[0] = KeyEvent.VK_END;
			break;
		// No Java equivalent: case Begin: vk[0] = KeyEvent.;

		// No Java equivalent: case Select: vk[0] = KeyEvent.;
		case Print:
			vk[0] = KeyEvent.VK_PRINTSCREEN;
			break;
		// No Java equivalent: case Execute: vk[0] = KeyEvent.;
		case Insert:
			vk[0] = KeyEvent.VK_INSERT;
			break;

		case Cancel:
			vk[0] = KeyEvent.VK_CANCEL;
			break;
		case Help:
			vk[0] = KeyEvent.VK_HELP;
			break;
		// No Java equivalent: case Break: vk[0] = KeyEvent.;
		case NumLock:
			vk[0] = KeyEvent.VK_NUM_LOCK;
			break;

		case KpSpace:
			vk[0] = KeyEvent.VK_SPACE;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpTab:
			vk[0] = KeyEvent.VK_TAB;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpEnter:
			vk[0] = KeyEvent.VK_ENTER;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;

		case KpHome:
			vk[0] = KeyEvent.VK_HOME;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpLeft:
			vk[0] = KeyEvent.VK_LEFT;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpUp:
			vk[0] = KeyEvent.VK_UP;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpRight:
			vk[0] = KeyEvent.VK_RIGHT;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpDown:
			vk[0] = KeyEvent.VK_DOWN;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpPageUp:
			vk[0] = KeyEvent.VK_PAGE_UP;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break; // = KpPrior
		case KpPageDown:
			vk[0] = KeyEvent.VK_PAGE_DOWN;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break; // = KpNext
		case KpEnd:
			vk[0] = KeyEvent.VK_END;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		// No Java equivalent: case KpBegin: vk[0] = KeyEvent.;
		case KpInsert:
			vk[0] = KeyEvent.VK_INSERT;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpDelete:
			vk[0] = KeyEvent.VK_DELETE;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpEqual:
			vk[0] = KeyEvent.VK_EQUALS;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpMultiply:
			vk[0] = KeyEvent.VK_MULTIPLY;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpAdd:
			vk[0] = KeyEvent.VK_ADD;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpSeparator:
			vk[0] = KeyEvent.VK_SEPARATOR;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpSubtract:
			vk[0] = KeyEvent.VK_SUBTRACT;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpDecimal:
			vk[0] = KeyEvent.VK_DECIMAL;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpDivide:
			vk[0] = KeyEvent.VK_DIVIDE;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;

		case KpF1:
			vk[0] = KeyEvent.VK_F1;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpF2:
			vk[0] = KeyEvent.VK_F2;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpF3:
			vk[0] = KeyEvent.VK_F3;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case KpF4:
			vk[0] = KeyEvent.VK_F4;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;

		case Kp0:
			vk[0] = KeyEvent.VK_NUMPAD0;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case Kp1:
			vk[0] = KeyEvent.VK_NUMPAD1;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case Kp2:
			vk[0] = KeyEvent.VK_NUMPAD2;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case Kp3:
			vk[0] = KeyEvent.VK_NUMPAD3;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case Kp4:
			vk[0] = KeyEvent.VK_NUMPAD4;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case Kp5:
			vk[0] = KeyEvent.VK_NUMPAD5;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case Kp6:
			vk[0] = KeyEvent.VK_NUMPAD6;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case Kp7:
			vk[0] = KeyEvent.VK_NUMPAD7;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case Kp8:
			vk[0] = KeyEvent.VK_NUMPAD8;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;
		case Kp9:
			vk[0] = KeyEvent.VK_NUMPAD9;
			vk[1] = KeyEvent.KEY_LOCATION_NUMPAD;
			break;

		case F1:
			vk[0] = KeyEvent.VK_F1;
			break;
		case F2:
			vk[0] = KeyEvent.VK_F2;
			break;
		case F3:
			vk[0] = KeyEvent.VK_F3;
			break;
		case F4:
			vk[0] = KeyEvent.VK_F4;
			break;
		case F5:
			vk[0] = KeyEvent.VK_F5;
			break;
		case F6:
			vk[0] = KeyEvent.VK_F6;
			break;
		case F7:
			vk[0] = KeyEvent.VK_F7;
			break;
		case F8:
			vk[0] = KeyEvent.VK_F8;
			break;
		case F9:
			vk[0] = KeyEvent.VK_F9;
			break;
		case F10:
			vk[0] = KeyEvent.VK_F10;
			break;
		case F11:
			vk[0] = KeyEvent.VK_F11;
			break;
		case F12:
			vk[0] = KeyEvent.VK_F12;
			break;
		case F13:
			vk[0] = KeyEvent.VK_F12;
			break;
		case F14:
			vk[0] = KeyEvent.VK_F12;
			break;
		case F15:
			vk[0] = KeyEvent.VK_F12;
			break;
		case F16:
			vk[0] = KeyEvent.VK_F12;
			break;
		case F17:
			vk[0] = KeyEvent.VK_F12;
			break;
		case F18:
			vk[0] = KeyEvent.VK_F12;
			break;
		case F19:
			vk[0] = KeyEvent.VK_F12;
			break;
		case F20:
			vk[0] = KeyEvent.VK_F12;
			break;
		case F21:
			vk[0] = KeyEvent.VK_F12;
			break;
		case F22:
			vk[0] = KeyEvent.VK_F12;
			break;
		case F23:
			vk[0] = KeyEvent.VK_F12;
			break;
		case F24:
			vk[0] = KeyEvent.VK_F12;
			break;
		case CapsLock:
			vk[0] = KeyEvent.VK_CAPS_LOCK;
			break;
		// No Java equivalent: case ShiftLock: vk[0] = KeyEvent.;
		default:
			if (keysym >= 32 && keysym <= 127) {
				vk[0] = keysym;
			} else {
				vk[0] = KeyEvent.VK_UNDEFINED;
				vk[1] = KeyEvent.KEY_LOCATION_UNKNOWN;
			}
			break;
		}
	}

	public static void toVKall(int keysym, int vk[]) {
		toVK(keysym, vk);
		if (vk[0] != KeyEvent.VK_UNDEFINED)
			return;

		vk[1] = KeyEvent.KEY_LOCATION_UNKNOWN;
		switch (keysym) {
		case BackSpace:
			vk[0] = KeyEvent.VK_BACK_SPACE;
			break;
		case ShiftL:
			vk[0] = KeyEvent.VK_SHIFT;
			vk[1] = KeyEvent.KEY_LOCATION_LEFT;
			break;
		case ShiftR:
			vk[0] = KeyEvent.VK_SHIFT;
			vk[1] = KeyEvent.KEY_LOCATION_RIGHT;
			break;
		case ControlL:
			vk[0] = KeyEvent.VK_CONTROL;
			vk[1] = KeyEvent.KEY_LOCATION_LEFT;
			break;
		case ControlR:
			vk[0] = KeyEvent.VK_CONTROL;
			vk[1] = KeyEvent.KEY_LOCATION_RIGHT;
			break;
		case MetaL:
			vk[0] = KeyEvent.VK_META;
			vk[1] = KeyEvent.KEY_LOCATION_LEFT;
			break;
		case MetaR:
			vk[0] = KeyEvent.VK_META;
			vk[1] = KeyEvent.KEY_LOCATION_RIGHT;
			break;
		case AltL:
			vk[0] = KeyEvent.VK_ALT;
			vk[1] = KeyEvent.KEY_LOCATION_LEFT;
			break;
		case AltR:
			vk[0] = KeyEvent.VK_ALT;
			vk[1] = KeyEvent.KEY_LOCATION_RIGHT;
			break;
		default:
			vk[0] = KeyEvent.VK_UNDEFINED;
			break;
		}
	}

	public static char toCharacter(int keysym) {
		switch (keysym) {
		case BackSpace:
			return '\b';
		default:
			return (char) keysym;
		}
	}

	public static void toMask(int keysym, int[] mask) {
		switch (keysym) {
		case ShiftL:
			mask[0] = KeyEvent.SHIFT_DOWN_MASK;
			mask[1] = KeyEvent.KEY_LOCATION_LEFT;
			break;
		case ShiftR:
			mask[0] = KeyEvent.SHIFT_DOWN_MASK;
			mask[1] = KeyEvent.KEY_LOCATION_RIGHT;
			break;
		case ControlL:
			mask[0] = KeyEvent.CTRL_DOWN_MASK;
			mask[1] = KeyEvent.KEY_LOCATION_LEFT;
			break;
		case ControlR:
			mask[0] = KeyEvent.CTRL_DOWN_MASK;
			mask[1] = KeyEvent.KEY_LOCATION_RIGHT;
			break;
		case MetaL:
			mask[0] = KeyEvent.META_DOWN_MASK;
			mask[1] = KeyEvent.KEY_LOCATION_LEFT;
			break;
		case MetaR:
			mask[0] = KeyEvent.META_DOWN_MASK;
			mask[1] = KeyEvent.KEY_LOCATION_RIGHT;
			break;
		case AltL:
			mask[0] = KeyEvent.ALT_DOWN_MASK;
			mask[1] = KeyEvent.KEY_LOCATION_LEFT;
			break;
		case AltR:
			mask[0] = KeyEvent.ALT_DOWN_MASK;
			mask[1] = KeyEvent.KEY_LOCATION_RIGHT;
			break;
		default:
			mask[0] = 0;
			mask[1] = KeyEvent.KEY_LOCATION_UNKNOWN;
			break;
		}
	}
}
