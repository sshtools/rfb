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