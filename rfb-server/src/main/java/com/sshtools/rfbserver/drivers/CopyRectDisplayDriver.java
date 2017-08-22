package com.sshtools.rfbserver.drivers;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbserver.DisplayDriver;
import com.sshtools.rfbserver.UpdateRectangle;

/**
 * A driver that intercepts the window move event, and uses copyrect to paint
 * the window at the new position. Updates are also request for the area the
 * window used to occupy (excluding any portion the new bounds may overlap).
 * <p>
 * This driver should ideally be placed above the
 * {@link WindowOutlineDisplayDriver} for best effect.
 */
public class CopyRectDisplayDriver extends FilteredDisplayDriver {

	final static Logger LOG = LoggerFactory.getLogger(CopyRectDisplayDriver.class);

	public CopyRectDisplayDriver(DisplayDriver underlyingDriver) {
		super(underlyingDriver, true);
	}

	@Override
	protected void filteredWindowMoved(String name, Rectangle rectangle, Rectangle oldRectangle) {
		if (oldRectangle != null && rectangle != null) {
			UpdateRectangle<Point> update = new UpdateRectangle<Point>(this, rectangle, RFBConstants.ENC_COPYRECT);
			update.setImportant(true);
			update.setData(new Point(oldRectangle.x, oldRectangle.y));
			LOG.info("Copyrect updated " + update);
			fireUpdate(update);
			if (rectangle.intersects(oldRectangle)) {
				for (Rectangle reducedRectangle : subtractRectangle(rectangle, oldRectangle)) {
					LOG.info("Moved window damage at " + reducedRectangle);
					fireDamageEvent(name, reducedRectangle, true, -1);
				}
			} else {
				LOG.info("Moved window damage at " + oldRectangle);
				fireDamageEvent(name, oldRectangle, true, -1);
			}
		} else {
			super.filteredWindowMoved(name, rectangle, oldRectangle);
		}
	}

	public static Rectangle subtract(Rectangle rect1, Rectangle rect2, int edge) {
		Rectangle remainder = new Rectangle();
		switch (edge) {
		case SwingConstants.SOUTH:
			remainder.x = rect2.x;
			remainder.y = (int) rect1.getMaxY();
			remainder.width = rect2.width;
			remainder.height = (int) rect2.getMaxY() - (int) rect1.getMaxY();
			return remainder;
		case SwingConstants.NORTH:
			remainder.x = rect2.x;
			remainder.y = rect2.y;
			remainder.width = rect2.width;
			remainder.height = rect1.y - rect2.y;
			return remainder;
		case SwingConstants.EAST:
			remainder.y = Math.max(rect2.y, rect1.y);
			remainder.x = (int) rect1.getMaxX();
			remainder.height = rect1.intersection(rect2).height;
			remainder.width = (int) rect2.getMaxX() - (int) rect1.getMaxX();
			return remainder;
		case SwingConstants.WEST:
			remainder.y = Math.max(rect2.y, rect1.y);
			remainder.x = rect2.x;
			remainder.height = rect1.intersection(rect2).height;
			remainder.width = rect1.x - rect2.x;
			return remainder;
		default:
			throw new IllegalArgumentException();
		}
	}

	public static Collection<Rectangle> subtractRectangle(Rectangle rect1, Rectangle rect2) {
		List<Rectangle> l = new ArrayList<Rectangle>();
		if (!rect1.isEmpty()) {
			Rectangle intersected = rect1.intersection(rect2);
			if (!intersected.isEmpty()) {
				// for(int edge : Arrays.asList(SwingConstants.SOUTH,
				// SwingConstants.NORTH, SwingConstants.EAST,
				// SwingConstants.WEST)) {
				for (int edge : Arrays.asList(SwingConstants.SOUTH, SwingConstants.NORTH, SwingConstants.EAST, SwingConstants.WEST)) {
					Rectangle subtractedArea = subtract(rect1, rect2, edge);
					if (!subtractedArea.isEmpty()) {
						l.add(subtractedArea);
					}
				}
			}
		}
		return l;
	}

	public static Collection<Rectangle> subtractRectangle2(Rectangle rect1, Rectangle rect2) {
		List<Rectangle> l = new ArrayList<Rectangle>();
		if (!rect1.isEmpty()) {
			Rectangle intersected = rect1.intersection(rect2);
			if (!intersected.isEmpty()) {
				Rectangle remainder = new Rectangle();
				Rectangle subtractedArea = rectBetweenXX(rect1, intersected, remainder, SwingConstants.SOUTH);
				if (!subtractedArea.isEmpty()) {
					l.add(subtractedArea);
				}
				// subtractedArea = rectBetween(remainder, intersected,
				// remainder, SwingConstants.NORTH);
				// if (!subtractedArea.isEmpty()) {
				// l.add(subtractedArea);
				// }
				// subtractedArea = rectBetween(remainder, intersected,
				// remainder, SwingConstants.EAST);
				// if (!subtractedArea.isEmpty()) {
				// l.add(subtractedArea);
				// }
				// subtractedArea = rectBetween(remainder, intersected,
				// remainder, SwingConstants.WEST);
				// if (!subtractedArea.isEmpty()) {
				// l.add(subtractedArea);
				// }
			}
		}
		return l;
	}

	public static Rectangle rectBetweenXX(Rectangle rect1, Rectangle rect2, Rectangle remainder, int edge) {
		Rectangle intersected = rect1.intersection(rect2);
		if (intersected.isEmpty()) {
			return intersected;
		}
		float chopAmount = 0;
		switch (edge) {
		case SwingConstants.SOUTH:
			chopAmount = rect1.height - (intersected.y - rect1.y);
			if (chopAmount > rect1.height) {
				chopAmount = rect1.height;
			}
			break;
		case SwingConstants.NORTH:
			chopAmount = rect1.height - ((int) rect1.getMaxY() - (int) intersected.getMaxY());
			if (chopAmount > rect1.height) {
				chopAmount = rect1.height;
			}
			break;
		case SwingConstants.EAST:
			chopAmount = rect1.width - (intersected.x - rect1.x);
			if (chopAmount > rect1.width) {
				chopAmount = rect1.width;
			}
			break;
		case SwingConstants.WEST:
			chopAmount = rect1.width - ((int) rect1.getMaxX() - (int) intersected.getMaxX());
			if (chopAmount > rect1.width) {
				chopAmount = rect1.width;
			}
			break;
		default:
			throw new IllegalArgumentException();
		}
		return divideRectangle(rect1, remainder, chopAmount, edge);
	}

	public static Rectangle divideRectangle(Rectangle source, Rectangle slice, float chopAmount, int edge) {
		System.out.println("Source = " + source + " Slice = " + slice + " Chop = " + chopAmount + " Edge = " + edge);
		Rectangle remainder = new Rectangle(source);
		switch (edge) {
		case SwingConstants.SOUTH:
			remainder.y = remainder.y + remainder.height;
			remainder.height -= chopAmount;
			break;
		case SwingConstants.NORTH:
			remainder.height = (int) chopAmount;
			break;
		case SwingConstants.EAST:
			remainder.x = remainder.x + remainder.width;
			remainder.width -= (int) chopAmount;
			break;
		case SwingConstants.WEST:
			remainder.width = (int) chopAmount;
			break;
		default:
			throw new IllegalArgumentException();
		}
		return remainder;
	}

	protected static Rectangle reduce(Rectangle rectangle, Rectangle oldRectangle) {
		LOG.info("Copyrect intersects " + rectangle + " / " + oldRectangle);

		Rectangle reducedRectangle = new Rectangle(oldRectangle);

		// Rectangle reducedRectangle = oldRectangle.intersection(rectangle) ;

		/*
		 * The old and new position overlap, so remove any overlapping region
		 * and fire damage events for what remains
		 */

		if (reducedRectangle.y > rectangle.y && reducedRectangle.y < rectangle.y + rectangle.height) {
			// Old position is below new position, and overlaps
			reducedRectangle.height -= reducedRectangle.y - rectangle.y;
			reducedRectangle.y -= rectangle.y + rectangle.height;
		} else if (reducedRectangle.y < rectangle.y && reducedRectangle.y + reducedRectangle.height > rectangle.y) {
			// Old position is to left of new position, and overlaps
			reducedRectangle.height = rectangle.y - reducedRectangle.y;
		} else if (reducedRectangle.x > rectangle.x && reducedRectangle.x < rectangle.x + rectangle.width) {
			// Old position is to right of new position, and overlaps
			reducedRectangle.width -= reducedRectangle.x - rectangle.x;
			reducedRectangle.x = rectangle.x + rectangle.width;
		} else if (reducedRectangle.x < rectangle.x && reducedRectangle.x + reducedRectangle.width > rectangle.x) {
			// Old position is to left of new position, and overlaps
			reducedRectangle.width = rectangle.x - reducedRectangle.x;
		}

		LOG.info("Reduced too " + reducedRectangle);

		return reducedRectangle;
	}

	static class TR {
		Rectangle r1, r2;

		TR(Rectangle r1, Rectangle r2) {
			this.r1 = r1;
			this.r2 = r2;
		}
	}

	static Image createRectImage(TR tr) {
		BufferedImage bim = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) bim.getGraphics();
		g.setColor(Color.RED);

		Stroke s = new BasicStroke(1.0f, // Width
			BasicStroke.CAP_SQUARE, // End cap
			BasicStroke.JOIN_MITER, // Join style
			10.0f, // Miter limit
			new float[] { 10f, 10f }, // Dash pattern
			0.0f); // Dash phase
		g.setStroke(s);
		g.drawRect(tr.r1.x, tr.r1.y, tr.r1.width, tr.r1.height);

		g.setColor(Color.GREEN);
		s = new BasicStroke(1.0f, // Width
			BasicStroke.CAP_SQUARE, // End cap
			BasicStroke.JOIN_MITER, // Join style
			10.0f, // Miter limit
			new float[] { 5f, 5f }, // Dash pattern
			0.0f); // Dash phase
		g.setStroke(s);
		g.drawRect(tr.r2.x, tr.r2.y, tr.r2.width, tr.r2.height);

		g.setColor(Color.BLUE);
		s = new BasicStroke(1.0f, // Width
			BasicStroke.CAP_SQUARE, // End cap
			BasicStroke.JOIN_MITER, // Join style
			10.0f, // Miter limit
			new float[] { 2f, 2f }, // Dash pattern
			0.0f); // Dash phase
		g.setStroke(s);
		Rectangle x = reduce(tr.r1, tr.r2);
		g.drawRect(x.x, x.y, x.width, x.height);
		return bim;
	}

	static Image createRectImage2(TR tr, boolean withSubtractions) {
		BufferedImage bim = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) bim.getGraphics();
		g.setColor(Color.RED);

		Stroke s = new BasicStroke(1.0f, // Width
			BasicStroke.CAP_SQUARE, // End cap
			BasicStroke.JOIN_MITER, // Join style
			10.0f, // Miter limit
			new float[] { 10f, 10f }, // Dash pattern
			0.0f); // Dash phase
		g.setStroke(s);
		g.drawRect(tr.r1.x, tr.r1.y, tr.r1.width, tr.r1.height);

		g.setColor(Color.GREEN);
		s = new BasicStroke(1.0f, // Width
			BasicStroke.CAP_SQUARE, // End cap
			BasicStroke.JOIN_MITER, // Join style
			10.0f, // Miter limit
			new float[] { 5f, 5f }, // Dash pattern
			0.0f); // Dash phase
		g.setStroke(s);
		g.drawRect(tr.r2.x, tr.r2.y, tr.r2.width, tr.r2.height);

		g.setColor(Color.YELLOW);
		s = new BasicStroke(1.0f, // Width
			BasicStroke.CAP_SQUARE, // End cap
			BasicStroke.JOIN_MITER, // Join style
			10.0f, // Miter limit
			new float[] { 15f, 15f }, // Dash pattern
			0.0f); // Dash phase
		g.setStroke(s);
		Rectangle i = tr.r1.intersection(tr.r2);
		g.drawRect(i.x, i.y, i.width, i.height);

		if (withSubtractions) {
			s = new BasicStroke(1.0f, // Width
				BasicStroke.CAP_SQUARE, // End cap
				BasicStroke.JOIN_MITER, // Join style
				10.0f, // Miter limit
				new float[] { 2f, 2f }, // Dash pattern
				0.0f); // Dash phase
			g.setStroke(s);

			Iterator<Color> itc = Arrays.asList(Color.BLUE, Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.PINK).iterator();

			for (Rectangle x : subtractRectangle(tr.r1, tr.r2)) {
				g.setColor(itc.next());
				g.drawRect(x.x, x.y, x.width, x.height);
			}
		}
		return bim;
	}

	public static void main(String[] args) {
		JFrame f = new JFrame("Rect Test");
		JTabbedPane tb = new JTabbedPane();

		for (TR tr : new TR[] { new TR(new Rectangle(383, 89, 224, 271), new Rectangle(434, 90, 224, 271)),
			new TR(new Rectangle(383, 89, 224, 271), new Rectangle(434, 90, 224, 271)),
			new TR(new Rectangle(383, 89, 224, 271), new Rectangle(389, 10, 224, 271)),
			new TR(new Rectangle(383, 89, 224, 271), new Rectangle(389, 150, 224, 271)),
			new TR(new Rectangle(383, 89, 224, 271), new Rectangle(379, 150, 224, 271)) }) {
			// tb.add(new JLabel(new ImageIcon(createRectImage(tr))));
			JPanel x = new JPanel();
			x.setLayout(new BorderLayout());
			tb.add(x);
			x.add(new JLabel(new ImageIcon(createRectImage2(tr, false))), BorderLayout.WEST);
			x.add(new JLabel(new ImageIcon(createRectImage2(tr, true))), BorderLayout.EAST);
		}

		f.getContentPane().add(tb);
		f.pack();
		f.setVisible(true);

		// reduce(new Rectangle(383, 89, 224, 271), new Rectangle(434, 90, 224,
		// 271));
		// reduce(new Rectangle(383, 89, 224, 271), new Rectangle(234, 90, 224,
		// 271));
		// reduce(new Rectangle(383, 89, 224, 271), new Rectangle(389, 10, 224,
		// 271));
		// reduce(new Rectangle(383, 89, 224, 271), new Rectangle(389, 150, 224,
		// 271));
	}

}
