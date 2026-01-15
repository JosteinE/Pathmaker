/*
 * Copyright (c) 2025, JosteinE
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *	  list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	  this list of conditions and the following disclaimer in the documentation
 *	  and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.Pathmaker;


import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.Component;

import java.util.ArrayList;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.ImageIcon;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;

@Slf4j
public class PathPanel extends JPanel
{
    private static final Border NAME_BOTTOM_BORDER = new CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
            BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR));

    private static final ImageIcon EXPAND_ICON;
    private static final ImageIcon COLLAPSE_ICON;
    private static final ImageIcon LOOP_ON_ICON;
    private static final ImageIcon LOOP_OFF_ICON;
    private static final ImageIcon EYE_OPEN_ICON;
	private static final ImageIcon EYE_CLOSED_ICON;
	private static final ImageIcon OFFSET_LEFT_ICON;
	private static final ImageIcon OFFSET_MIDDLE_ICON;
	private static final ImageIcon OFFSET_RIGHT_ICON;
	private static final ImageIcon PERSON_ICON;
	private static final ImageIcon PERSON_GREEN_ICON;
	private static final ImageIcon PERSON_GREEN_LINES_ICON;
	private static final ImageIcon TRIPLE_DOTS_ICON;

	enum pathDrawOffset
	{
		OFFSET_LEFT,
		OFFSET_MIDDLE,
		OFFSET_RIGHT,
	}

	enum drawFromPlayerMode
	{
		NEVER,
		START_ONLY,
		ALWAYS,
	}

    private final PathmakerPlugin plugin;
    private final PathmakerPath path;

    private final JPanel pathContainer = new JPanel();
    private final JButton label = new JButton();
	private final JPanel labelPanel;

    private final int ICON_WIDTH = 18;

    //private boolean panelExpanded = true;
    private final JButton expandToggle;
    private final JButton visibilityToggle;

    private final BufferedImage brushImage = ImageUtil.loadImageResource(PathmakerPlugin.class, "brush.png");
    private final BufferedImage crossImage = ImageUtil.loadImageResource(PathmakerPlugin.class, "cross.png");

    static
    {
        BufferedImage upArrowImage = ImageUtil.loadImageResource(PathmakerPlugin.class, "up_arrow.png");
        COLLAPSE_ICON = new ImageIcon(upArrowImage);
        EXPAND_ICON = new ImageIcon(ImageUtil.flipImage(upArrowImage, false, true));
        LOOP_ON_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "loop_on.png"));
        LOOP_OFF_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "loop_off.png"));
		OFFSET_LEFT_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "offset_left.png"));
		OFFSET_MIDDLE_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "offset_middle.png"));
		OFFSET_RIGHT_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "offset_right.png"));
        EYE_OPEN_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "eye_open.png"));
        EYE_CLOSED_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "eye_closed.png"));
		PERSON_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "person.png"));
		PERSON_GREEN_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "person_green.png"));
		PERSON_GREEN_LINES_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "person_green_lines.png"));
		TRIPLE_DOTS_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "triple_dots.png"));
    }

    PathPanel(PathmakerPlugin plugin, String pathLabel)
    {
        this.plugin = plugin;
        this.path = plugin.getStoredPaths().get(pathLabel);
		pathContainer.setLayout(new BoxLayout(pathContainer, BoxLayout.Y_AXIS));

        labelPanel = new JPanel(new BorderLayout());
        labelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        label.setText(pathLabel);
        label.setForeground(Color.WHITE); //path.color);
		label.setToolTipText(pathLabel + ": " + path.getSize() + " point" + (path.getSize() > 1 ? "s, " : ", ") +
			path.getRegionIDs().size() + " region" + (path.getRegionIDs().size() > 1 ? "s" : ""));
        label.setPreferredSize(new Dimension(122, 20)); // Client.PANEL_WIDTH = 225. (18x6 buttons, 5 margin
        labelPanel.add(label, BorderLayout.CENTER);

        expandToggle = PanelBuildUtils.createExpandToggleButton(path.panelExpanded, ICON_WIDTH, 0, "path");
        expandToggle.addActionListener(actionEvent ->
        {
            toggleCollapsed();
			plugin.savePath(getPathLabel());
			plugin.rebuildPanel(false);
        });

        visibilityToggle = new JButton(path.hidden ? EYE_CLOSED_ICON : EYE_OPEN_ICON);
        visibilityToggle.setPreferredSize(new Dimension(ICON_WIDTH, 0));
        visibilityToggle.setToolTipText((path.hidden ? "Show" : "Hide") + " path");
        visibilityToggle.addActionListener(actionEvent ->
        {
            setVisibility(!path.hidden);
        });

        JButton colorPickerButton = PanelBuildUtils.createColorPickerButton(ICON_WIDTH, 0, path.color, "path");
        colorPickerButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent mouseEvent)
            {
                RuneliteColorPicker colorPicker = getColorPicker(path.color == null ? plugin.getDefaultPathColor() : path.color, colorPickerButton);
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

        JButton deletePathButton = PanelBuildUtils.createDeleteButton(plugin, ICON_WIDTH, 0 , this, getPathLabel(),"path",true);
//        deletePathButton.setIcon(new ImageIcon(ImageUtil.recolorImage(crossImage, Color.RED)));
//        deletePathButton.setToolTipText("Delete path");
//        deletePathButton.setPreferredSize(new Dimension(ICON_WIDTH, 0));
//        String warningMsg = "Are you sure you want to permanently delete path: " + label.getText() + "?";
//        deletePathButton.addMouseListener(new MouseAdapter()
//        {
//            @Override
//            public void mousePressed(MouseEvent mouseEvent) {
//                int confirm = JOptionPane.showConfirmDialog(PathPanel.this,
//                        warningMsg,
//                        "Warning", JOptionPane.OK_CANCEL_OPTION);
//
//                if (confirm == 0)
//                {
//                    plugin.removePath(label.getText());
//                    plugin.rebuildPanel(true);
//                }
//            }
//        });

        // Add loop button
//        JButton loopButton = new JButton();
//        loopButton.setIcon(path.loopPath ? LOOP_ON_ICON : LOOP_OFF_ICON);
//        loopButton.setPreferredSize(new Dimension(ICON_WIDTH, 0));
//        loopButton.setToolTipText((path.loopPath ? "disable" :  "enable") + " path loop");
//        loopButton.addMouseListener(new MouseAdapter()
//        {
//            @Override
//            public void mousePressed(MouseEvent mouseEvent)
//            {
//                path.loopPath = !path.loopPath;
//                loopButton.setToolTipText((path.loopPath ? "disable" :  "enable") + " path loop");
//                loopButton.setIcon(path.loopPath ? LOOP_ON_ICON : LOOP_OFF_ICON);
//            }
//        });

		// Add offset button
		JButton offsetButton = new JButton();
		offsetButton.setIcon(getPathDrawOffsetIcon(path));
		offsetButton.setPreferredSize(new Dimension(ICON_WIDTH, 0));
		offsetButton.setToolTipText("Set path draw offset");
		offsetButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				path.pathDrawOffset = path.pathDrawOffset + 1 > 2 ? 0 : path.pathDrawOffset + 1;
				offsetButton.setIcon(getPathDrawOffsetIcon(path));
				//plugin.saveAll();

				offsetButton.updateUI();
			}
		});

		int pathSize = path.getSize();
		ArrayList<PathPoint> drawOrder = path.getDrawOrder(null);

		// Add offset button
//		JButton loopToPlayerButton = new JButton();
//		loopToPlayerButton.setIcon(getLoopToPlayerIcon(path.drawToPlayer));
//		loopToPlayerButton.setPreferredSize(new Dimension(ICON_WIDTH, 0));
//		loopToPlayerButton.setToolTipText(getLoopToPlayerTooltip(path.drawToPlayer));
//		loopToPlayerButton.addMouseListener(new MouseAdapter()
//		{
//			@Override
//			public void mousePressed(MouseEvent mouseEvent)
//			{
//
//				path.drawToPlayer = path.drawToPlayer + 1 > 2 ? 0 : path.drawToPlayer + 1;
//				loopToPlayerButton.setIcon(getLoopToPlayerIcon(path.drawToPlayer));
//				loopToPlayerButton.setToolTipText(getLoopToPlayerTooltip(path.drawToPlayer));
//				plugin.savePath(getPathLabel());
//				plugin.rebuildPanel(false);
//			}
//		});

		// Main options & menu
		JButton optionsButton = new JButton();
		optionsButton.setIcon(TRIPLE_DOTS_ICON);
		optionsButton.setPreferredSize(new Dimension(ICON_WIDTH, 0));
		optionsButton.setToolTipText("Options");

		JPopupMenu optionsMenu = new JPopupMenu();

		Runnable openOptionsMenu = () -> {optionsMenu.show(optionsButton, 0, labelPanel.getHeight());};

		optionsButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				openOptionsMenu.run();
			}
		});

		int iconTextGap = 10;

		// Rename option
		JMenuItem renameMenuEntry = optionsMenu.add("Rename path (WIP)");
		//renameMenuEntry.setIcon();
		renameMenuEntry.setIconTextGap(iconTextGap);
		renameMenuEntry.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent mouseEvent)
			{
				super.mouseReleased(mouseEvent);

				JLabel centeredNameText = new JLabel("New path name (WIP)", JLabel.CENTER);
				centeredNameText.setHorizontalTextPosition(SwingConstants.CENTER);
				String newPathName = JOptionPane.showInputDialog(optionsButton, centeredNameText, getPathLabel());
				// todo: rename option
			}
		});

		// Label mode option
		JMenu pointLabelModeSubMenu = new JMenu("Text");
		pointLabelModeSubMenu.setIconTextGap(iconTextGap);
		optionsMenu.add(pointLabelModeSubMenu);

		Function<String, MouseAdapter> checkBoxModeAdapter = modeType ->
			new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					super.mousePressed(e);

					JCheckBoxMenuItem checkBox = (JCheckBoxMenuItem) e.getComponent();
					boolean boxState = !checkBox.getState(); // (getState() changes after this is called)
					int relativeToType = checkBox.getText().equals("Tiles") || checkBox.getText().equals("Index") ? 1 : 2;
					int currentMode;

					if(modeType.equals("Label"))
						currentMode = path.labelMode.ordinal();
					else
						currentMode = path.pointDrawMode.ordinal();

					int newMode = currentMode;

					switch (currentMode)
					{
						case 0: // NONE
						{
							if (boxState)
							{
								if (relativeToType == 1) newMode = 1;	// Returning INDEX or TILES
								else newMode = 2; 						// Returning LABEL or NPCS_AND_OBJECTS
							}
							break;
						}
						case 1: // INDEX or TILES
						{
							if (!boxState && relativeToType == 1) newMode = 0;		// Returning NONE
							else if (boxState && relativeToType == 2) newMode = 3;	// Returning BOTH
							break;
						}
						case 2: // LABEL or NPCS_AND_OBJECTS
						{
							if (boxState && relativeToType == 1) newMode = 3;		// Returning BOTH
							else if (!boxState && relativeToType == 2) newMode = 0;	// Returning NONE
							break;
						}
						case 3: // BOTH
						{
							if (!boxState)
							{
								if (relativeToType == 1) newMode = 2;	// Returning LABEL or NPCS_AND_OBJECTS
								else newMode = 1;						// Returning INDEX or TILES
							}
							break;
						}
					}

					if(modeType.equals("Label"))
						path.labelMode = PathmakerConfig.pathPointLabelMode.values()[newMode];
					else
						path.pointDrawMode = PathmakerConfig.pathPointMode.values()[newMode];
				}
			};

		JCheckBoxMenuItem indexModeMenuEntry =  new JCheckBoxMenuItem("Index");
		indexModeMenuEntry.setIconTextGap(iconTextGap);
		indexModeMenuEntry.setState(path.labelMode == PathmakerConfig.pathPointLabelMode.INDEX || path.labelMode == PathmakerConfig.pathPointLabelMode.BOTH);
		indexModeMenuEntry.addMouseListener(checkBoxModeAdapter.apply("Label"));

		JCheckBoxMenuItem labelModeMenuEntry = new JCheckBoxMenuItem("Label");
		labelModeMenuEntry.setIconTextGap(iconTextGap);
		labelModeMenuEntry.setState(path.labelMode == PathmakerConfig.pathPointLabelMode.LABEL || path.labelMode == PathmakerConfig.pathPointLabelMode.BOTH);
		labelModeMenuEntry.addMouseListener(checkBoxModeAdapter.apply("Label"));

		pointLabelModeSubMenu.add(indexModeMenuEntry);
		pointLabelModeSubMenu.add(labelModeMenuEntry);

		// Highlight mode option
		JMenu pointHighlightModeSubMenu = new JMenu("Highlight");
		pointHighlightModeSubMenu.setIconTextGap(iconTextGap);
		optionsMenu.add(pointHighlightModeSubMenu);

		JCheckBoxMenuItem highlightTilesMenuEntry = new JCheckBoxMenuItem("Tiles");
		highlightTilesMenuEntry.setIconTextGap(iconTextGap);
		highlightTilesMenuEntry.setState(path.pointDrawMode == PathmakerConfig.pathPointMode.BOTH ||
			path.pointDrawMode == PathmakerConfig.pathPointMode.TILES);
		highlightTilesMenuEntry.addMouseListener(checkBoxModeAdapter.apply("Highlight"));

		JCheckBoxMenuItem highlightObjectsAndNpcMenuEntry = new JCheckBoxMenuItem("Object and NPCs");
		highlightObjectsAndNpcMenuEntry.setIconTextGap(iconTextGap);
		highlightObjectsAndNpcMenuEntry.setState(path.pointDrawMode == PathmakerConfig.pathPointMode.BOTH ||
			path.pointDrawMode == PathmakerConfig.pathPointMode.NPCS_AND_OBJECTS);
		highlightObjectsAndNpcMenuEntry.addMouseListener(checkBoxModeAdapter.apply("Highlight"));

		pointHighlightModeSubMenu.add(highlightTilesMenuEntry);
		pointHighlightModeSubMenu.add(highlightObjectsAndNpcMenuEntry);

		// Loop path
		JMenuItem loopMenuEntry = optionsMenu.add((path.loopPath ? "Unloop" : "Loop") + " path");
		loopMenuEntry.setIcon(path.loopPath ? LOOP_ON_ICON : LOOP_OFF_ICON);
		loopMenuEntry.setIconTextGap(iconTextGap);
		loopMenuEntry.setToolTipText((path.loopPath ? "Disable" :  "Enable") + " path loop");
		loopMenuEntry.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseReleased(MouseEvent mouseEvent)
            {
				super.mouseReleased(mouseEvent);
                path.loopPath = !path.loopPath;
				loopMenuEntry.setText((path.loopPath ? "Unloop" : "Loop") + " path");
				loopMenuEntry.setToolTipText((path.loopPath ? "Disable" :  "Enable") + " path loop");
				loopMenuEntry.setIcon(path.loopPath ? LOOP_ON_ICON : LOOP_OFF_ICON);

				openOptionsMenu.run();
            }
        });

		// Connect to player sub menu
		JMenu connectToPlayerSubMenu = PanelBuildUtils.createDrawToPlayerMenu(plugin, path, pathLabel);
		connectToPlayerSubMenu.setIcon(getLoopToPlayerIcon(path.drawToPlayer));
		connectToPlayerSubMenu.setIconTextGap(iconTextGap);
		optionsMenu.add(connectToPlayerSubMenu);

		// Add button panel to the right
        JPanel rightActionPanel = new JPanel(new BorderLayout());
		//rightActionPanel.add(loopButton, BorderLayout.WEST);
		//rightActionPanel.add(loopToPlayerButton, BorderLayout.WEST);
		rightActionPanel.add(offsetButton, BorderLayout.WEST);
		rightActionPanel.add(optionsButton, BorderLayout.CENTER);
        rightActionPanel.add(deletePathButton, BorderLayout.EAST);
        labelPanel.add(rightActionPanel, BorderLayout.EAST);

        pathContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        pathContainer.add(labelPanel, BorderLayout.CENTER);

		if (path.panelExpanded)
		{
			for (int i = pathSize - 1; i >= 0; i--)
			{
				PathPoint point = drawOrder.get(i);
				JPanel pointContainer = new JPanel(new BorderLayout());
				pointContainer.setBorder(new EmptyBorder(0, 0, 0, 0));
				pointContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

				//
				FlatTextField pointLabel = new FlatTextField();
				String label = "";// = "Point";
				label = (point.getLabel() != null && !point.getLabel().isEmpty()) ? point.getLabel() : label;
				pointLabel.setText(label);
				pointLabel.setForeground(Color.WHITE);
				pointLabel.setBackground(Color.DARK_GRAY);
				pointLabel.setPreferredSize(new Dimension(150, 20));
				pointLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
				((AbstractDocument) pointLabel.getTextField().getDocument()).setDocumentFilter(new MaxLengthFilter(plugin.MAX_POINT_LABEL_LENGTH));
				pointLabel.getDocument().addDocumentListener(new DocumentListener()
				{
					public void insertUpdate(DocumentEvent e)
					{
						point.setLabel(pointLabel.getText());
					}

					public void removeUpdate(DocumentEvent e)
					{
						point.setLabel(pointLabel.getText());
					}

					public void changedUpdate(DocumentEvent e)
					{
					}
				});

				pointContainer.add(pointLabel, BorderLayout.WEST);

				// Add spinner box for optionally assigning a new point index.
				JSpinner indexSpinner = new JSpinner(new SpinnerNumberModel(point.getDrawIndex() + 1, 1, pathSize, 1));
				indexSpinner.setToolTipText("point index");
				indexSpinner.addChangeListener(ce ->
				{
					path.setNewIndex(point, (Integer) indexSpinner.getValue() - 1);
					plugin.rebuildPanel(true);
				});
				SpinnerNumberModel model = (SpinnerNumberModel) indexSpinner.getModel();
				// indexSpinner.getComponents()[0].getName() == "Spinner.nextButton"
				// indexSpinner.getComponents()[1].getName() == "Spinner.previousButton"
				indexSpinner.getComponents()[0].setEnabled(!model.getMaximum().equals(model.getValue()));
				indexSpinner.getComponents()[1].setEnabled(!model.getMinimum().equals(model.getValue()));

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
						plugin.removePoint(getPathLabel(), point);
						// Rebuilding in removePoint (because of the in-game shift+click dropdown menu)
					}
				});
				pointContainer.add(deletePathPointButton, BorderLayout.EAST);

				if (i > 0 || path.drawToPlayer != drawFromPlayerMode.NEVER.ordinal())
				{
					pointContainer.add(addDrawToLastButton(point), BorderLayout.SOUTH);
				}

				pointContainer.setVisible(path.panelExpanded);
				pathContainer.add(pointContainer);
			}
		}
        add(pathContainer);
    }

	private JButton addDrawToLastButton(PathPoint point)
	{
		JButton drawToLastButton = new JButton();
		//drawToLastButton.setIcon(new ImageIcon(crossImage));
		drawToLastButton.setToolTipText(point.drawToPrevious ? "Unlink from previous point" : "Link to previous point");
		Color defaultColor = drawToLastButton.getBackground();
		drawToLastButton.setBackground(point.drawToPrevious ? defaultColor : Color.RED);
		//drawToLastButton.setText(point.drawToPrevious ? "(linked)" : "(unlinked)");
		//drawToLastButton.setForeground(getBackground().brighter().brighter());
		drawToLastButton.setPreferredSize(new Dimension(0, 10));
		drawToLastButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				point.drawToPrevious = !point.drawToPrevious;
				drawToLastButton.setBackground(point.drawToPrevious ? defaultColor : Color.RED);
				//drawToLastButton.setText(point.drawToPrevious ? "(linked)" : "(unlinked)");
				//drawToLastButton.setForeground(getBackground().brighter().brighter());
				plugin.saveAll();
			}
		});
		return drawToLastButton;
	}

    private void toggleCollapsed()
    {
        path.panelExpanded = !path.panelExpanded;
        for (int i = 1; i < pathContainer.getComponentCount(); i++)
        {
            pathContainer.getComponent(i).setVisible(path.panelExpanded);
        }

        expandToggle.setIcon(path.panelExpanded ? COLLAPSE_ICON : EXPAND_ICON);
        expandToggle.setToolTipText((path.panelExpanded ? "Collapse" : "Expand") + " path");
    }

    void setVisibility(boolean hidden)
    {
        path.hidden = hidden;

        visibilityToggle.setIcon(path.hidden ? EYE_CLOSED_ICON : EYE_OPEN_ICON);
        visibilityToggle.setToolTipText((path.hidden ? "Show" : "Hide") + " path");
    }

    void setPathLabel(String label)
    {
        this.label.setText(label);
    }

	JButton getPathLabelButton()
	{
		return label;
	}

    String getPathLabel()
    {
        return label.getText();
    }

	JPanel getLabelPanel()
	{
		return labelPanel;
	}

	ImageIcon getPathDrawOffsetIcon(PathmakerPath path)
	{
		switch (pathDrawOffset.values()[path.pathDrawOffset])
		{
			case OFFSET_LEFT: return OFFSET_LEFT_ICON;
			case OFFSET_RIGHT: return OFFSET_RIGHT_ICON;
			default: return OFFSET_MIDDLE_ICON;
		}
	}

	ImageIcon getLoopToPlayerIcon(int drawToPlayer)
	{
		switch (drawFromPlayerMode.values()[drawToPlayer])
		{
			case START_ONLY: return PERSON_GREEN_ICON;
			case ALWAYS: return PERSON_GREEN_LINES_ICON;
			default: return PERSON_ICON;
		}
	}

	String getLoopToPlayerTooltip(int drawToPlayer)
	{
		switch (drawFromPlayerMode.values()[drawToPlayer])
		{
			case START_ONLY: return "Set always draw to player";
			case ALWAYS: return "Un-set player from path";
			default: return "Set player as point 0";
		}
	}

    private RuneliteColorPicker getColorPicker(Color colour, Component relativeTo)
    {
        RuneliteColorPicker colorPicker = plugin.getColorPickerManager().create(
                SwingUtilities.windowForComponent(this),
                colour,
                label.getText() + " path color",
                false);
        colorPicker.setLocationRelativeTo(relativeTo);
        return colorPicker;
    }
}