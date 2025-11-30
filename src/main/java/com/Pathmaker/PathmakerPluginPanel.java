package com.Pathmaker;

import net.runelite.api.Client;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.util.ImageUtil;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;


public class PathmakerPluginPanel extends PluginPanel
{
    private static final ImageIcon IMPORT_ICON;
    private static final ImageIcon EXPORT_ICON;

    private final PluginErrorPanel noPathPanel = new PluginErrorPanel();
    private final JPanel pathView = new JPanel();

    Client client;
    PathmakerPlugin plugin;

    FlatTextField activePath;
    final int MAX_TEXT_LENGTH = 7; // Based on ÆÆÆÆÆÆÆÆ

    static
    {
        IMPORT_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "import.png"));
        EXPORT_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "export.png"));
    }

    PathmakerPluginPanel(Client client, PathmakerPlugin plugin)
    {
        this.client = client;
	    this.plugin = plugin;

        // Define standard client panel layout
        setLayout(new BorderLayout());
        //setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create title panel
        JPanel titlePanel = new JPanel();
        //titlePanel.setBorder(new EmptyBorder(1, 0, 10, 0));

        // Create label and add to title panel
        JLabel title = new JLabel();
        title.setText("Pathmaker");
        title.setPreferredSize(new Dimension(80,20)); //getGraphics().getFontMetrics().stringWidth(title.getText()) + 10
        title.setForeground(Color.WHITE);
        title.setToolTipText("by Fraph");
        titlePanel.add(title, BorderLayout.CENTER);

        /* EXPORT / IMPORT -> To be implemented
        JButton exportButton = new JButton();
        exportButton.setIcon(EXPORT_ICON);
        exportButton.setToolTipText("Export active path");
        exportButton.setPreferredSize(new Dimension(18, 18));
        exportButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent mouseEvent)
            {
            }
        });
        JButton importButton = new JButton();
        importButton.setIcon(IMPORT_ICON);
        importButton.setToolTipText("Import path");
        importButton.setPreferredSize(new Dimension(18, 18));
        importButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent mouseEvent)
            {
            }
        });

        JPanel rightActionTitlePanel = new JPanel();
        //rightActionTitlePanel.setPreferredSize(new Dimension(40, 20));
        rightActionTitlePanel.add(importButton, BorderLayout.WEST);
        rightActionTitlePanel.add(exportButton, BorderLayout.EAST);
        titlePanel.add(rightActionTitlePanel, BorderLayout.EAST);

         */

        // Create body panel and add titlePanel
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBorder(new EmptyBorder(1, 0, 10, 0));
        northPanel.add(titlePanel, BorderLayout.NORTH);

        // Add link to config button
        // But how? Watchdogs implementation
        // https://github.com/adamk33n3r/runelite-watchdog/blob/master/src/main/java/com/adamk33n3r/runelite/watchdog/NotificationOverlay.java
//        JButton configButton = new JButton();
//        configButton.setIcon(new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "cross.png")));
//        configButton.setToolTipText("Config");
//        configButton.addChangeListener(ce ->
//        {
//            plugin.getEventBus().post(
//                    new OverlayMenuClicked(
//                    new OverlayMenuEntry(
//                    RUNELITE_OVERLAY_CONFIG,
//                            null,
//                            null),
//                            notificationOverlay)); <---- What in this is relevant? (link above)
//        });
//        northPanel.add(configButton);

        // Add Active Path text field
        JLabel activePathLabel = new JLabel("Active Path: ");
        activePathLabel.setPreferredSize(new Dimension(75, 20));
        activePathLabel.setForeground(Color.WHITE);
        northPanel.add(activePathLabel, BorderLayout.WEST);

        activePath = new FlatTextField();
        activePath.setText("unnamed");
        activePath.setForeground(Color.WHITE);
        activePath.setBackground(Color.DARK_GRAY);
        activePath.addKeyListener(new KeyListener()
        {
            @Override
            public void keyTyped(KeyEvent e)
            {
                if (activePath.getTextField().getText().length() > MAX_TEXT_LENGTH)
                {
                    e.consume();
                }
            }
            @Override
            public void keyPressed(KeyEvent e){}
            @Override
            public void keyReleased(KeyEvent e){}
        });
        northPanel.add(activePath);

        // Add panel to client panel
        add(northPanel, BorderLayout.NORTH);

        // Configure path view panel
        pathView.setLayout(new BoxLayout(pathView, BoxLayout.Y_AXIS));

        // Configure PluginErrorPanel
        noPathPanel.setVisible(false);
        //noPathPanel.setPreferredSize(new Dimension(50, 30));
        noPathPanel.setContent("No stored paths", "Shift right-click a tile to add a path point.");

        // Add body panel
        JPanel centerPanel = new JPanel(new BorderLayout());
        //centerPanel.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH-5, 0));
        centerPanel.add(noPathPanel, BorderLayout.NORTH);
        centerPanel.add(pathView, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        rebuild();
    }

    void rebuild()
    {
        pathView.removeAll();

        // HashMap<String, PathmakerPath>
        HashMap<String, PathmakerPath> paths = plugin.getStoredPaths();
        for (final String pathLabel : plugin.getStoredPaths().keySet())
        {
            // Create new path entry
            PathPanel pathEntry = new PathPanel(plugin, pathLabel);

            // Set as active path on label click
            pathEntry.getPathLabel().addActionListener(actionEvent ->
            {
                activePath.setText(pathEntry.getPathLabel().getText());
            });
            pathView.add(pathEntry, BorderLayout.CENTER);
        }

        boolean empty = pathView.getComponentCount() == 0;
        noPathPanel.setVisible(empty);

        repaint();
        revalidate();
    }
}
