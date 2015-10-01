package com.sshtools.vnc;

import nanoxml.XMLElement;

import com.sshtools.profile.AbstractSchemeOptions;
import com.sshtools.profile.ProfileException;
import com.sshtools.profile.SchemeOptions;

public class VNCSchemeOptions extends AbstractSchemeOptions {
    int transportProvider = USE_STANDARD_SOCKET;
    String proxyHostname;
    String proxyUsername;
    String proxyPassword;
    int proxyPort;

    public VNCSchemeOptions() {
    	super("vnc");
    }

    public void init(XMLElement element) throws ProfileException {
        String n;
        if (element != null) {
            try {
                n = (String) element.getAttribute("transportProvider");
                transportProvider = n != null && !n.equals("") ? Integer.parseInt(n) : USE_STANDARD_SOCKET;
            } catch (NumberFormatException ex) {
            }
        }
        n = (String) element.getAttribute("proxyHostname");
        proxyHostname = n != null && !n.equals("") ? n : null;
        int i = element.getIntAttribute("proxyPort");
        proxyPort = i > 0 ? i : 0;
        n = (String) element.getAttribute("proxyUsername");
        proxyUsername = n != null && !n.equals("") ? n : null;
        n = (String) element.getAttribute("proxyPassword");
        proxyPassword = n != null && !n.equals("") ? n : null;
    }

    public XMLElement getElement() {
        XMLElement el = new XMLElement();
        el.setName("schemeOptions");
        el.setAttribute("proxyHostname", proxyHostname == null ? "" : proxyHostname);
        el.setIntAttribute("proxyPort", proxyPort);
        el.setAttribute("proxyUsername", proxyUsername == null ? "" : proxyUsername);
        el.setAttribute("proxyPassword", proxyPassword == null ? "" : proxyPassword);
        el.setIntAttribute("transportProvider", transportProvider);
        return el;
    }

    public boolean isAppropriateForScheme(String schemeName) {
        return schemeName.equalsIgnoreCase("vnc");
    }

    public int getTransportProvider() {
        return transportProvider;
    }

    /**
     * @return
     */
    public String getProxyHost() {
        return proxyHostname;
    }

    /**
     * @return
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * @return
     */
    public String getProxyUsername() {
        return proxyUsername;
    }

    /**
     * @return
     */
    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setTransportProvider(int transportProvider) {
        this.transportProvider = transportProvider;
    }

    /**
     * @param proxyHostname
     */
    public void setProxyHost(String proxyHostname) {
        this.proxyHostname = proxyHostname;
    }

    /**
     * @param proxyPort
     */
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * @param proxyUsername
     */
    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    /**
     * @param proxyPassword
     */
    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }
}