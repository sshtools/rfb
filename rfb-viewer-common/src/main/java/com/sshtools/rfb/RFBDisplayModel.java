/*
 */
package com.sshtools.rfb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfb.RFBToolkit.RFBGraphicsContext;
import com.sshtools.rfb.RFBToolkit.RFBImage;
import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ScreenData;

public class RFBDisplayModel extends PixelFormat {

	final static Logger LOG = LoggerFactory.getLogger(ProtocolEngine.class);
	
	public final static int BGR = 1;
	public final static int RGB = 0;
	
	private boolean showSoftCursor = false;
	private RFBContext context;
	private int cursorWidth, cursorHeight;
	private int cursorX = 0, cursorY = 0;
	private RFBDisplay<?, ?> display;
	private RFBGraphicsContext graphicBuffer;
	private int hotX, hotY;
	private RFBImage imageBuffer;
	private int imagex, imagey;
	private RFBImage lastRect;
	private Object lock = new Object();
	private String rfbName;
	private ScreenData screenData = new ScreenData();
	private RFBImage softCursor;
	private double xscale, yscale;

	public RFBDisplayModel(RFBDisplay<?, ?> display) {
		super();
		this.display = display;
		init();
	}

	public void changeFramebufferSize(int origin, ScreenData screenData) {
		this.screenData.set(screenData);
		updateBuffer();
	}

	public void drawRectangle(final int x, final int y, final int width, final int height, final int color) {
		graphicBuffer.setColor(RFBToolkit.get().newColor().setRGB(color));
		graphicBuffer.fillRect(x, y, width, height);
		display.requestRepaint(display.getContext().getScreenUpdateTimeout(), x, y, width, height);
	}

	public void drawRectangle(final int x, final int y, final int width, final int height, RFBImage bim) {
		if (bim == null) {
			bim = lastRect;
		} else {
			lastRect = bim;
		}
		if (bim != null) {
			graphicBuffer.drawImage(bim, x, y);
			display.requestRepaint(display.getContext().getScreenUpdateTimeout(), x, y, width, height);
		} else {
			LOG.warn("Request to draw last rectangle when there was none.");
		}
	}

	public RFBImage getCursor() {
		return softCursor;
	}

	public int[] getCursorHotspot() {
		return new int[] { hotX, hotY };
	}

	public int[] getCursorRect() {
		return new int[] { cursorX - hotX, cursorY - hotY, cursorWidth, cursorHeight };
	}

	public RFBGraphicsContext getGraphicBuffer() {
		return graphicBuffer;
	}

	public int getHotX() {
		return hotX;
	}

	public int getHotY() {
		return hotY;
	}

	public RFBImage getImageBuffer() {
		return imageBuffer;
	}

	public int getImagex() {
		return imagex;
	}

	public int getImagey() {
		return imagey;
	}

	public Object getLock() {
		return lock;
	}

	public int getRfbHeight() {
		return screenData.getHeight();
	}

	public String getRfbName() {
		return rfbName;
	}

	public int getRfbWidth() {
		return screenData.getWidth();
	}

	public ScreenData getScreenData() {
		return screenData;
	}

	public double getXscale() {
		return xscale;
	}

	public double getYscale() {
		return yscale;
	}

	public boolean hasCursor() {
		return showSoftCursor;
	}

	public void init() {
		xscale = 1.0;
		yscale = 1.0;
		imagex = 0;
		imagey = 0;
		rfbName = null;
		screenData.reset();
		imageBuffer = null;
		graphicBuffer = null;
		updateBuffer();
	}

	public void paintBuffer(RFBGraphicsContext g) {
		paintBuffer(g, imagex, imagey, xscale, yscale, display.getContext().getScaleMode());
	}

	public void paintBuffer(RFBGraphicsContext g, int imagex, int imagey, double xscale, double yscale, int scaleMode) {
		g.drawImage(imageBuffer, imagex, imagey, (int) (imageBuffer.getWidth() * xscale), (int) (imageBuffer.getHeight() * yscale),
				scaleMode);
		if (showSoftCursor) {
			paintCursor(g);
		}
	}

	public void paintCursor(RFBGraphicsContext g) {
		int[] clip = getCursorRect();
		clip[0] += imagex;
		clip[0] *= xscale;
		clip[1] += imagey;
		clip[1] *= yscale;
		clip[2] *= yscale;
		clip[3] *= xscale;
		int[] displayClip = new int[4];
		System.arraycopy(clip, 0, displayClip, 0, 4);
		displayClip[0] = Math.max(imagex, displayClip[0]);
		displayClip[1] = Math.max(imagey, displayClip[1]);
		int mx = imagex + (int) (screenData.getWidth() * xscale);
		int my = imagey + (int) (screenData.getHeight() * yscale);
		int xover = (displayClip[0] + displayClip[2]) - mx;
		if (xover > 0) {
			displayClip[2] -= xover;
		}
		int yover = (displayClip[0] + displayClip[3]) - my;
		if (yover > 0) {
			displayClip[3] -= yover;
		}
		int[] oclip = g.getClipBounds();
		g.setClip(displayClip);
		g.drawImage(softCursor, clip[0], clip[1], clip[2], clip[3], 0);
		g.setClip(oclip);
	}

	public void reset() {
	}

	public void setContext(RFBContext context) {
		this.context = context;
	}

	public void setGraphicBuffer(RFBGraphicsContext graphicBuffer) {
		this.graphicBuffer = graphicBuffer;
	}

	public void setImagex(int imagex) {
		this.imagex = imagex;
	}

	public void setImagey(int imagey) {
		this.imagey = imagey;
	}

	public void setRfbName(String rfbName) { 
		this.rfbName = rfbName;
	}

	public void setXscale(double xscale) {
		this.xscale = xscale;
	}

	public void setYscale(double yscale) {
		this.yscale = yscale;
	}

	public synchronized void softCursorFree() {
		if (showSoftCursor) {
			showSoftCursor = false;
			softCursor = null;
			display.requestRepaint(display.getContext().getCursorUpdateTimeout(), cursorX - hotX, cursorY - hotY, cursorWidth,
					cursorHeight);
		}
	}

	public synchronized void softCursorMove(int x, int y) {
		showSoftCursor = context.isCursorUpdatesRequested() && !context.isCursorUpdateIgnored()
				&& !context.isLocalCursorDisplayed();
		int ox = cursorX;
		int oy = cursorY;
		cursorX = x;
		cursorY = y;
		if (cursorX < 0) {
			cursorX = 0;
		}
		if (cursorY < 0) {
			cursorY = 0;
		}
		if (cursorX >= screenData.getWidth()) {
			cursorX = screenData.getWidth() - 1;
		}
		if (cursorY >= screenData.getHeight()) {
			cursorY = screenData.getHeight() - 1;
		}
		if (showSoftCursor) {
			display.requestRepaint(display.getContext().getCursorUpdateTimeout(), ox - hotX, oy - hotY, cursorWidth, cursorHeight);
			display.requestRepaint(display.getContext().getCursorUpdateTimeout(), ox - hotX, oy - hotY, cursorWidth, cursorHeight);
		}
	}

	public void updateBuffer() {
		synchronized (lock) {
			RFBImage prev = imageBuffer;
			if (screenData.isEmpty()) {
				return;
			}
			updateScale(display);
			recreateImage();
			if (prev != null && graphicBuffer != null) {
				graphicBuffer.drawImage(prev, 0, 0);
			}
			display.resizeComponent();
		}
	}

	public synchronized void updateCursor(RFBImage cursorImg, int x, int y, int width, int height) {
		softCursorFree();
		showSoftCursor = context.isCursorUpdatesRequested() && !context.isCursorUpdateIgnored()
				&& !context.isLocalCursorDisplayed();
		softCursor = cursorImg;
		cursorWidth = width;
		cursorHeight = height;
		hotX = x;
		hotY = y;
		display.getEngine().updateCursor(cursorX, cursorY);
	}

	public void updateScale(RFBDisplay<?, ?> display) {
		if (display.getDisplayComponent() == null) {
		} else {
			int rfbWidth = screenData.getWidth();
			int rfbHeight = screenData.getHeight();
			int[] s = display.getDisplayComponentSize();
			if (display.getContext().getScaleMode() == RFBDisplay.NO_SCALING || display.getContext().getScaleMode() == RFBDisplay.RESIZE_DESKTOP) {
				if (s[0] > rfbWidth) {
					imagex = (s[0] - rfbWidth) / 2;
				} else {
					imagex = 0;
				}
				if (s[1] > rfbHeight) {
					imagey = (s[1] - rfbHeight) / 2;
				} else {
					imagey = 0;
				}
				xscale = 1;
				yscale = 1;
			} else {
				imagex = 0;
				imagey = 0;
				xscale = ((double) s[0] / (double) rfbWidth);
				yscale = ((double) s[1] / (double) rfbHeight);
			}
		}
	}

	private void recreateImage() {
		int width = screenData.getWidth();
		int height = screenData.getHeight();
		if (imageBuffer == null) {
			imageBuffer = RFBToolkit.get().createImage(this, width, height);
		} else if (imageBuffer.getWidth() != width || imageBuffer.getHeight() != height) {
			synchronized (imageBuffer) {
				imageBuffer = RFBToolkit.get().createImage(this, width, height);
			}
		}
		else {
			return;
		}
		LOG.info("Image is now " + imageBuffer.getType() + " and " + width + " x " + height);
		graphicBuffer = imageBuffer == null ? null : imageBuffer.getGraphicsContext();
	}
}
