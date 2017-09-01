package com.sshtools.rfbserver.encodings;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.rfbcommon.ImageUtil;
import com.sshtools.rfbcommon.PaletteAnalyser;
import com.sshtools.rfbcommon.PixelFormat;
import com.sshtools.rfbcommon.ProtocolWriter;
import com.sshtools.rfbcommon.RFBConstants;
import com.sshtools.rfbserver.RFBClient;
import com.sshtools.rfbserver.UpdateRectangle;

public abstract class AbstractRREEncoding extends AbstractRawEncoding<BufferedImage> {
	final static Logger LOG = LoggerFactory.getLogger(AbstractRREEncoding.class);
	private static final int SUBRECT_THRESHOLD = 30000;

	public AbstractRREEncoding() {
	}

	public void encode(UpdateRectangle<BufferedImage> update, ProtocolWriter dout, PixelFormat pixelFormat, RFBClient client)
			throws IOException {
		BufferedImage img = update.getData();
		DataBufferInt dint = (DataBufferInt) img.getData().getDataBuffer();
		int[] pixels = dint.getData();
		int background = new PaletteAnalyser().analyse(pixels, pixels.length).getBackground();
		int w = update.getArea().width;
		int h = update.getArea().height;
		List<SubRect> subrects = extractSubRects(pixels, background, w, h);
		if (subrects.size() > SUBRECT_THRESHOLD) {
			if(LOG.isDebugEnabled())
				LOG.debug("Number of subrects (" + (subrects.size()) + ") exceeds threshold of " + SUBRECT_THRESHOLD
					+ ", reverting to raw");
			byte[] array = prepareEncode(update, pixelFormat);
			dout.writeUInt32(RFBConstants.ENC_RAW);
			dout.write(array);
			return;
		}
		dout.writeUInt32(getType().getCode());
		dout.writeUInt32(subrects.size());
		writePixel(dout, pixelFormat, background);
		for (SubRect s : subrects) {
			writeSubrect(dout, pixelFormat, s);
		}
	}

	protected List<SubRect> extractSubRects(int[] pixels, int bgpixel, int w, int h) {
		List<SubRect> subrects = new ArrayList<AbstractRawEncoding.SubRect>();
		SubRect subrect;
		int currentPixel;
		int currentX, currentY;
		int runningX, runningY;
		int firstX = 0, firstY, firstW, firstH;
		int secondX = 0, secondY, secondW, secondH;
		boolean firstYflag;
		int segment;
		int line;
		for (currentY = 0; currentY < h; currentY++) {
			line = currentY * w;
			for (currentX = 0; currentX < w; currentX++) {
				if (pixels[line + currentX] != bgpixel) {
					currentPixel = pixels[line + currentX];
					firstY = currentY - 1;
					firstYflag = true;
					for (runningY = currentY; runningY < h; runningY++) {
						segment = runningY * w;
						if (pixels[segment + currentX] != currentPixel)
							break;
						runningX = currentX;
						while ((runningX < w) && (pixels[segment + runningX] == currentPixel))
							runningX++;
						runningX--;
						if (runningY == currentY)
							secondX = firstX = runningX;
						if (runningX < secondX)
							secondX = runningX;
						if (firstYflag && (runningX >= firstX))
							firstY++;
						else
							firstYflag = false;
					}
					secondY = runningY - 1;
					firstW = firstX - currentX + 1;
					firstH = firstY - currentY + 1;
					secondW = secondX - currentX + 1;
					secondH = secondY - currentY + 1;
					subrect = new SubRect();
					subrects.add(subrect);
					subrect.pixel = currentPixel;
					subrect.x = currentX;
					subrect.y = currentY;
					if ((firstW * firstH) > (secondW * secondH)) {
						subrect.w = firstW;
						subrect.h = firstH;
					} else {
						subrect.w = secondW;
						subrect.h = secondH;
					}
					for (runningY = subrect.y; runningY < (subrect.y + subrect.h); runningY++)
						for (runningX = subrect.x; runningX < (subrect.x + subrect.w); runningX++)
							pixels[runningY * w + runningX] = bgpixel;
				}
			}
		}
		return subrects;
	}

	protected abstract void writeSubrect(DataOutputStream dout, PixelFormat pixelFormat, SubRect s) throws IOException;

	protected int getBackground(BufferedImage img) {
		// TODO experiment with smaller scales
		DataBufferInt dint = (DataBufferInt) img.getData().getDataBuffer();
		int[] pixels = dint.getData();
		return ImageUtil.count(pixels).background;
	}
}
