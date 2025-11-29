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

    private final PathmakerPlugin plugin;
    private final PathmakerPath path;

    private final JPanel pathContainer = new JPanel();
    //private final JButton expandToggle;
    private final JLabel label = new JLabel();
    private final JLabel deletePath = new JLabel();

    private boolean panelCollapsed = false;
    private final JButton expandToggle;

    static
    {
        BufferedImage upArrow = ImageUtil.loadImageResource(PathmakerPlugin.class, "up_arrow.png");
        COLLAPSE_ICON = new ImageIcon(upArrow);
        EXPAND_ICON= new ImageIcon(ImageUtil.rotateImage(upArrow, Math.PI));
    }

    PathPanel(PathmakerPlugin plugin, String pathLabel)
    {
        this.plugin = plugin;
        this.path = plugin.paths.get(pathLabel);


        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        labelPanel.setBorder(NAME_BOTTOM_BORDER);

        label.setText(pathLabel);
        //nameWrapper.add(expandToggle, BorderLayout.WEST);
        labelPanel.add(label, BorderLayout.CENTER);
        //nameWrapper.add(nameActions, BorderLayout.EAST);

        expandToggle = new JButton(panelCollapsed ? COLLAPSE_ICON : EXPAND_ICON);
        expandToggle.setPreferredSize(new Dimension(15, 0));
        expandToggle.setBorder(new EmptyBorder(0, 6, 1, 0));
        expandToggle.setToolTipText((panelCollapsed ? "Expand" : "Collapse") + " path");
        expandToggle.addActionListener(actionEvent ->
        {
            toggleCollapsed();
            //plugin.saveMarkers();
        });
        labelPanel.add(expandToggle, BorderLayout.WEST);

        pathContainer.setLayout(new BoxLayout(pathContainer, BoxLayout.Y_AXIS));
        pathContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        pathContainer.add(labelPanel, BorderLayout.CENTER);

        int pathSize = path.getSize();
        for (int regionID : path.getRegionIDs())
        {
            ArrayList<PathPoint> regionPoints = path.getPointsInRegion(regionID);
            for (int i = 1; i < regionPoints.size(); i++)
            {
                JPanel pointContainer = new JPanel(new BorderLayout());
                pointContainer.setBorder(new EmptyBorder(5, 0, 5, 0));
                pointContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

                //
                JLabel pointLabel = new JLabel();
                pointLabel.setText("p"+i);
                pointLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                pointContainer.add(pointLabel, BorderLayout.CENTER);

                // Add spinner box for optionally assigning a new point index.
                JSpinner indexSpinner = new JSpinner(new SpinnerNumberModel(i, 0, pathSize, 1));
                indexSpinner.setToolTipText("point index");
                //index.setPreferredSize(new Dimension(53, 20));
                PathPoint point = regionPoints.get(i);
                indexSpinner.addChangeListener(ce ->
                {
                    plugin.paths.get(pathLabel).setNewIndex(point, (Integer) indexSpinner.getValue());
                    // rebuild
                    // redraw path
                    //plugin.saveMarkers();
                });
                pointContainer.add(indexSpinner, BorderLayout.CENTER);

                // IMPLEMENT REMOVE FUNC
//                JLabel deletePathPoint = new JLabel();
//                //deletePath.setIcon(DELETE_ICON);
//                deletePathPoint.setToolTipText("Delete point");
//                deletePathPoint.setBorder(new EmptyBorder(1, 0, 0, 0));
//                deletePathPoint.addMouseListener(new MouseAdapter() {
//                    @Override
//                    public void mousePressed(MouseEvent mouseEvent) {
//                        int confirm = JOptionPane.showConfirmDialog(PathPanel.this,
//                                "Are you sure you want to permanently delete this path point?",
//                                "Warning", JOptionPane.OK_CANCEL_OPTION);
//
//                        if (confirm == 0) {
//                            plugin.removePathPoint(regionPoint);
//                        }
//                    }
//
//                    @Override
//                    public void mouseEntered(MouseEvent mouseEvent) {
//                        deletePath.setIcon(DELETE_HOVER_ICON);
//                    }
//
//                    @Override
//                    public void mouseExited(MouseEvent mouseEvent) {
//                        deleteLine.setIcon(DELETE_ICON);
//                    }
//                });

                //rightActions.add(deleteLine);
                //pointContainer.add(rightActions, BorderLayout.EAST);

                pathContainer.add(pointContainer);
            }
        }
        add(pathContainer);
    }

    private void toggleCollapsed()
    {
        panelCollapsed = !panelCollapsed;
        for (int i = 1; i < pathContainer.getComponentCount(); i++)
        {
            pathContainer.getComponent(i).setVisible(panelCollapsed);
        }

        expandToggle.setIcon(panelCollapsed ? COLLAPSE_ICON : EXPAND_ICON);
        expandToggle.setToolTipText((panelCollapsed ? "Collapse" : "Expand") + " path");
    }

    void setPathLabel(String label)
    {
        this.label.setText(label);
    }
}