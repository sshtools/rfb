/*
 */
package com.sshtools.rfb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfb.RFBToolkit.RFBGraphicsContext;
import com.sshtools.rfb.RFBToolkit.RFBImage;
import com.sshtools.rfbcommon.PixelFormat;

public class RFBDisplayModel extends PixelFormat {
	final static Logger LOG = LoggerFactory.getLogger(ProtocolEngine.class);

	public final static int RGB = 0;
	public final static int BGR = 1;

	// Remote desktop information

	private int imagex, imagey;
	private String rfbName;
	private int rfbWidth;
	private int rfbHeight;
	private double xscale, yscale;
	private RFBImage imageBuffer;
	private RFBGraphicsContext graphicBuffer;
	private RFBDisplay display;
	boolean showSoftCursor = false;
	private RFBImage softCursor;
	private int cursorX = 0, cursorY = 0;
	private int cursorWidth, cursorHeight;
	private int hotX, hotY;
	private RFBContext context;
	private RFBImage lastRect;
	private Object lock = new Object();

	public RFBImage getCursor() {
		return softCursor;
	}

	public int[] getCursorHotspot() {
		return new int[] { hotX, hotY };
	}

	public boolean hasCursor() {
		return showSoftCursor;
	}

	public int[] getCursorRect() {
		return new int[] { cursorX - hotX, cursorY - hotY, cursorWidth, cursorHeight };
	}

	/**
	 * @return Returns the graphicBuffer.
	 */
	public RFBGraphicsContext getGraphicBuffer() {
		return graphicBuffer;
	}

	/**
	 * @param graphicBuffer
	 *            The graphicBuffer to set.
	 */
	public void setGraphicBuffer(RFBGraphicsContext graphicBuffer) {
		this.graphicBuffer = graphicBuffer;
	}

	public void setContext(RFBContext context) {
		this.context = context;
	}

	public void reset() {

	}

	public RFBImage getImageBuffer() {
		return imageBuffer;
	}

	public RFBDisplayModel(RFBDisplay display) {
		super();
		this.display = display;
		init();
	}

	public void init() {
		xscale = 1.0;
		yscale = 1.0;
		imagex = 0;
		imagey = 0;
		rfbName = null;
		rfbWidth = 0;
		rfbHeight = 0;
		imageBuffer = null;
		graphicBuffer = null;
		updateBuffer();

	}

	public int getRfbHeight() {
		return rfbHeight;
	}

	public void setRfbHeight(int rfbHeight) {
		this.rfbHeight = rfbHeight;
	}

	public String getRfbName() {
		return rfbName;
	}

	public void setRfbName(String rfbName) {
		this.rfbName = rfbName;
	}

	public int getRfbWidth() {
		return rfbWidth;
	}

	public void setRfbWidth(int rfbWidth) {
		this.rfbWidth = rfbWidth;
	}

	public double getXscale() {
		return xscale;
	}

	public void setXscale(double xscale) {
		this.xscale = xscale;
	}

	public double getYscale() {
		return yscale;
	}

	public void setYscale(double yscale) {
		this.yscale = yscale;
	}

	public void updateScale(RFBDisplay display) {
		if (display.getDisplayComponent() == null) {

		} else {
			int[] s = display.getDisplayComponentSize();
			if (display.getContext().getScaleMode() == RFBDisplay.NO_SCALING) {
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

	public int getImagex() {
		return imagex;
	}

	public void setImagex(int imagex) {
		this.imagex = imagex;
	}

	public int getImagey() {
		return imagey;
	}

	public void setImagey(int imagey) {
		this.imagey = imagey;
	}

	public void updateBuffer() {
		synchronized (lock) {
			RFBImage prev = imageBuffer;
			if (rfbWidth == 0 || rfbHeight == 0) {
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

	private void recreateImage() {
		int width = rfbWidth;
		int height = rfbHeight;
		if (imageBuffer == null) {
			imageBuffer = RFBToolkit.get().createImage(this, width, height);
		} else if (imageBuffer.getWidth() != width || imageBuffer.getHeight() != height) {
			synchronized (imageBuffer) {
				imageBuffer = RFBToolkit.get().createImage(this, width, height);
			}
		}
		LOG.info("Image is now " + imageBuffer.getType() + " and " + width + " x " + height);
		graphicBuffer = imageBuffer == null ? null : imageBuffer.getGraphicsContext();
	}

	public void changeFramebufferSize(int width, int height) {
		rfbWidth = width;
		rfbHeight = height;
		updateBuffer();
	}

	public void paintBuffer(RFBGraphicsContext g) {
		paintBuffer(g, imagex, imagey, xscale, yscale, display.getContext().getScaleMode());
	}

	public void paintBuffer(RFBGraphicsContext g, int imagex, int imagey, double xscale, double yscale, int scaleMode) {

		g.drawImage(imageBuffer, imagex, imagey, (int) (imageBuffer.getWidth() * xscale),
				(int) (imageBuffer.getHeight() * yscale), scaleMode);
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
		int mx = imagex + (int) (rfbWidth * xscale);
		int my = imagey + (int) (rfbHeight * yscale);
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
		if (cursorX >= rfbWidth) {
			cursorX = rfbWidth - 1;
		}
		if (cursorY >= rfbHeight) {
			cursorY = rfbHeight - 1;
		}
		if (showSoftCursor) {
			display.requestRepaint(display.getContext().getCursorUpdateTimeout(), ox - hotX, oy - hotY, cursorWidth,
					cursorHeight);
			display.requestRepaint(display.getContext().getCursorUpdateTimeout(), ox - hotX, oy - hotY, cursorWidth,
					cursorHeight);
		}
	}

	public synchronized void softCursorFree() {
		if (showSoftCursor) {
			showSoftCursor = false;
			softCursor = null;
			display.requestRepaint(display.getContext().getCursorUpdateTimeout(), cursorX - hotX, cursorY - hotY,
					cursorWidth, cursorHeight);
		}
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

	public int getHotX() {
		return hotX;
	}

	public int getHotY() {
		return hotY;
	}

	public Object getLock() {
		return lock;
	}

}
