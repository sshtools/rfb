package com.sshtools.rfbcommon;

import java.io.IOException;

public class RFBVersion implements Comparable<RFBVersion> {

	public final static RFBVersion VERSION_3_7 = new RFBVersion(3, 7);
	public final static RFBVersion VERSION_3_8 = new RFBVersion(3, 8);

	private int major;
	private int minor;

	public RFBVersion() {
	}

	public RFBVersion(int major, int minor) {
		this.major = major;
		this.minor = minor;
	}

	public RFBVersion(String versionString) {
		parseVersion(formatVersionString(versionString));
	}

	/**
	 * Determine the servers version.
	 * 
	 * @throws IOException
	 */
	public void determineVersion(byte[] buf) throws IOException {
		if (buf.length != 12) {
			throw new IOException("Expects 12 bytes for version");
		}
		String versionText = new String(buf, "ASCII").trim();
		parseVersion(versionText);
	}

	private void parseVersion(String versionText) {
		int[] ident = parseVersionText(versionText);
		major = ident[0];
		minor = ident[1];
	}

	static String formatVersionString(String versionString) {
		String[] v = versionString.split("\\.");
		return String.format("RFB %03d.%03d\n", Integer.parseInt(v[0]), Integer.parseInt(v[1]));
	}

	private static int[] parseVersionText(String versionText) {
		if (versionText.startsWith("RFB ")) {
			try {
				int[] v = new int[2];
				v[0] = Integer.parseInt(versionText.substring(4, 7));
				v[1] = Integer.parseInt(versionText.substring(8, 11));
				return v;
			} catch (Exception ex) {
				throw new IllegalArgumentException("Failed to read protocol version from identification: " + versionText);
			}
		}
		throw new IllegalArgumentException(versionText + " is not a version");
	}

	/**
	 * Send our version
	 * 
	 * @throws IOException
	 */
	public byte[] formatVersion() throws IOException {
		String sendVersion = String.format("RFB %03d.%03d\n", major, minor);
		return sendVersion.getBytes("ASCII");
	}

	public static int getVersion(int major, int minor) {
		return (major * 100) + minor;
	}

	public int getVersion() {
		return getVersion(major, minor);
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public void set(int major, int minor) {
		this.major = major;
		this.minor = minor;
	}

	public int compareTo(RFBVersion o) {
		return new Integer(getVersion()).compareTo(new Integer(o.getVersion()));
	}

	public String toString() {
		return major + "." + minor;
	}
}
