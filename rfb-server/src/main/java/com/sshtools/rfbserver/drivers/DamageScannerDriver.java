package com.sshtools.rfbserver.drivers;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JLabel;

import com.sshtools.rfbserver.DisplayDriver;

public class DamageScannerDriver extends FilteredDisplayDriver {

	private Timer timer;
	private int tileSize = 256;
	private int tileX = -1;
	private int tileY = 0;
	private BufferedImage backingStore;
	private ThreadLocal<QueuedImage> queuedImage = new ThreadLocal<DamageScannerDriver.QueuedImage>();
	private File dir;
	private int idx;
	private long lastDamage = -1;
	private List<Rectangle> damage = new ArrayList<Rectangle>();
	private int scanInterval = 75;
	private BufferedImage currentTile;

	private JLabel label;
	private BufferedImage currentReducedImage;

	private final static int DAMAGE_WAIT = 1000;

	class QueuedImage {
		BufferedImage img;
		Rectangle area;

		public QueuedImage(BufferedImage img, Rectangle area) {
			super();
			this.img = img;
			this.area = area;
		}

	}

	public DamageScannerDriver(DisplayDriver underlyingDriver, boolean initAndDestroy) {
		super(underlyingDriver, initAndDestroy);
	}

	public int getTileSize() {
		return tileSize;
	}

	public void setTileSize(int tileSize) {
		this.tileSize = tileSize;
	}

	public int getScanInterval() {
		return scanInterval;
	}

	public void setScanInterval(int scanInterval) {
		this.scanInterval = scanInterval;
	}

	@Override
	public void init() throws Exception {
		super.init();

		// JFrame f = new JFrame("Backing store monitor");
		// f.getContentPane().add(new JScrollPane(label = new JLabel()));
		// f.setSize(800, 600);
		// f.setVisible(true);

		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				nextTile();
			}
		}, scanInterval, scanInterval);
	}

	@Override
	public void destroy() {
		super.destroy();
		timer.cancel();
		dispose(backingStore);
		dispose(currentTile);
		dispose(currentReducedImage);
	}

	private void dispose(BufferedImage img) {
		if (img != null) {
			img.getGraphics().dispose();
		}
	}

	@Override
	protected void filteredWindowMoved(String name, Rectangle rectangle, Rectangle oldRectangle) {
		lastDamage = System.currentTimeMillis();
		super.filteredWindowMoved(name, rectangle, oldRectangle);
	}

	@Override
	protected void filteredWindowResized(String name, Rectangle rectangle, Rectangle oldRectangle) {
		lastDamage = System.currentTimeMillis();
		super.filteredWindowResized(name, rectangle, oldRectangle);
	}

	@Override
	protected void filteredDamageEvent(String name, Rectangle rectangle, boolean important) {
		super.filteredDamageEvent(name, rectangle, important);
		synchronized (damage) {
			damage.add(rectangle);
		}
	}

	void nextTile() {
		if (backingStore == null) {
			return;
		}
		if (lastDamage != -1 && System.currentTimeMillis() < lastDamage + DAMAGE_WAIT) {
			return;
		}

		tileX++;
		int pixelX = tileX * tileSize;
		int pixelY = tileY * tileSize;
		int height = getHeight();
		int width = getWidth();
		if (pixelX >= width) {
			pixelX = 0;
			tileX = 0;
			tileY++;
			pixelY += tileSize;
			if (pixelY >= height) {
				tileY = 0;
				pixelY = 0;
			}
		}

		int pixelW = tileSize;
		int pixelH = tileSize;

		if (pixelX + pixelW >= width) {
			pixelW = width - pixelX;
		}
		if (pixelY + pixelH >= height) {
			pixelH = height - pixelY;
		}

		// When the scanned rectangle intersects something that has recently
		// been damaged, skip it
		Rectangle area = new Rectangle(pixelX, pixelY, pixelW, pixelH);
		synchronized (damage) {
			for (Iterator<Rectangle> dit = damage.iterator(); dit.hasNext();) {
				if (area.intersects(dit.next())) {
					return;
				}
			}
		}

		// Get the current image from the underlying driver
		BufferedImage currentImage = underlyingDriver.grabArea(area);
		if (currentReducedImage == null || currentReducedImage.getWidth() != pixelW || currentReducedImage.getHeight() != pixelH) {
			currentReducedImage = new BufferedImage(pixelW, pixelH, BufferedImage.TYPE_BYTE_GRAY);
		}
		currentReducedImage.getGraphics().drawImage(currentImage, 0, 0, pixelW, pixelH, 0, 0, pixelW, pixelH, null);

		// Get the image from the backing store
		if (currentTile == null || currentTile.getWidth() != pixelW || currentTile.getHeight() != pixelH) {
			currentTile = new BufferedImage(pixelW, pixelH, BufferedImage.TYPE_BYTE_GRAY);
		}
		currentTile.getGraphics().drawImage(backingStore, 0, 0, pixelW, pixelH, pixelX, pixelY, pixelX + pixelW, pixelY + pixelH,
			null);

		// Compare the two
		byte[] bimdata = ((DataBufferByte) currentReducedImage.getRaster().getDataBuffer()).getData();
		byte[] ximdata = ((DataBufferByte) currentTile.getRaster().getDataBuffer()).getData();
		if (!Arrays.equals(bimdata, ximdata)) {
			backingStore.getGraphics().drawImage(currentImage, pixelX, pixelY, null);
			updateFromBs();

			// Saves another grab
			queuedImage.set(new QueuedImage(currentImage, area));
			try {
				fireDamageEvent("DamageDetection", area, false, -1);
			} finally {
				queuedImage.set(null);
			}
		}
	}

	@Override
	public BufferedImage grabArea(Rectangle area) {
		QueuedImage qim = queuedImage.get();
		if (qim != null && area.equals(qim.area)) {
			queuedImage.set(null);
			return qim.img;
		}

		// When the scanned rectangle intersects something that has recently
		// been damaged, skip it
		synchronized (damage) {
			for (Iterator<Rectangle> dit = damage.iterator(); dit.hasNext();) {
				if (area.intersects(dit.next())) {
					dit.remove();
				}
			}
		}

		BufferedImage bim = underlyingDriver.grabArea(area);
		if (backingStore == null) {
			backingStore = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		}
		backingStore.getGraphics().drawImage(bim, area.x, area.y, null);
		updateFromBs();
		return bim;
	}

	void updateFromBs() {
		// if (backingStore != null) {
		// SwingUtilities.invokeLater(new Runnable() {
		// public void run() {
		// label.setIcon(new ImageIcon(backingStore));
		// }
		// });
		// }
	}
}
