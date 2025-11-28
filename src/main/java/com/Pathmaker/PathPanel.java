package com.Pathmaker;


import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.FlatTextField;

public class PathPanel extends JPanel
{
    private static final Border NAME_BOTTOM_BORDER = new CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
            BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR));

    private final PathmakerPlugin plugin;
    private final PathmakerPath path;

    private final JPanel pathContainer = new JPanel();
    //private final JButton expandToggle;
    private final JLabel label = new JLabel();
    private final JLabel deletePath = new JLabel();

    PathPanel(PathmakerPlugin plugin, PathmakerPath path)
    {
        this.plugin = plugin;
        this.path = path;


        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        labelPanel.setBorder(NAME_BOTTOM_BORDER);

        //nameWrapper.add(expandToggle, BorderLayout.WEST);
        labelPanel.add(label, BorderLayout.CENTER);
        //nameWrapper.add(nameActions, BorderLayout.EAST);

        pathContainer.setLayout(new BoxLayout(pathContainer, BoxLayout.Y_AXIS));
        pathContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        pathContainer.add(labelPanel, BorderLayout.CENTER);

        for (int regionID : path.getRegionIDs())
        {
            ArrayList<PathPoint> regionPoints = path.getPointsInRegion(regionID);
            for (int i = 1; i < regionPoints.size(); i++)
            {
                JPanel pointContainer = new JPanel(new BorderLayout());
                pointContainer.setBorder(new EmptyBorder(5, 0, 5, 0));
                pointContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

                JLabel pointLabel = new JLabel();
                pointLabel.setText("p"+i);
                pointLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                pointContainer.add(pointLabel, BorderLayout.CENTER);

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

    void setPathLabel(String label)
    {
        this.label.setText(label);
    }
}