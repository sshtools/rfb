package com.sshtools.rfbserver.protocol;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.PixelFormatImageFactory;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbcommon.TightCapability;
import com.sshtools.rfbserver.Beep;
import com.sshtools.rfbserver.DisplayDriver;
import com.sshtools.rfbserver.DisplayDriver.PointerShape;
import com.sshtools.rfbserver.FrameBufferUpdate;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.ServerCut;
import com.sshtools.rfbserver.UpdateRectangle;
import com.sshtools.rfbserver.encodings.CORREEncoding;
import com.sshtools.rfbserver.encodings.CompressLevel0;
import com.sshtools.rfbserver.encodings.CompressLevel1;
import com.sshtools.rfbserver.encodings.CompressLevel2;
import com.sshtools.rfbserver.encodings.CompressLevel3;
import com.sshtools.rfbserver.encodings.CompressLevel4;
import com.sshtools.rfbserver.encodings.CompressLevel5;
import com.sshtools.rfbserver.encodings.CompressLevel6;
import com.sshtools.rfbserver.encodings.CompressLevel7;
import com.sshtools.rfbserver.encodings.CompressLevel8;
import com.sshtools.rfbserver.encodings.CompressLevel9;
import com.sshtools.rfbserver.encodings.CopyRectEncoding;
import com.sshtools.rfbserver.encodings.CursorEncoding;
import com.sshtools.rfbserver.encodings.CursorPositionEncoding;
import com.sshtools.rfbserver.encodings.HextileEncoding;
import com.sshtools.rfbserver.encodings.RFBResizeEncoding;
import com.sshtools.rfbserver.encodings.RFBServerEncoding;
import com.sshtools.rfbserver.encodings.RREEncoding;
import com.sshtools.rfbserver.encodings.RawEncoding;
import com.sshtools.rfbserver.encodings.XCursorEncoding;
import com.sshtools.rfbserver.encodings.ZLIBEncoding;
import com.sshtools.rfbserver.encodings.ZRLEEncoding;

public class RFBEncoder {
    final static Logger LOG = LoggerFactory.getLogger(RFBEncoder.class);

    private Map<Integer, RFBServerEncoding> encodings = new HashMap<Integer, RFBServerEncoding>();
    private List<Integer> enabledEncodings = new ArrayList<Integer>();
    private List<Reply<? extends Object>> damaged = new ArrayList<Reply<?>>();
    private Object lock = new Object();
    private Object waitLock = new Object();
    private RFBClient client;

    private boolean pointerShapeSent;

    public RFBEncoder(RFBClient client) {
        this.client = client;

        addEncoding(new RawEncoding());
        addEncoding(new RREEncoding());

        // TODO not complete
        // addEncoding(new ZRLEEncoding());

        addEncoding(new ZLIBEncoding());
        addEncoding(new CompressLevel0());
        addEncoding(new CompressLevel1());
        addEncoding(new CompressLevel2());
        addEncoding(new CompressLevel3());
        addEncoding(new CompressLevel4());
        addEncoding(new CompressLevel5());
        addEncoding(new CompressLevel6());
        addEncoding(new CompressLevel7());
        addEncoding(new CompressLevel8());
        addEncoding(new CompressLevel9());
        addEncoding(new HextileEncoding());
        addEncoding(new ZRLEEncoding());
        addEncoding(new RFBResizeEncoding());
        addEncoding(new CursorEncoding());
        addEncoding(new CursorPositionEncoding());
        addEncoding(new XCursorEncoding());
        addEncoding(new CORREEncoding());
        addEncoding(new CopyRectEncoding());
    }

    public void addEncoding(RFBServerEncoding enc) {
        if (encodings.containsKey(enc.getCode())) {
            throw new IllegalArgumentException("Encoding with code " + enc.getCode() + " already exists ("
                            + encodings.get(enc.getCode()) + ")");
        }
        encodings.put(enc.getType().getCode(), enc);
    }

    public void removeEncoding(RFBServerEncoding enc) {
        enabledEncodings.remove((Object) enc.getType().getCode());
        encodings.remove(enc.getType().getCode());
    }

    public void resetEncodings() {
        enabledEncodings.clear();
        enabledEncodings.add(RFBConstants.ENC_RAW);
    }

    public boolean isEncodingEnabled(int type) {
        return enabledEncodings.contains(type);
    }

    public List<TightCapability> getAvailableEncodingsAsCapabilities() {
        List<TightCapability> e = new ArrayList<TightCapability>();
        for (Integer i : encodings.keySet()) {
            RFBServerEncoding enc = encodings.get(i);
            if (enc == null) {
                throw new IllegalStateException("Encoding " + i + " is enabled but unknown");
            }
            e.add(enc.getType());
        }
        return e;
    }

    public List<RFBServerEncoding> getEnabledEncodings() {
        List<RFBServerEncoding> e = new ArrayList<RFBServerEncoding>();
        for (Integer i : enabledEncodings) {
            e.add(encodings.get(i));
        }
        return e;
    }

    public RFBServerEncoding getEnabledEncoding(int type) {
        return encodings.containsKey(type) ? encodings.get(type) : null;
    }

    public UpdateRectangle<? extends Object> resizeWindow(DisplayDriver displayDriver, int width, int height) {
        RFBServerEncoding resizeEncoding = getEnabledEncoding(RFBConstants.ENC_NEW_FB_SIZE);
        UpdateRectangle<Void> updateRectangle = resizeEncoding == null ? null : new UpdateRectangle<Void>(displayDriver,
                        new Rectangle(0, 0, width, height), resizeEncoding.getType().getCode());
        if (updateRectangle != null) {
            queueUpdate(updateRectangle);
        }
        return updateRectangle;
    }

    public UpdateRectangle<Void> pointerPositionUpdate(DisplayDriver displayDriver, int x, int y) {
        RFBServerEncoding cursorPositionEncoding = getEnabledEncoding(RFBConstants.ENC_POINTER_POS);
        UpdateRectangle<Void> updateRectangle = cursorPositionEncoding == null ? null : new UpdateRectangle<Void>(displayDriver,
                        new Rectangle(x, y, 0, 0), cursorPositionEncoding.getType().getCode());
        if (updateRectangle != null) {
            queueUpdate(updateRectangle);
        }
        return updateRectangle;
    }

    public UpdateRectangle<PointerShape> pointerShapeUpdate(DisplayDriver displayDriver, PointerShape change) {
        RFBServerEncoding pointerShapeEncoding = getEnabledEncoding(RFBConstants.ENC_X11_CURSOR);
        if (pointerShapeEncoding == null) {
            pointerShapeEncoding = getEnabledEncoding(RFBConstants.ENC_RICH_CURSOR);
            if (pointerShapeEncoding == null) {
                return null;
            }
        }
        UpdateRectangle<PointerShape> upd = new UpdateRectangle<PointerShape>(displayDriver, new Rectangle(change.getHotX(),
                        change.getHotY(), change.getWidth(), change.getHeight()), pointerShapeEncoding.getType().getCode());
        upd.setData(change);
        queueUpdate(upd);
        return upd;
    }

    public UpdateRectangle<BufferedImage> frameUpdate(DisplayDriver displayDriver, Rectangle rectangle, int preferredEncoding) {
        return frameUpdate(displayDriver, rectangle, false, preferredEncoding);
    }

    public UpdateRectangle<BufferedImage> frameUpdate(DisplayDriver displayDriver, Rectangle rectangle, boolean important,
                                                      int preferredEncoding) {
        UpdateRectangle<BufferedImage> rect = doFrameUpdate(displayDriver, rectangle, null, important, preferredEncoding);
        if (rect != null) {
            queueUpdate(rect);
        }
        return rect;
    }

    private UpdateRectangle<BufferedImage> doFrameUpdate(DisplayDriver displayDriver, Rectangle rectangle,
                                                         UpdateRectangle<BufferedImage> update, boolean important,
                                                         int updatePreferredEncoding) {

        assert rectangle != null;
        assert displayDriver != null;
        if (rectangle.x < 0) {
            rectangle.x = 0;
        } else if (rectangle.x > displayDriver.getWidth()) {
            rectangle.x = displayDriver.getWidth();
        }
        if (rectangle.y < 0) {
            rectangle.y = 0;
        } else if (rectangle.y > displayDriver.getHeight()) {
            rectangle.y = displayDriver.getHeight();
        }
        if (rectangle.width + rectangle.x > displayDriver.getWidth()) {
            rectangle.width = displayDriver.getWidth() - rectangle.x;
        }
        if (rectangle.height + rectangle.y > displayDriver.getHeight()) {
            rectangle.height = displayDriver.getHeight() - rectangle.y;
        }
        System.err.println(">>>> " +rectangle);
        if (rectangle.width == 0 || rectangle.height == 0) {
            // Update is out of bounds
            LOG.warn("Rectangle out of bounds, skipping: " + rectangle);
            return null;
        }
        if (update == null) {
            update = new UpdateRectangle<BufferedImage>(displayDriver, rectangle, updatePreferredEncoding);
        } else {
            update.setEncoding(updatePreferredEncoding);
            update.setArea(rectangle);
        }
        update.setImportant(important);

        // Create compatible image to send
        BufferedImage img = new PixelFormatImageFactory(client.getPixelFormat()).create(rectangle.width, rectangle.height);
        img.getGraphics().drawImage(displayDriver.grabArea(rectangle), 0, 0, null);
        update.setData(img);

        return update;
    }

    public int getPreferredEncoding() {
        for (int i : enabledEncodings) {
            if (encodings.containsKey(i) && !encodings.get(i).isPseudoEncoding() && i != RFBConstants.ENC_COPYRECT) {
                return i;
            }
        }
        return RFBConstants.ENC_RAW;
    }

    public boolean isAvailable(int enc) {
        return encodings.containsKey(enc);
    }

    public void clearEnabled() {
        enabledEncodings.clear();
    }

    public void enable(RFBClient client, int enc) {
        RFBServerEncoding enco = encodings.get(enc);
        if (enco == null) {
            LOG.warn("No such encoding as " + enc);
        } else {
            enabledEncodings.add(enc);
            enco.selected(client);
            LOG.info("Enabling " + enco.getType().getSignature());
        }
    }

    private void queueUpdate(int position, Reply<?> reply) {
        if (reply != null) {
            synchronized (lock) {
                damaged.add(position == -1 ? damaged.size() : position, reply);
                synchronized (waitLock) {
                    waitLock.notifyAll();
                }
            }
        }
    }

    // private void queueUpdate(int position, Reply<?> reply) {
    // if (reply != null) {
    // synchronized (lock) {
    // for (Iterator<Reply<?>> it = damaged.iterator(); it.hasNext();) {
    // Reply<?> r = it.next();
    // if (reply r instanceof UpdateRectangle) {
    // UpdateRectangle<?> u = (UpdateRectangle<?>) r;
    // RFBServerEncoding enc = u.getEncoding() == -1 ? null :
    // encodings.get(u.getEncoding());
    // if (enc == null || !enc.isPseudoEncoding()) {
    // Rectangle area = u.getArea();
    // UpdateRectangle<?> update = (UpdateRectangle<?>) reply;
    // Rectangle updateArea = update.getArea();
    // if (area.contains(updateArea) || area.equals(updateArea)) {
    // // There is already an update that covers this
    // // area
    // if (!update.isImportant()) {
    // // TODO optimise to only update the enclosed
    // // rectangle
    // doFrameUpdate(update.getDriver(), update.getArea(),
    // (UpdateRectangle<BufferedImage>) update,
    // update.isImportant(), update.getEncoding());
    //
    // return;
    // }
    // } else if (updateArea.contains(area)) {
    // // Our update contains an existing update
    // if (!u.isImportant()) {
    // it.remove();
    // }
    // }
    // }
    // }
    // }
    // LOG.info("Damaged " + reply);
    // damaged.add(position == -1 ? damaged.size() : position, reply);
    // synchronized (waitLock) {
    // waitLock.notifyAll();
    // }
    // }
    // }
    // }

    public void queueUpdate(UpdateRectangle<?> update) {
        synchronized (lock) {
            // Look for a frame buffer update to add the rectangle too
            FrameBufferUpdate fbu = null;
            for (Reply<?> f : damaged) {
                if (f instanceof FrameBufferUpdate) {
                    fbu = (FrameBufferUpdate) f;
                    break;
                }
            }
            if (fbu == null) {
                fbu = new FrameBufferUpdate(client.getPixelFormat(), this);
                fbu.getData().add(update);
                queueUpdate(fbu);
            } else {
                fbu.getData().add(update);
            }
        }
    }

    public RFBClient getClient() {
        return client;
    }

    public void queueUpdate(Reply<?> update) {
        queueUpdate(-1, update);
    }

    public Object getLock() {
        return lock;
    }

    public List<Reply<?>> popUpdates() {
        synchronized (lock) {
            List<Reply<?>> rep = new ArrayList<>(damaged);
            damaged.clear();
            return rep;
        }
    }

    public void beep(DisplayDriver displayDriver) {
        queueUpdate(new Beep());
    }

    public void clipboardChanged(DisplayDriver displayDriver, FlavorEvent e) {
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
        try {
            queueUpdate(new ServerCut((String) t.getTransferData(DataFlavor.stringFlavor)));
        } catch (Exception ex) {
            LOG.error("Failed to get clipboard contents.", ex);
        }
    }

    public boolean waitForUpdates(long timeout) {
        synchronized (waitLock) {
            if (!damaged.isEmpty()) {
                return true;
            }

            try {
                waitLock.wait(timeout);
            } catch (InterruptedException e) {
            }
            return !damaged.isEmpty();
        }
    }

    public void clearUpdates() {
        damaged.clear();        
    }
    
    public boolean isPointerShapeSent() {
        return pointerShapeSent;
    }
    
    public void pointerShapeSent() {
        pointerShapeSent = true;
    }

    public void resetPointerShape() {
        pointerShapeSent = false;        
    }
}
