/**
 * RFB Server (Windows Driver) - A JNA based driver for Windows,
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
package com.sshtools.rfbserver.windows.jni;

//
// FrameTest.java
//
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class FrameTest extends JFrame {
	private JPanel mainPanel;
	private JTextArea mainTextArea;
	private HookTest hook;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new FrameTest().setVisible(true);
			}
		});
	}

	FrameTest() {
		super("FrameTest");
		setSize(200, 200);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainTextArea = new JTextArea();
		mainPanel.add(mainTextArea, BorderLayout.CENTER);
		getContentPane().add(mainPanel);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				hook.unRegisterHook();
			}
		});
		new Thread() {
			public void run() {
				hook = new HookTest();
				hook.registerHook();
			}
		}.start();
	}
}
