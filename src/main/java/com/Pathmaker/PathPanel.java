package com.Pathmaker;


import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.ImageIcon;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.util.ImageUtil;

public class PathPanel extends JPanel
{
    private static final Border NAME_BOTTOM_BORDER = new CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
            BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR));

    private static final ImageIcon EXPAND_ICON;
    private static final ImageIcon COLLAPSE_ICON;
    private static final ImageIcon DELETE_ICON;
    private static final ImageIcon BRUSH_ICON;

    private final PathmakerPlugin plugin;
    private final PathmakerPath path;

    private final JPanel pathContainer = new JPanel();
    //private final JButton expandToggle;
    private final JButton label = new JButton();
    private final JLabel deletePath = new JLabel();

    private boolean panelExpanded = true;
    private final JButton expandToggle;
    private final JButton deletePathButton;
    //private final JButton pathColorButton;

    static
    {
        DELETE_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "cross.png"));
        BRUSH_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "brush.png"));
        BufferedImage upArrow = ImageUtil.loadImageResource(PathmakerPlugin.class, "up_arrow.png");
        COLLAPSE_ICON = new ImageIcon(upArrow);
        EXPAND_ICON= new ImageIcon(ImageUtil.rotateImage(upArrow, Math.PI));
    }

    PathPanel(PathmakerPlugin plugin, String pathLabel)
    {
        this.plugin = plugin;
        this.path = plugin.getStoredPaths().get(pathLabel);


        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        labelPanel.setBorder(NAME_BOTTOM_BORDER);

        label.setText(pathLabel);
        //nameWrapper.add(expandToggle, BorderLayout.WEST);
        labelPanel.add(label, BorderLayout.CENTER);
        //nameWrapper.add(nameActions, BorderLayout.EAST);

        expandToggle = new JButton(panelExpanded ? COLLAPSE_ICON : EXPAND_ICON);
        expandToggle.setPreferredSize(new Dimension(15, 0));
        //expandToggle.setBorder(new EmptyBorder(0, 6, 1, 0));
        expandToggle.setToolTipText((panelExpanded ? "Expand" : "Collapse") + " path");
        expandToggle.addActionListener(actionEvent ->
        {
            toggleCollapsed();
            //plugin.saveMarkers();
        });

        JButton colorPicker = new JButton();
        colorPicker.setPreferredSize(new Dimension(15, 0));
        colorPicker.setIcon(BRUSH_ICON);
        colorPicker.setToolTipText("Choose path color");
        colorPicker.addActionListener(actionEvent ->
        {
//            @Override
//            public void mousePressed(MouseEvent mouseEvent)
//            {
//                RuneliteColorPicker colourPicker = getColourPicker(line.getColour() == null ? plugin.defaultColour : line.getColour());
//                colourPicker.setOnColorChange(c ->
//                {
//                    line.setColour(c);
//                    colour.setBorder(line.getWidth() == 0 ? null : new MatteBorder(0, 0, 3, 0, line.getColour()));
//                    colour.setIcon(line.getWidth() == 0 ? NO_SETTINGS_ICON : SETTINGS_ICON);
//                });
//                colourPicker.setVisible(true);
        });

        JPanel leftActionPanel = new JPanel(new BorderLayout());
        leftActionPanel.add(expandToggle, BorderLayout.WEST);
        leftActionPanel.add(colorPicker, BorderLayout.EAST);
        labelPanel.add(leftActionPanel, BorderLayout.WEST);

        deletePathButton = new JButton();
        deletePathButton.setIcon(DELETE_ICON);
        deletePathButton.setToolTipText("Delete point");
        deletePathButton.setBorder(new EmptyBorder(1, 0, 0, 0));
        String warningMsg = "Are you sure you want to permanently delete path: " + label.getText() + "?";
        deletePathButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                int confirm = JOptionPane.showConfirmDialog(PathPanel.this,
                        warningMsg,
                        "Warning", JOptionPane.OK_CANCEL_OPTION);

                if (confirm == 0)
                {
                    plugin.removePath(label.getText());
                }
            }
        });
        labelPanel.add(deletePathButton, BorderLayout.EAST);

//        pathColorButton = new JButton();
//        pathColorButton.setToolTipText("Edit path colour");
//        pathColorButton.setForeground(pathColorButton.getColour() == null ? plugin.defaultColour : line.getColour());
//        pathColorButton.setBorder(line.getWidth() == 0 ? null : new MatteBorder(0, 0, 3, 0, line.getColour()));
//        pathColorButton.setIcon(line.getWidth() == 0 ? NO_SETTINGS_ICON : SETTINGS_ICON);
//        pathColorButton.addMouseListener(new MouseAdapter()
//        {
//            @Override
//            public void mousePressed(MouseEvent mouseEvent)
//            {
//                RuneliteColorPicker colourPicker = getColourPicker(line.getColour() == null ? plugin.defaultColour : line.getColour());
//                colourPicker.setOnColorChange(c ->
//                {
//                    line.setColour(c);
//                    colour.setBorder(line.getWidth() == 0 ? null : new MatteBorder(0, 0, 3, 0, line.getColour()));
//                    colour.setIcon(line.getWidth() == 0 ? NO_SETTINGS_ICON : SETTINGS_ICON);
//                });
//                colourPicker.setVisible(true);
//            }
//        });

        pathContainer.setLayout(new BoxLayout(pathContainer, BoxLayout.Y_AXIS));
        pathContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        pathContainer.add(labelPanel, BorderLayout.CENTER);

        int pathSize = path.getSize();
        for (int regionID : path.getRegionIDs())
        {
            ArrayList<PathPoint> regionPoints = path.getPointsInRegion(regionID);
            for (int i = 0; i < regionPoints.size(); i++)
            {
                JPanel pointContainer = new JPanel(new BorderLayout());
                pointContainer.setBorder(new EmptyBorder(5, 0, 5, 0));
                pointContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

                //
                JLabel pointLabel = new JLabel();
                pointLabel.setText("Point: ");
                pointLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                pointContainer.add(pointLabel, BorderLayout.WEST);

                PathPoint point = regionPoints.get(i);
                // Add spinner box for optionally assigning a new point index.
                JSpinner indexSpinner = new JSpinner(new SpinnerNumberModel(point.getIndex() + 1, 1, pathSize, 1));
                indexSpinner.setToolTipText("point index");
                //index.setPreferredSize(new Dimension(53, 20));
                indexSpinner.addChangeListener(ce ->
                {
                    plugin.getStoredPaths().get(pathLabel).setNewIndex(point, (Integer) indexSpinner.getValue()-1);
                    // rebuild
                    // redraw path
                    //plugin.saveMarkers();
                });
                pointContainer.add(indexSpinner, BorderLayout.CENTER);

                JButton deletePathPointButton = new JButton();
                deletePath.setIcon(DELETE_ICON);
                deletePathPointButton.setToolTipText("Delete point");
                deletePathPointButton.setBorder(new EmptyBorder(1, 0, 0, 0));
                deletePathPointButton.addMouseListener(new MouseAdapter()
                {
                    //plugin.removePathPoint(regionPoint);
                });
                pointContainer.add(deletePathPointButton, BorderLayout.EAST);

                //rightActions.add(deleteLine);
                //pointContainer.add(rightActions, BorderLayout.EAST);
                pointContainer.setVisible(panelExpanded);
                pathContainer.add(pointContainer);
            }
        }
        add(pathContainer);
    }

    private void toggleCollapsed()
    {
        panelExpanded = !panelExpanded;
        for (int i = 1; i < pathContainer.getComponentCount(); i++)
        {
            pathContainer.getComponent(i).setVisible(panelExpanded);
        }

        expandToggle.setIcon(panelExpanded ? COLLAPSE_ICON : EXPAND_ICON);
        expandToggle.setToolTipText((panelExpanded ? "Collapse" : "Expand") + " path");
    }

    void setPathLabel(String label)
    {
        this.label.setText(label);
    }

    JButton getPathLabel()
    {
        return label;
    }
}