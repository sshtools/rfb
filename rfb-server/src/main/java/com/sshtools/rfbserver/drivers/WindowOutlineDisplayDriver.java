package com.sshtools.rfbserver.drivers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.ImageUtil;
import com.sshtools.rfbserver.DisplayDriver;

/**
 * A driver that can be overlaid over another driver to intercept window
 * movement events and instead of moving the actual window, frame buffer updates
 * containing a wire-frame of the window (significantly reducing CPU and
 * bandwidth requirements for the operations).
 * <p>
 * This requires an underlying driver that fires window events. The driver
 * should be inserted above the underlying driver, any damage detection driver
 * and bounds altering driver. Other overlay drivers may be inserted above this
 * driver.
 * 
 */
public class WindowOutlineDisplayDriver extends FilteredDisplayDriver {

    final static Logger LOG = LoggerFactory.getLogger(WindowOutlineDisplayDriver.class);

    private long lastMoved = -1;
    private BufferedImage backingStore;
    private String windowName;
    private Thread timerThread;
    private int moveTimeout = 1000;
    private int dampening = 100;

    private Rectangle windowBounds;
    private Rectangle oldBounds;
    private Rectangle newWindowBounds;
    private Object lock = new Object();

    public enum Border {
        NORTH, SOUTH, EAST, WEST, BACKGROUND
    }

    private ThreadLocal<Border> drawFrame = new ThreadLocal<Border>();
    private ThreadLocal<Boolean> undraw = new ThreadLocal<Boolean>();

    public WindowOutlineDisplayDriver(DisplayDriver underlyingDriver) {
        super(underlyingDriver, true);
    }

    public int getDampening() {
        return dampening;
    }

    public void setDampening(int dampening) {
        this.dampening = dampening;
    }

    public int getMoveTimeout() {
        return moveTimeout;
    }

    public void setMoveTimeout(int moveTimeout) {
        this.moveTimeout = moveTimeout;
    }

    @Override
    protected void filteredDamageEvent(String name, Rectangle rectangle, boolean important) {
        synchronized (lock) {
            if (backingStore == null) {
                super.filteredDamageEvent(name, rectangle, important);
            }
        }
    }

    @Override
    public BufferedImage grabArea(Rectangle area) {
        synchronized (lock) {
            if (backingStore == null || windowBounds == null) {
                return underlyingDriver.grabArea(area);
            }
            boolean undraw = this.undraw.get();
            if (undraw) {
                BufferedImage bim = new BufferedImage(area.width, area.height, backingStore.getType());
                Graphics2D createGraphics = bim.createGraphics();
                createGraphics.drawImage(backingStore, -area.x, -area.y,  null);
                return bim;
//                return backingStore.getSubimage(area.x, area.y, area.width, area.height);
            } else {
                BufferedImage bim = new BufferedImage(area.width, area.height, backingStore.getType());
                Graphics2D createGraphics = bim.createGraphics();
                createGraphics.setColor(Color.RED);
                createGraphics.drawRect(0, 0, area.width, area.height);
                return bim;
            }

        }
    }

    @Override
    protected void filteredWindowResized(String name, Rectangle rectangle, Rectangle oldRectangle) {
        synchronized (lock) {
            doFakeWindow(name, rectangle, oldRectangle, false);
        }
    }

    @Override
    protected void filteredWindowMoved(String name, Rectangle rectangle, Rectangle oldRectangle) {
        synchronized (lock) {
            doFakeWindow(name, rectangle, oldRectangle, true);
        }
    }

    protected void doFakeWindow(String name, Rectangle rectangle, Rectangle oldRectangle, final boolean move) {
        if (checkLastMoved()) {
            return;
        }
        if (backingStore == null || !name.equals(windowName)) {
            if (backingStore != null) {
                stoppedDrag(move);
            }

            // If dragging has just started
            backingStore = ImageUtil.deepCopy(underlyingDriver.grabArea(new Rectangle(0, 0, getWidth(), getHeight())));
            windowName = name;
            windowBounds = rectangle;
            oldBounds = rectangle;
            
            LOG.info("Starting to drag window " + name + " at " + windowBounds + " (" + move + " move)");
        }
        newWindowBounds = rectangle;
        if (timerThread != null) {
            timerThread.interrupt();
        }
        timerThread = new Thread() {
            public void run() {
                try {
                    sleep(moveTimeout);
                    synchronized (lock) {
                        stoppedDrag(move);
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        if (LOG.isDebugEnabled()) {
            LOG.debug("Dragged window to " + rectangle);
        }

        // Top left to top right
        if (oldBounds != null) {
            fireRectangularDamage(name, oldBounds, true);
        }
        if (rectangle != null) {
            fireRectangularDamage(name, rectangle, false);
        }
        oldBounds = rectangle;
        timerThread.start();
    }

    protected void stoppedDrag(final boolean move) {
        LOG.info("Stopped drag window " + windowName);
        if (timerThread != null) {
            timerThread.interrupt();
        }
        String wn = windowName;
        backingStore = null;
        oldBounds = null;
        windowName = null;
        lastMoved = -1;
        if (move) {
            fireWindowMoved(wn, newWindowBounds, windowBounds);
        } else {
            fireWindowResized(wn, newWindowBounds, windowBounds);
        }
        timerThread = null;
    }

    protected boolean checkLastMoved() {
        long currentTimeMillis = System.currentTimeMillis();
        if (lastMoved != -1 && currentTimeMillis < lastMoved + dampening) {
            return true;
        }
        lastMoved = currentTimeMillis;
        return false;
    }

    @Override
    protected void filteredMouseEvent(int x, int y) {
        super.filteredMouseEvent(x, y);
    }

    @Override
    protected void filteredPointChangeEvent(PointerShape change) {
        super.filteredPointChangeEvent(change);
    }

    protected void fireRectangularDamage(String name, Rectangle rectangle, boolean undraw) {
        if (rectangle.height < 1 || rectangle.width < 1) {
            LOG.warn("Rectangular damage for " + name + " of " + rectangle + " is too small");
            return;
        }
        
        this.undraw.set(undraw);
        
        drawFrame.set(Border.NORTH);
        fireDamageEvent(name, new Rectangle(rectangle.x, rectangle.y, rectangle.width, 1), true, -1);

        drawFrame.set(Border.SOUTH);
        fireDamageEvent(name, new Rectangle(rectangle.x, rectangle.y + rectangle.height - 1, rectangle.width, 1), true, -1);

        drawFrame.set(Border.WEST);
        fireDamageEvent(name, new Rectangle(rectangle.x, rectangle.y + 1, 1, rectangle.height - 2), true, -1);

        drawFrame.set(Border.EAST);
        fireDamageEvent(name, new Rectangle(rectangle.x + rectangle.width - 1, rectangle.y + 1, 1, rectangle.height - 2), true, -1);
    }

}
