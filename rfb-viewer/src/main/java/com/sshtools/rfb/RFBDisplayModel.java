/*
 */
package com.sshtools.rfb;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.PixelFormatImageFactory;

public class RFBDisplayModel extends PixelFormat {
	final static Logger LOG = LoggerFactory.getLogger(ProtocolEngine.class);

	public final static int RGB = 0;
	public final static int BGR = 1;

	// Remote desktop information

	private int imagex, imagey;
	private String rfbName;
	private int rfbWidth;
	private int rfbHeight;
	private ColorModel colorModel;
	private double xscale, yscale;
	private BufferedImage imageBuffer;
	private Graphics graphicBuffer;
	private RFBDisplay display;
	boolean showSoftCursor = false;
	private Image softCursor;
	private int cursorX = 0, cursorY = 0;
	private int cursorWidth, cursorHeight;
	private int hotX, hotY;
	private RFBContext context;
	private BufferedImage lastRect;
	private Object lock = new Object();

	private PixelFormatImageFactory imageFactory;

	public Image getCursor() {
		return softCursor;
	}

	public Point getCursorHotspot() {
		return new Point(hotX, hotY);
	}

	public boolean hasCursor() {
		return showSoftCursor;
	}

	public Rectangle getCursorRect() {
		return new Rectangle(cursorX - hotX, cursorY - hotY, cursorWidth,
				cursorHeight);
	}

	/**
	 * @return Returns the graphicBuffer.
	 */
	public Graphics getGraphicBuffer() {
		return graphicBuffer;
	}

	/**
	 * @param graphicBuffer
	 *            The graphicBuffer to set.
	 */
	public void setGraphicBuffer(Graphics graphicBuffer) {
		this.graphicBuffer = graphicBuffer;
	}

	public void setContext(RFBContext context) {
		this.context = context;
	}

	public void reset() {

	}

	public BufferedImage getImageBuffer() {
		return imageBuffer;
	}

	public RFBDisplayModel(RFBDisplay display) {
		super();
		this.display = display;
		init();
	}

	public void init() {
		imageFactory = new PixelFormatImageFactory(this);
		xscale = 1.0;
		yscale = 1.0;
		imagex = 0;
		imagey = 0;
		rfbName = null;
		rfbWidth = 0;
		rfbHeight = 0;
		colorModel = null;
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
			Dimension s = display.getDisplayComponent().getSize();
			if (display.getContext().getScaleMode() == RFBDisplay.NO_SCALING) {
				if (s.width > rfbWidth) {
					imagex = (s.width - rfbWidth) / 2;
				} else {
					imagex = 0;
				}
				if (s.height > rfbHeight) {
					imagey = (s.height - rfbHeight) / 2;
				} else {
					imagey = 0;
				}
				xscale = 1;
				yscale = 1;
			} else {
				imagex = 0;
				imagey = 0;
				xscale = ((double) s.width / (double) rfbWidth);
				yscale = ((double) s.height / (double) rfbHeight);
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
			BufferedImage prev = imageBuffer;
			if (rfbWidth == 0 || rfbHeight == 0) {
				return;
			}
			updateScale(display);
			recreateImage();
			if (prev != null && graphicBuffer != null) {
				graphicBuffer.drawImage(prev, 0, 0, null);
			}
			display.resizeComponent();
		}
	}

	private void recreateImage() {
		int width = rfbWidth;
		int height = rfbHeight;
		if (imageBuffer == null) {
			imageBuffer = imageFactory.create(width, height);
		} else if (imageBuffer.getWidth() != width
				|| imageBuffer.getHeight() != height) {
			synchronized (imageBuffer) {
				imageBuffer = imageFactory.create(width, height);
			}
		}
		LOG.info("Image is now " + imageBuffer.getType() + " and " + width
				+ " x " + height);
		graphicBuffer = imageBuffer == null ? null : imageBuffer.getGraphics();
	}

	public ColorModel getColorModel() {
		return colorModel;
	}

	public void changeFramebufferSize(int width, int height) {
		rfbWidth = width;
		rfbHeight = height;
		updateBuffer();
	}

	public void paintBuffer(Graphics g, ImageObserver imageObserver) {
		paintBuffer(g, imageObserver, imagex, imagey, xscale, yscale, display
				.getContext().getScaleMode());
	}

	public void paintBuffer(Graphics g, ImageObserver imageObserver,
			int imagex, int imagey, double xscale, double yscale, int scaleMode) {

		if (scaleMode != RFBDisplay.NO_SCALING) {
			switch (scaleMode) {
			case RFBDisplay.BILINEAR:
				((java.awt.Graphics2D) g).setRenderingHint(
						java.awt.RenderingHints.KEY_INTERPOLATION,
						java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				break;
			case RFBDisplay.BICUBIC:
				((java.awt.Graphics2D) g).setRenderingHint(
						java.awt.RenderingHints.KEY_INTERPOLATION,
						java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				break;
			default:
				((java.awt.Graphics2D) g)
						.setRenderingHint(
								java.awt.RenderingHints.KEY_INTERPOLATION,
								java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				break;
			}
			g.drawImage(imageBuffer, imagex, imagey,
					(int) (imageBuffer.getWidth(imageObserver) * xscale),
					(int) (imageBuffer.getHeight(imageObserver) * yscale), null);
		} else {
			g.drawImage(imageBuffer, imagex, imagey,
					imageBuffer.getWidth(imageObserver),
					imageBuffer.getHeight(imageObserver), null);
		}

		if (showSoftCursor) {
			paintCursor(g);
		}
	}

	public void paintCursor(Graphics g) {
		Rectangle clip = getCursorRect();
		clip.x += imagex;
		clip.x *= xscale;
		clip.y += imagey;
		clip.y *= yscale;
		clip.height *= yscale;
		clip.width *= xscale;
		Rectangle displayClip = new Rectangle(clip);

		displayClip.x = Math.max(imagex, displayClip.x);
		displayClip.y = Math.max(imagey, displayClip.y);
		int mx = imagex + (int) (rfbWidth * xscale);
		int my = imagey + (int) (rfbHeight * yscale);
		int xover = (displayClip.x + displayClip.width) - mx;
		if (xover > 0) {
			displayClip.width -= xover;
		}
		int yover = (displayClip.y + displayClip.height) - my;
		if (yover > 0) {
			displayClip.height -= yover;
		}
		Rectangle oclip = g.getClipBounds();
		g.setClip(displayClip);
		g.drawImage(softCursor, clip.x, clip.y, clip.width, clip.height, null);
		g.setClip(oclip);

	}

	public synchronized void updateCursor(BufferedImage cursorImg, int x,
			int y, int width, int height) {
		softCursorFree();
		showSoftCursor = context.isCursorUpdatesRequested()
				&& !context.isCursorUpdateIgnored()
				&& !context.isLocalCursorDisplayed();

		softCursor = cursorImg;

		cursorWidth = width;
		cursorHeight = height;
		hotX = x;
		hotY = y;

		display.getEngine().updateCursor(cursorX, cursorY);

	}

	public synchronized void softCursorMove(int x, int y) {

		showSoftCursor = context.isCursorUpdatesRequested()
				&& !context.isCursorUpdateIgnored()
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
			display.requestRepaint(display.getContext()
					.getCursorUpdateTimeout(), ox - hotX, oy - hotY,
					cursorWidth, cursorHeight);
			display.requestRepaint(display.getContext()
					.getCursorUpdateTimeout(), ox - hotX, oy - hotY,
					cursorWidth, cursorHeight);
		}
	}

	public synchronized void softCursorFree() {
		if (showSoftCursor) {
			showSoftCursor = false;
			softCursor = null;
			display.requestRepaint(display.getContext()
					.getCursorUpdateTimeout(), cursorX - hotX, cursorY - hotY,
					cursorWidth, cursorHeight);
		}
	}

	public void drawRectangle(final int x, final int y, final int width,
			final int height, final int color) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					drawRectangle(x, y, width, height, color);
				}
			});
			return;
		}
		graphicBuffer.setColor(new Color(color));
		graphicBuffer.fillRect(x, y, width, height);
		display.requestRepaint(display.getContext().getScreenUpdateTimeout(),
				x, y, width, height);
	}

	public void drawRectangle(final int x, final int y, final int width,
			final int height, BufferedImage bim) {
		if (bim == null) {
			bim = lastRect;
		} else {
			lastRect = bim;
		}
		if (bim != null) {
			graphicBuffer.drawImage(bim, x, y, null);
			display.requestRepaint(display.getContext()
					.getScreenUpdateTimeout(), x, y, width, height);
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

	public PixelFormatImageFactory getFactory() {
		return imageFactory;
	}

}
