/*
 * JMCL
 * Copyright (C) 2026  OCS
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.Open_code_Studio.jmcl.util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

/// A reusable, standalone Swing-based download progress dialog.
/// Supports: progress bar, per-file labels, speed/ETA display,
/// cancel button, and change-source button.
///
/// @author Glavo, OCS contributors
public final class DownloadProgressWindow extends JDialog {

    private final JLabel headerLabel;
    private final JLabel currentLabel;
    private final JLabel speedLabel;
    private final JProgressBar progressBar;
    private final JButton btnChangeSource;
    private final JButton btnCancel;
    private final JTextArea detailArea;
    private final JScrollPane detailScrollPane;

    private long lastBytes;
    private long lastTime;
    private final long startTime;

    /// Creates a download progress dialog.
    ///
    /// @param title      dialog title bar text.
    /// @param header     descriptive text shown at the top (supports HTML).
    /// @param total      total number of units (files).
    /// @param cancelled  shared flag — the dialog will {@link #dispose()} itself when cancel is pressed,
    ///                   and the caller polls this flag to abort the download loop.
    public DownloadProgressWindow(String title, String header, int total, AtomicBoolean cancelled) {
        setTitle(title);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setModalityType(ModalityType.APPLICATION_MODAL);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        // --- Header ---
        headerLabel = new JLabel(header);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.PLAIN, 13f));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // --- Center: progress + details ---
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        currentLabel = new JLabel(i18n("download.javafx.prepare"));
        currentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(currentLabel);
        centerPanel.add(Box.createVerticalStrut(4));

        progressBar = new JProgressBar(0, total);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setPreferredSize(new Dimension(450, 22));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        centerPanel.add(progressBar);
        centerPanel.add(Box.createVerticalStrut(2));

        speedLabel = new JLabel(" ");
        speedLabel.setFont(speedLabel.getFont().deriveFont(Font.PLAIN, 11f));
        speedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(speedLabel);
        centerPanel.add(Box.createVerticalStrut(6));

        // --- Detail area (collapsible) ---
        detailArea = new JTextArea(4, 40);
        detailArea.setEditable(false);
        detailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        detailArea.setBackground(UIManager.getColor("Panel.background"));

        detailScrollPane = new JScrollPane(detailArea);
        detailScrollPane.setBorder(new TitledBorder(i18n("download.details")));
        detailScrollPane.setVisible(false);
        // Fixed size to prevent layout jumping
        detailScrollPane.setPreferredSize(new Dimension(450, 80));
        detailScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        centerPanel.add(detailScrollPane);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // --- Buttons ---
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnChangeSource = new JButton(i18n("button.change_source"));
        btnCancel = new JButton(i18n("button.cancel"));
        buttonBar.add(btnChangeSource);
        buttonBar.add(btnCancel);
        mainPanel.add(buttonBar, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);

        btnCancel.addActionListener(e -> {
            cancelled.set(true);
            dispose();
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelled.set(true);
                dispose();
            }
        });

        startTime = System.currentTimeMillis();
        lastTime = startTime;
    }

    /// Set the description of the current download unit.
    public void setCurrent(String component) {
        SwingUtilities.invokeLater(() -> currentLabel.setText(component));
    }

    /// Set overall progress value (0..max).
    public void setProgress(int n) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(n));
    }

    /// Advance progress by 1.
    public void incrementProgress() {
        SwingUtilities.invokeLater(() -> progressBar.setValue(progressBar.getValue() + 1));
    }

    /// Advance progress by 1 and update speed display.
    public void advanceProgress(long bytesDownloaded) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(progressBar.getValue() + 1);
            updateSpeed(bytesDownloaded);
        });
    }

    /// Set the action for the "Change Source" button.
    /// The consumer receives "ok" when user confirms source change.
    public void setOnChangeSource(Runnable action) {
        btnChangeSource.addActionListener(e -> action.run());
    }

    /// Append a line to the detail text area.
    public void appendDetail(String text) {
        SwingUtilities.invokeLater(() -> {
            detailArea.append(text + "\n");
            // auto-scroll to bottom
            detailArea.setCaretPosition(detailArea.getDocument().getLength());
        });
    }

    /// Show or hide the detail panel.
    public void setDetailsVisible(boolean visible) {
        SwingUtilities.invokeLater(() -> detailScrollPane.setVisible(visible));
    }

    private void updateSpeed(long totalBytes) {
        long now = System.currentTimeMillis();
        long elapsed = now - lastTime;
        if (elapsed < 800) return; // update at most ~1.2 Hz

        long bytes = totalBytes - lastBytes;
        lastBytes = totalBytes;
        lastTime = now;

        if (elapsed == 0) return;

        double bytesPerSec = bytes * 1000.0 / elapsed;
        String speedStr;
        if (bytesPerSec >= 1024 * 1024) {
            speedStr = i18n("download.speed.megabyte_per_second", bytesPerSec / (1024 * 1024));
        } else if (bytesPerSec >= 1024) {
            speedStr = i18n("download.speed.kibibyte_per_second", bytesPerSec / 1024);
        } else {
            speedStr = i18n("download.speed.byte_per_second", (int) bytesPerSec);
        }

        long etaSec = 0;
        int current = progressBar.getValue();
        int max = progressBar.getMaximum();
        if (current > 0 && current < max && bytesPerSec > 0) {
            int remaining = max - current;
            // rough ETA: remaining files * avg time per file
            long totalElapsed = now - startTime;
            double avgMsPerFile = totalElapsed / (double) current;
            etaSec = (long) (remaining * avgMsPerFile / 1000);
        }

        String etaStr;
        if (etaSec <= 0) {
            etaStr = "--:--";
        } else if (etaSec < 60) {
            etaStr = etaSec + "s";
        } else if (etaSec < 3600) {
            etaStr = (etaSec / 60) + "m " + (etaSec % 60) + "s";
        } else {
            etaStr = (etaSec / 3600) + "h " + ((etaSec % 3600) / 60) + "m";
        }

        speedLabel.setText(speedStr + "  |  " + i18n("download.eta") + ": " + etaStr);
    }
}
