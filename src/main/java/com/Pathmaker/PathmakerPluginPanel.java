package com.Pathmaker;

import net.runelite.api.Client;
import net.runelite.client.ui.PluginPanel;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class PathmakerPluginPanel extends PluginPanel
{

    Client client;
    PathmakerPlugin plugin;

    PathmakerPluginPanel(Client client, PathmakerPlugin plugin)
    {
        this.client = client;
	    this.plugin = plugin;

        // Define standard panel layout
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBorder(new EmptyBorder(1, 3, 10, 7));

        // Create label and add to title panel
        JLabel title = new JLabel();
        title.setText("Pathmaker");
        title.setForeground(Color.WHITE);
        titlePanel.add(title, BorderLayout.CENTER);

        // Create north panel and add titlePanel
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBorder(new EmptyBorder(1, 0, 10, 0));
        northPanel.add(titlePanel, BorderLayout.NORTH);

        // Add north panel to client panel
        add(northPanel, BorderLayout.NORTH);

        // SOUTH PANEL
        // Create signature panel
        JPanel signaturePanel = new JPanel();
        signaturePanel.setBorder(new EmptyBorder(1, 3, 10, 7));

        // Create signature and add to signature panel
        JLabel signature = new JLabel();
        signature.setText("By Fraph");
        signature.setForeground(Color.WHITE);
        signaturePanel.add(signature, BorderLayout.CENTER);

        // Create south panel and add signaturePanel
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(new EmptyBorder(1, 0, 10, 0));
        southPanel.add(signaturePanel, BorderLayout.SOUTH);

        // Add south panel to client panel
        add(southPanel, BorderLayout.SOUTH);
    }
}
