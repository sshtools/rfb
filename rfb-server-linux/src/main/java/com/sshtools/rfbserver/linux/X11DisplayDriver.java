package com.sshtools.rfbserver.linux;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.x.CLibrary;
import org.x.ExtX;
import org.x.ExtX.XShmSegmentInfo;
import org.x.X11;
import org.x.X11.Atom;
import org.x.X11.Window;
import org.x.X11.XImage;

import com.sshtools.rfbserver.drivers.RobotDisplayDriver;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;

public class X11DisplayDriver extends RobotDisplayDriver {

	final static Logger LOG = LoggerFactory.getLogger(X11DisplayDriver.class);

	private ExtX.Display dpy;
	private ExtX x11Instance;
	private ExtX.Xdamage xdamageInstance;
	private ExtX.XFixes xfixesInstance;
	private List<NativeLong> windowList;
	private Atom _NET_CLIENT_LIST_STACKING;
	private Atom _NET_WM_WINDOW_TYPE;
	private Atom _NET_WM_WINDOW_TYPE_NORMAL;
	private Atom _NET_WM_WINDOW_TYPE_DOCK;
	private Atom _NET_WM_WINDOW_TYPE_DESKTOP;
	private Atom _NET_WM_NAME;
	private Atom UTF8_STRING;
	private IntByReference damage_event;
	private IntByReference fixes_event;
	private boolean destroy;
	private Rectangle area;
	private long lastTs;
	private Map<NativeLong, Rectangle> bounds = new HashMap<NativeLong, Rectangle>();

	private Window xDefaultRootWindow;

	private XImage image;

	private XShmSegmentInfo shminfo;

	public X11DisplayDriver() {

		x11Instance = ExtX.INSTANCE;
		xdamageInstance = ExtX.Xdamage.INSTANCE;
		xfixesInstance = ExtX.XFixes.INSTANCE;

		x11Instance.XInitThreads();

		damage_event = new IntByReference();
		IntByReference damage_error = new IntByReference();
		IntByReference major = new IntByReference();
		IntByReference minor = new IntByReference();

		fixes_event = new IntByReference();
		IntByReference fixes_error = new IntByReference();
		IntByReference fixes_major = new IntByReference();
		IntByReference fixes_minor = new IntByReference();

		dpy = x11Instance.XOpenDisplay((String) null);
		x11Instance.XSetErrorHandler(new X11.XErrorHandler() {
			public int apply(X11.Display DisplayPtr1, X11.XErrorEvent err) {

				/*
				 * With some window when are deleted cause a Damage error cause
				 * a XDamageSubtract() call. To avoid this handle separately the
				 * error.
				 * 
				 * I don't know if is correct or not.
				 */

				ByteBuffer error_message = ByteBuffer.allocate(1024);
				x11Instance.XGetErrorText(dpy, err.error_code, error_message,
						1024);
				x11Instance.XGetErrorDatabaseText(dpy, "XDAMAGE", "", "",
						error_message, 1024);
				LOG.error(String.format(
						"Error received from X server: %s\n%s\n%s\n", "", "",
						new String(error_message.array())));
				return 0;
			}
		});

		/* property from WM */
		UTF8_STRING = x11Instance.XInternAtom(dpy, "UTF8_STRING", false);
		_NET_CLIENT_LIST_STACKING = x11Instance.XInternAtom(dpy,
				"_NET_CLIENT_LIST_STACKING", false);
		_NET_WM_NAME = x11Instance.XInternAtom(dpy, "_NET_WM_NAME", false);
		_NET_WM_WINDOW_TYPE = x11Instance.XInternAtom(dpy,
				"_NET_WM_WINDOW_TYPE", false);
		_NET_WM_WINDOW_TYPE_NORMAL = x11Instance.XInternAtom(dpy,
				"_NET_WM_WINDOW_TYPE_NORMAL", false);
		_NET_WM_WINDOW_TYPE_DOCK = x11Instance.XInternAtom(dpy,
				"_NET_WM_WINDOW_TYPE_DOCK", false);
		_NET_WM_WINDOW_TYPE_DESKTOP = x11Instance.XInternAtom(dpy,
				"_NET_WM_WINDOW_TYPE_DESKTOP", false);

		/* need this if I want to receive window creation event */
		xDefaultRootWindow = x11Instance.XDefaultRootWindow(dpy);
		x11Instance.XSelectInput(dpy, xDefaultRootWindow, new NativeLong(
				X11.SubstructureNotifyMask | X11.StructureNotifyMask));

		/* takes some info about xdamage extension */
		xdamageInstance.XDamageQueryExtension(dpy, damage_event, damage_error);
		LOG.debug(String.format("Damageevent base: %d\tbase error: %d",
				damage_event.getValue(), damage_error.getValue()));
		xdamageInstance.XDamageQueryVersion(dpy, major, minor);
		LOG.debug(String.format("Damage major = %d, minor = %d",
				major.getValue(), minor.getValue()));

		/* takes some info about xfixes extension */
		xfixesInstance.XFixesQueryExtension(dpy, fixes_event, fixes_error);
		LOG.debug(String.format("Fixes event base: %d\tbase error: %d",
				fixes_event.getValue(), fixes_error.getValue()));
		xfixesInstance.XFixesQueryVersion(dpy, fixes_major, fixes_minor);
		LOG.debug(String.format("Fixes major = %d, minor = %d",
				fixes_major.getValue(), fixes_minor.getValue()));
		xfixesInstance.XFixesSelectCursorInput(dpy, xDefaultRootWindow,
				new NativeLong(ExtX.XFixes.XFixesDisplayCursorNotifyMask));

		/* allocate some shared memory for XShmGetImage */
//		 shminfo = new ExtX.XShmSegmentInfo();
//		int scr = ExtX.INSTANCE.XDefaultScreen(dpy);
//
//		int defaultDepth = ExtX.Macros.DefaultDepth(dpy, scr);
//		image = ExtX.XExt.INSTANCE.XShmCreateImage(dpy, vis,
//				new IntByReference(defaultDepth),
//				ExtX.ZPixmap, null, shminfo, new IntByReference(getWidth()),
//				new IntByReference(getHeight()));
//		shminfo.shmid = CLibrary.INSTANCE.shmget(CLibrary.IPC_PRIVATE,
//				image.bytes_per_line * image.height, CLibrary.IPC_CREAT | 0777);
//		if (shminfo.shmid == -1) {
//			throw new RuntimeException("Can't get shared memory.");
//		}
//		shminfo.shmaddr = image.data = CLibrary.INSTANCE.shmat(shminfo.shmid,
//				Pointer.createConstant(0), 0);
//		shminfo.readOnly = false;
//		if (!ExtX.XExt.INSTANCE.XShmAttach(dpy, shminfo)) {
//			throw new RuntimeException("Failed to attach shared memory.");
//		}
	}

	// public BufferedImage grabArea(Rectangle area) {
	// //
	// http://stackoverflow.com/questions/6063329/screenshot-using-opengl-and-or-x11
	// long started = System.currentTimeMillis();
	// BufferedImage createScreenCapture = null;
	// XImage img = new XImage();
	// ExtX.XShm.INSTANCE.XShmGetImage(dpy, xDefaultRootWindow, img,
	// (int) area.getX(), (int) area.getY(), ExtX.AllPlanes);
	// if (LOG.isDebugEnabled()) {
	// LOG.debug("Grab of " + area + " took "
	// + (System.currentTimeMillis() - started) + "ms");
	// }
	// return createScreenCapture;
	// }

	private String getWindowName(NativeLong windowId) {
		return "X11" + windowId.longValue();
		// NativeLongByReference n_items = new NativeLongByReference();
		// NativeLongByReference n_left = new NativeLongByReference();
		// PointerByReference data = new PointerByReference();
		// IntByReference realFormat = new IntByReference();
		// NativeLongByReference realType = new NativeLongByReference();
		// int status = x11Instance.XGetWindowProperty(dpy, windowId,
		// _NET_WM_NAME, new NativeLong(0l), new NativeLong(65536l), 0,
		// new NativeLong(UTF8_STRING.longValue()), realType, realFormat,
		// n_items, n_left, data);
		//
		// if (status != x11Instance.Success) {
		// return "";
		// }
		// Pointer value = data.getValue();
		// if (value == null) {
		// // Try another method
		// PointerByReference pb = new PointerByReference();
		// x11Instance.XFetchName(dpy, windowId, pb);
		// Pointer val = pb.getValue();
		// return val == null ? "<Unknown>" : val.getString(0);
		// }
		// String name = value.getString(0);
		// // x11Instance.XFree(data.getPointer());
		// return name;
	}

	@Override
	public void destroy() {
		super.destroy();
//		ExtX.XExt.INSTANCE.XShmDetach(dpy, shminfo);
//		CLibrary.INSTANCE.shmdt(shminfo.shmaddr);
//		CLibrary.INSTANCE.shmctl(shminfo.shmid, CLibrary.IPC_RMID, null);
//		ExtX.INSTANCE.XDestroyImage(image);
		image = null;
		ExtX.INSTANCE.XCloseDisplay(dpy);
		destroy = false;
	}

	private List<NativeLong> xd_get_win_list(NativeLong id) {

		IntByReference realFormat = new IntByReference();
		X11.AtomByReference realType = new X11.AtomByReference();
		NativeLongByReference n_client = new NativeLongByReference();
		NativeLongByReference n_left = new NativeLongByReference();
		PointerByReference data = new PointerByReference();
		X11.Window wtmp;

		int status = x11Instance.XGetWindowProperty(dpy,
				x11Instance.XDefaultRootWindow(dpy), _NET_CLIENT_LIST_STACKING,
				new NativeLong(0l), new NativeLong(8192l), false,
				X11.XA_WINDOW, realType, realFormat, n_client, n_left, data);

		if (status != X11.Success) {
			return null;
		}

		int noWindows = n_client.getValue().intValue();
		LOG.info(String.format("There are windows", noWindows));

		List<NativeLong> ws = new ArrayList<NativeLong>();

		long[] arr = data.getValue().getLongArray(0, noWindows);
		for (long i : arr) {
			wtmp = new X11.Window(i);
			LOG.info(String.format("   * %d", wtmp.longValue()));
			check_window_for_add(ws, wtmp);
		}

		LOG.info(String.format("Handling %d windows", ws.size()));

		return ws;
	}

	private int check_window_for_add(List<NativeLong> ws, Window wtmp) {
		int status;
		IntByReference realFormat = new IntByReference();
		X11.AtomByReference realType = new X11.AtomByReference();
		NativeLongByReference n_type = new NativeLongByReference();
		NativeLongByReference n_type_left = new NativeLongByReference();
		PointerByReference data_w_type = new PointerByReference();

		if (!ws.contains(wtmp)) {

			status = x11Instance.XGetWindowProperty(dpy, wtmp,
					_NET_WM_WINDOW_TYPE, new NativeLong(0l),
					new NativeLong(1l), false, X11.XA_ATOM, realType,
					realFormat, n_type, n_type_left, data_w_type);
			if (status != X11.Success) {
				LOG.warn("Some error occoured on XGetWindowProperty");
				return 1;
			}

			if (n_type.getValue().intValue() == 0
					|| (n_type.getValue().intValue() != 0 && data_w_type
							.getPointer().getNativeLong(0) != _NET_WM_WINDOW_TYPE_DESKTOP)
					|| (n_type.getValue().intValue() != 0 && data_w_type
							.getPointer().getNativeLong(0) != _NET_WM_WINDOW_TYPE_DESKTOP)) {
				String wn = getWindowName(wtmp);
				ws.add(wtmp);
				LOG.info("Monitoring " + wn);
				xdamageInstance.XDamageCreate(dpy, wtmp,
						ExtX.Xdamage.XDamageReportRawRectangles);
				X11.XWindowAttributes xwa = new X11.XWindowAttributes();

				x11Instance.XGetWindowAttributes(dpy, wtmp, xwa);
				bounds.put(wtmp, new Rectangle(xwa.x, xwa.y, xwa.width,
						xwa.height));
				x11Instance.XSelectInput(dpy, wtmp, new NativeLong(
						X11.SubstructureNotifyMask | X11.StructureNotifyMask));
			}
		}

		return 0;
	}

	@Override
	public void init() throws Exception {
		super.init();

		/* creates list of windows */
		windowList = xd_get_win_list(null);

		destroy = false;

		/* main cycle */
		new Thread("X11EventLoop") {
			public void run() {
				eventLoop();
			}
		}.start();
	}

	public PointerShape getPointerShape() {
		ExtX.XFixes.XFixesCursorImage s = xfixesInstance
				.XFixesGetCursorImage(dpy);

		PointerShape c = new PointerShape();
		c.setWidth(s.width);
		c.setHeight(s.height);
		c.setHotX(s.xhot);
		c.setHotY(s.yhot);
		// c.setX(s.x);
		// c.setY(s.y);

		PointerInfo pm = MouseInfo.getPointerInfo();
		Point location = pm.getLocation();
		c.setX(location.x);
		c.setY(location.y);

		// Turn the ARGB cursor image into TYPE_INT_ARGB we work with
		ByteBuffer buf = s.pixels.getPointer().getByteBuffer(0,
				s.width * s.height * NativeLong.SIZE);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		BufferedImage bim = new BufferedImage(s.width, s.height,
				BufferedImage.TYPE_INT_ARGB);
		WritableRaster raster = bim.getRaster();
		for (int y = 0; y < s.height; y++) {
			for (int x = 0; x < s.width; x++) {
				long z = NativeLong.SIZE == 8 ? buf.getLong() : buf.getInt();
				int b = (int) ((z >> 24) & 0xFF);
				int a = (int) ((z >> 16) & 0xFF);
				int g = (int) ((z >> 8) & 0xFF);
				int r = (int) (z & 0xFF);
				raster.setPixel(x, y, new int[] { a, r, g, b });
			}
		}
		c.setData(bim);
		return c;
	}

	private void eventLoop() {
		ExtX.Xdamage.NotifyEvent dev;
		ExtX.ExtXEvent e = new ExtX.ExtXEvent();
		while (!destroy) {
			x11Instance.XNextEvent(dpy, e);
			// events++;
			// if (events % 1000 == 0) {
			// LOG.info(events + " events");
			// }
			// System.out.println(e.type);
			switch (e.type) {
			case X11.MapNotify:
				X11.XMapEvent xme = (X11.XMapEvent) e.readField("xmap");
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("Mapped window %d",
							xme.window.longValue()));
				}
				check_window_for_add(windowList, xme.window);
				X11.XWindowAttributes xwa = new X11.XWindowAttributes();
				x11Instance.XGetWindowAttributes(dpy, xme.window, xwa);
				Rectangle area3 = new Rectangle(xwa.x, xwa.y, xwa.width,
						xwa.height);
				bounds.put(xme.window, area3);
				fireWindowCreated(getWindowName(xme.window), area3);
				fireDamageEvent("", area3, false, -1);
				break;
			case X11.CreateNotify:
				X11.XCreateWindowEvent xcwe = (X11.XCreateWindowEvent) e
						.readField("xcreatewindow");
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("Created window %d",
							xcwe.window.longValue()));
				}
				break;
			case X11.DestroyNotify:
				X11.XDestroyWindowEvent xdwe = (X11.XDestroyWindowEvent) e
						.readField("xdestroywindow");
				windowList.remove(xdwe.window);
				Rectangle area2 = bounds.remove(xdwe.window);
				xwa = new X11.XWindowAttributes();
				x11Instance.XGetWindowAttributes(dpy, xdwe.window, xwa);
				// if (LOG.isDebugEnabled()) {
				// }
				if (area2 != null) {
					LOG.info(String.format("Destroyed window %d at %s",
							xdwe.window.longValue(), area2));
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
					}
					fireWindowClosed("", area2);
					fireDamageEvent("", area2, true, -1);
				}
				break;
			case X11.ConfigureNotify:
				X11.XConfigureEvent xce = (X11.XConfigureEvent) e
						.readField("xconfigure");
				// System.err.println("WINDOW CONFIGURE N: x = " + xce.x +
				// " y = " + xce.y + " w = " + xce.width + " h = "
				// + xce.height + " b = " + xce.border_width + " t =" +
				// xce.type);
				Rectangle b = bounds.get(xce.window);
				X11.Window theWindow = getFrame(xce.window);
				X11.XWindowAttributes wattr = new X11.XWindowAttributes();
				x11Instance.XGetWindowAttributes(dpy, theWindow, wattr);
				Rectangle newBounds = new Rectangle(wattr.x, wattr.y,
						wattr.width, wattr.height);

				if (b != null) {
					if (b.x != newBounds.x || b.y != newBounds.y) {
						// if (LOG.isDebugEnabled()) {
						LOG.info("Actual window move" + newBounds + " from "
								+ b);
						// }
						bounds.put(theWindow, newBounds);
						fireWindowMoved(getWindowName(theWindow), newBounds, b);
					} else if (b.width != newBounds.width
							|| b.height != newBounds.height) {
						bounds.put(theWindow, newBounds);
						fireWindowResized(getWindowName(theWindow), newBounds,
								b);
					}
				}
				break;
			default:
				if (e.type == damage_event.getValue()
						+ ExtX.Xdamage.XDamageNotify) {
					dev = (ExtX.Xdamage.NotifyEvent) e.readField("xdamage");
					if (windowList.contains(dev.drawable)) {
						xdamageInstance.XDamageSubtract(dpy, dev.damage, null,
								null);
						if (dev.more == 0) {
							/*
							 * Seem to get a lot of damage for the same
							 * locations very quickly. This code tries to limit
							 * that a bit
							 */
							long ts = dev.timestamp.longValue();
							Rectangle newArea = new Rectangle(dev.geometry.x,
									dev.geometry.y, dev.geometry.width,
									dev.geometry.height);
							if (area == null
									| !newArea.equals(area)
									|| (area.equals(newArea) && ts > lastTs + 100)) {
								lastTs = ts;
								area = newArea;
								String wn = getWindowName(dev.drawable);
								if (LOG.isDebugEnabled()) {
									LOG.trace("Real damage at " + area
											+ " from window " + wn);
								}
								fireDamageEvent(wn, area, false, -1);
							}
						}
					}

				}

				else if (e.type == fixes_event.getValue()
						+ ExtX.XFixes.XFixesCursorNotify) {
					// X11.XFixes.CursorNotifyEvent xfcne =
					// (X11.XFixes.CursorNotifyEvent)
					// e.readField("xfixescursor");
					firePointerChange(getPointerShape());
				}
			}
		}
	}

	protected X11.Window getFrame(X11.Window contentWindow) {
		// To get the bounds, we look for the "top-level" window (i.e.
		// the one
		// that is a child of the root window and a parent of this). See
		// http://stackoverflow.com/questions/3233660/how-to-get-accurate-window-information-dimensions-etc-in-linux-x

		X11.WindowByReference rootWindow = new X11.WindowByReference();
		X11.WindowByReference parentWindow = new X11.WindowByReference();
		IntByReference noChildren = new IntByReference();
		PointerByReference data = new PointerByReference();

		X11.Window theWindow = contentWindow;
		while (true) {
			x11Instance.XQueryTree(dpy, theWindow, rootWindow, parentWindow,
					data, noChildren);
			if (rootWindow.getValue().longValue() == parentWindow.getValue()
					.longValue()
					|| rootWindow.getValue().longValue() == contentWindow
							.longValue()) {
				break;
			} else {
				theWindow = parentWindow.getValue();
			}
		}
		return theWindow;
	}

	public static void main(String[] args) throws Exception {
		X11DisplayDriver x11dd = new X11DisplayDriver();
		x11dd.init();
	}

}
