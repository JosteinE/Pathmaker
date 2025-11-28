package com.Pathmaker;

import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;

import java.awt.*;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.EmptyBorder;


public class PathmakerPluginPanel extends PluginPanel
{
    private final PluginErrorPanel noPathPanel = new PluginErrorPanel();
    private final JPanel pathView = new JPanel();

    Client client;
    PathmakerPlugin plugin;

    PathmakerPluginPanel(Client client, PathmakerPlugin plugin)
    {
        this.client = client;
	    this.plugin = plugin;

        // Define standard client panel layout
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create labeled panels
        JPanel northPanel = createLabeledPanel(BorderLayout.NORTH, "Pathmaker", Color.WHITE);
        JPanel southPanel = createLabeledPanel(BorderLayout.SOUTH, "By Fraph", Color.YELLOW);

        // Add panels to client panel
        add(northPanel, BorderLayout.NORTH);
        add(southPanel, BorderLayout.SOUTH);

        // Configure path view panel
        pathView.setLayout(new BoxLayout(pathView, BoxLayout.Y_AXIS));
        pathView.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Configure PluginErrorPanel
        noPathPanel.setVisible(false);
        noPathPanel.setContent("Pathmaker", "Shift right-click a tile to add a path point.");
        pathView.add(noPathPanel, BorderLayout.CENTER);

        // Add body panel
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        centerPanel.add(pathView, BorderLayout.CENTER);

        // Add path view panel to body
        add(centerPanel, BorderLayout.CENTER);

        rebuild();
    }

    void rebuild()
    {
        pathView.removeAll();

        // HashMap<String, PathmakerPath>
        HashMap<String, PathmakerPath> paths = plugin.getLinePaths();
        for (final String pathLabel : plugin.getLinePaths().keySet())
        {
//            if (group.getName().toLowerCase().contains(getSearchText().toLowerCase()) &&
//                    (Filter.ALL.equals(filter) ||
//                            (Filter.REGION.equals(filter) && plugin.anyLineInRegion(group.getLines(), regionId)) ||
//                            (Filter.VISIBLE.equals(filter) && group.isVisible()) ||
//                            (Filter.INVISIBLE.equals(filter) && !group.isVisible())))
//          {
            PathPanel pathEntry = new PathPanel(plugin, paths.get(pathLabel));
            pathEntry.setPathLabel(pathLabel);
            pathView.add(pathEntry, BorderLayout.CENTER);
            pathView.add(Box.createRigidArea(new Dimension(0, 10)));
            // }

        }

        boolean empty = pathView.getComponentCount() == 0;
        noPathPanel.setVisible(empty);

        repaint();
        revalidate();
    }

    // Create labeled panel within a panel
    JPanel createLabeledPanel(String borderLayout, String label, Color color)
    {
        // Create title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBorder(new EmptyBorder(1, 0, 10, 0));

        // Create label and add to title panel
        JLabel title = new JLabel();
        title.setText(label);
        title.setForeground(color);
        titlePanel.add(title, BorderLayout.CENTER);

        // Create body panel and add titlePanel
        JPanel bodyPanel = new JPanel(new BorderLayout());
        bodyPanel.setBorder(new EmptyBorder(1, 0, 10, 0));
        bodyPanel.add(titlePanel, borderLayout);
        return bodyPanel;
    }
}
