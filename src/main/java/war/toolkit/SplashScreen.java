package war.toolkit;

import javax.swing.*;
import java.awt.*;

public class SplashScreen {

    static {
        System.setProperty("java.awt.headless", "false");
        showSplashAndBlock();
    }

    public static void load() {}

    private static synchronized void showSplashAndBlock() {

        Toolkit.getDefaultToolkit();

        JFrame frame = new JFrame();
        frame.setUndecorated(true);
        frame.setSize(500, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.BLACK);

        JLabel fallback = new JLabel("JNT3", SwingConstants.CENTER);
        fallback.setForeground(Color.WHITE);
        fallback.setFont(new Font("Arial", Font.BOLD, 24));
        mainPanel.add(fallback, BorderLayout.CENTER);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(0, 4));
        progressBar.setBackground(new Color(30, 30, 40));
        progressBar.setForeground(new Color(100, 180, 255));
        progressBar.setBorderPainted(false);
        progressBar.setStringPainted(false);

        mainPanel.add(progressBar, BorderLayout.SOUTH);
        frame.setContentPane(mainPanel);
        frame.setVisible(true);

        // Drive animation synchronously (block here)
        for (int i = 0; i <= 100; i++) {
            progressBar.setValue(i);
            progressBar.paintImmediately(progressBar.getBounds()); // force repaint
            try {
                Thread.sleep(15);
            } catch (InterruptedException ignored) {}
        }

        frame.dispose();
    }
}
