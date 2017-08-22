package com.sshtools.rfbcommon;

import java.util.ArrayList;
import java.util.List;

public class ScreenData {
	private ScreenDimension dimension;
	private List<ScreenDetail> details = new ArrayList<>();

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

}