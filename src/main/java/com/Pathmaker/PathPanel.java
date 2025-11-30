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
import java.awt.event.MouseAdapter;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;

public class PathPanel extends JPanel
{
    private static final Border NAME_BOTTOM_BORDER = new CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
            BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR));

    private static final ImageIcon EXPAND_ICON;
    private static final ImageIcon COLLAPSE_ICON;
    //private static final ImageIcon DELETE_ICON;
    //private static final ImageIcon BRUSH_ICON;
    private static final ImageIcon LOOP_ON_ICON;
    private static final ImageIcon LOOP_OFF_ICON;
    private static final ImageIcon EYE_OPEN_ICON;
    private static final ImageIcon EYE_CLOSED_ICON;

    private final PathmakerPlugin plugin;
    private final PathmakerPath path;

    private final JPanel pathContainer = new JPanel();
    //private final JButton expandToggle;
    private final JButton label = new JButton();
    private final JLabel deletePath = new JLabel();

    private final int ICON_WIDTH = 18;

    private boolean panelExpanded = true;
    private final JButton expandToggle;
    private final JButton visibilityToggle;
    //private final JButton deletePathButton;
    //private final JButton pathColorButton;

    private final BufferedImage brushImage = ImageUtil.loadImageResource(PathmakerPlugin.class, "brush.png");
    private final BufferedImage crossImage = ImageUtil.loadImageResource(PathmakerPlugin.class, "cross.png");

    static
    {
        //BufferedImage crossImage = ImageUtil.loadImageResource(PathmakerPlugin.class, "cross.png");
        //DELETE_ICON = new ImageIcon(crossImage);
        //BRUSH_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "brush.png"));
        BufferedImage upArrowImage = ImageUtil.loadImageResource(PathmakerPlugin.class, "up_arrow.png");
        COLLAPSE_ICON = new ImageIcon(upArrowImage);
        EXPAND_ICON= new ImageIcon(ImageUtil.rotateImage(upArrowImage, Math.PI));
        LOOP_ON_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "loop_on.png"));
        LOOP_OFF_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "loop_off.png"));
        EYE_OPEN_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "eye_open.png"));
        EYE_CLOSED_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "eye_closed.png"));
    }

    PathPanel(PathmakerPlugin plugin, String pathLabel)
    {
        this.plugin = plugin;
        this.path = plugin.getStoredPaths().get(pathLabel);


        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        //labelPanel.setBorder(NAME_BOTTOM_BORDER);

        label.setText(pathLabel);
        label.setForeground(path.color);
        label.setPreferredSize(new Dimension(130, 20)); // Client.PANEL_WIDTH = 225. (18x4 buttons, 5 margin
        labelPanel.add(label, BorderLayout.CENTER);

        expandToggle = new JButton(panelExpanded ? COLLAPSE_ICON : EXPAND_ICON);
        expandToggle.setPreferredSize(new Dimension(ICON_WIDTH, 0));
        expandToggle.setToolTipText((panelExpanded ? "Expand" : "Collapse") + " path");
        expandToggle.addActionListener(actionEvent ->
        {
            toggleCollapsed();
        });

        visibilityToggle = new JButton(path.hidden ? EYE_CLOSED_ICON : EYE_OPEN_ICON);
        visibilityToggle.setPreferredSize(new Dimension(ICON_WIDTH, 0));
        visibilityToggle.setToolTipText((path.hidden ? "Show" : "Hide") + " path");
        visibilityToggle.addActionListener(actionEvent ->
        {
            toggleVisibility();
        });

        JButton colorPickerButton = new JButton();
        colorPickerButton.setPreferredSize(new Dimension(ICON_WIDTH, 0));
        colorPickerButton.setIcon(new ImageIcon(ImageUtil.recolorImage(brushImage, path.color)));
        colorPickerButton.setToolTipText("Choose path color");
        colorPickerButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent mouseEvent)
            {
                RuneliteColorPicker colorPicker = getColorPicker(path.color == null ? plugin.getDefaultPathColor() : path.color);
                colorPicker.setOnColorChange(newColor ->
                {
                    path.color = newColor;
                    label.setForeground(newColor);
                    colorPickerButton.setIcon(new ImageIcon(ImageUtil.recolorImage(brushImage, newColor)));
                });
                colorPicker.setVisible(true);
            }
        });

        // Add button panel on the left
        JPanel leftActionPanel = new JPanel(new BorderLayout());
        leftActionPanel.add(expandToggle, BorderLayout.WEST);
        leftActionPanel.add(visibilityToggle, BorderLayout.CENTER);
        leftActionPanel.add(colorPickerButton, BorderLayout.EAST);
        labelPanel.add(leftActionPanel, BorderLayout.WEST);

        JButton deletePathButton = new JButton();
        deletePathButton.setIcon(new ImageIcon(ImageUtil.recolorImage(crossImage, Color.RED)));
        deletePathButton.setToolTipText("Delete point");
        deletePathButton.setPreferredSize(new Dimension(ICON_WIDTH, 0));
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

        // Add loop button
        JButton loopButton = new JButton();
        loopButton.setIcon(path.loopPath ? LOOP_ON_ICON : LOOP_OFF_ICON);
        loopButton.setPreferredSize(new Dimension(ICON_WIDTH, 0));
        loopButton.setToolTipText((path.loopPath ? "disable" :  "enable") + " path loop");
        loopButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent mouseEvent)
            {
                path.loopPath = !path.loopPath;
                loopButton.setToolTipText((path.loopPath ? "disable" :  "enable") + " path loop");
                loopButton.setIcon(path.loopPath ? LOOP_ON_ICON : LOOP_OFF_ICON);
            }
        });

        // Add button panel to the right
        JPanel rightActionPanel = new JPanel(new BorderLayout());
        rightActionPanel.add(loopButton, BorderLayout.WEST);
        rightActionPanel.add(deletePathButton, BorderLayout.EAST);
        labelPanel.add(rightActionPanel, BorderLayout.EAST);

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
                pointLabel.setHorizontalAlignment(SwingConstants.CENTER);
                pointLabel.setPreferredSize(new Dimension(60, 20));
                pointLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                pointContainer.add(pointLabel, BorderLayout.WEST);

                PathPoint point = regionPoints.get(i);
                // Add spinner box for optionally assigning a new point index.
                JSpinner indexSpinner = new JSpinner(new SpinnerNumberModel(point.getDrawIndex() + 1, 1, pathSize, 1));
                indexSpinner.setToolTipText("point index");
                indexSpinner.addChangeListener(ce ->
                {
                    plugin.getStoredPaths().get(pathLabel).setNewIndex(point, (Integer) indexSpinner.getValue()-1);
                    plugin.rebuildPanel();
                });
                pointContainer.add(indexSpinner, BorderLayout.CENTER);

                JButton deletePathPointButton = new JButton();
                deletePathPointButton.setIcon(new ImageIcon(crossImage));
                deletePathPointButton.setToolTipText("Delete point");
                deletePathPointButton.setPreferredSize(new Dimension(ICON_WIDTH, 0));
                deletePathPointButton.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mousePressed(MouseEvent mouseEvent)
                    {
                        plugin.removePoint(getPathLabel().getText(), point);
                        plugin.rebuildPanel();
                    }
                });
                pointContainer.add(deletePathPointButton, BorderLayout.EAST);

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

    private void toggleVisibility()
    {
        path.hidden = !path.hidden;

        visibilityToggle.setIcon(path.hidden ? EYE_CLOSED_ICON : EYE_OPEN_ICON);
        visibilityToggle.setToolTipText((path.hidden ? "Show" : "Hide") + " path");
    }

    void setPathLabel(String label)
    {
        this.label.setText(label);
    }

    JButton getPathLabel()
    {
        return label;
    }

    private RuneliteColorPicker getColorPicker(Color colour)
    {
        RuneliteColorPicker colorPicker = plugin.getColorPickerManager().create(
                SwingUtilities.windowForComponent(this),
                colour,
                label.getText() + " path color",
                false);
        colorPicker.setLocationRelativeTo(this);
        //colourPicker.setOnClose(c -> plugin.saveMarkers());
        return colorPicker;
    }
}