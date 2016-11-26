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
