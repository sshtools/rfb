/**
 * RFB Common - Remote Frame Buffer common code used both in client and server.
 * Copyright © 2006 SSHTOOLS Limited (support@sshtools.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.sshtools.rfbcommon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScreenData {
	private ScreenDimension dimension;
	private List<ScreenDetail> details = new ArrayList<>();

	public ScreenData(ScreenData other) {
		this();
		set(other);
	}
	
	public ScreenData() {
		this(new ScreenDimension(0, 0));
	}

	public ScreenData(ScreenDimension dimension) {
		super();
		this.dimension = dimension;
	}

	public ScreenDimension getDimension() {
		return dimension;
	}

	public List<ScreenDetail> getDetails() {
		return details;
	}
	
	public synchronized List<ScreenDetail> getAllDetails() {
		if(details.isEmpty())
			return Arrays.asList(new ScreenDetail(0, 0, 0, dimension, 0));
		else
			return Collections.unmodifiableList(details);
	}

	public synchronized void reset() {
		this.dimension.reset();
		details.clear();
	}

	public int getWidth() {
		return dimension.getWidth();
	}

	public int getHeight() {
		return dimension.getHeight();
	}

	public boolean isEmpty() {
		return dimension.isEmpty();
	}

	public synchronized void set(ScreenData screenData) {
		this.dimension.set(screenData.getDimension());
		details.clear();
		for(ScreenDetail d : screenData.getDetails()) {
			details.add(new ScreenDetail(d));
		}
	}

}