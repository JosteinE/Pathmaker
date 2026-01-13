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

import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;

@Slf4j
public class GroupPanel extends JPanel
{
	FlatTextField groupTextField = new FlatTextField();
	JPanel memberPanel = new JPanel();
	JButton visibilityToggle;
	JButton colorPicker;
	PathmakerPluginPanel.PathGroup group = new PathmakerPluginPanel.PathGroup();
	JButton expandToggle;
	boolean beingDragged = false;
//	boolean expanded = true;
//	boolean hidden = false;
//	Color color = new Color(0, 100,100);

	GroupPanel(PathmakerPlugin plugin, JPanel parentPanel, PathPanel firstPathEntry, String groupName, int parentPanelIndex)
	{
		JPanel groupPanel = this;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		((AbstractDocument) groupTextField.getTextField().getDocument()).setDocumentFilter(new MaxLengthFilter(20));
		groupTextField.setText(groupName);
		groupTextField.getTextField().setEnabled(false);
		setBorder(createDefaultBorder());

		// Drag & Drop panel margin
		int PANEL_MARGIN = 20;
		// Add drag and drop adapters, but also extra logic to allow for renaming on click.
		MouseMotionAdapter dragAdapter = new DragAdapter(parentPanel, groupPanel, PANEL_MARGIN);
		groupTextField.getTextField().addMouseMotionListener(new MouseMotionAdapter()
		{
			@Override
			public void mouseDragged(MouseEvent e)
			{
				super.mouseDragged(e);
				beingDragged = true;
				dragAdapter.mouseDragged(e);
			}
		});

		DropAdapter dropAdapter = new DropAdapter(plugin, parentPanel, groupPanel, parentPanelIndex, null, PANEL_MARGIN);
		groupTextField.getTextField().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				super.mouseClicked(e);
				groupTextField.getTextField().setEnabled(true);
				groupTextField.requestFocusInWindow();
				groupTextField.setBackground(Color.DARK_GRAY);
				beingDragged = false;
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				super.mouseReleased(e);
				if (beingDragged) // need to do this, otherwise mouseClicked doesn't get called
				{
					dropAdapter.mouseReleased(e);
					beingDragged = false;
				}
			}
		});
		groupTextField.getTextField().addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				beingDragged = false;
				groupTextField.getTextField().setText(groupName);
				finalizeEditing();
			}
		});

		int iconSize = PanelBuildUtils.ICON_SIZE;
		String panelTypeText = "group";

		expandToggle = PanelBuildUtils.createExpandToggleButton(group.expanded, iconSize, iconSize, panelTypeText);
		expandToggle.addActionListener(actionEvent ->
		{
			setExpanded(!group.expanded);
			plugin.saveGroup(groupName);
			plugin.rebuildPanel(false);
		});

		visibilityToggle = PanelBuildUtils.createVisibilityToggleButton(group.hidden, iconSize, iconSize, panelTypeText);
		visibilityToggle.addActionListener(actionEvent ->
		{
			setHidden(!group.hidden);
			plugin.saveAll();
		});
		colorPicker = PanelBuildUtils.createColorPickerButton(iconSize, iconSize, group.color, panelTypeText);
		colorPicker.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				super.mousePressed(e);
				RuneliteColorPicker cPicker = PanelBuildUtils.getColorPicker(plugin.getColorPickerManager(), group.color, groupPanel, colorPicker, panelTypeText);
				cPicker.setOnColorChange(newColor ->
				{
					setColor(newColor);
					colorPicker.setIcon(PanelBuildUtils.getRecoloredBrushIcon(newColor));
				});
				cPicker.setVisible(true);
				cPicker.setOnClose(c -> {plugin.saveAll();});
			}
		});

		int actionBorder = 5;
		JPanel leftActions =  new JPanel();
		leftActions.setLayout(new BorderLayout(2,0));
		leftActions.setBorder(BorderFactory.createEmptyBorder(actionBorder,actionBorder,actionBorder,actionBorder));
		leftActions.add(expandToggle, BorderLayout.WEST);
		leftActions.add(visibilityToggle, BorderLayout.CENTER);
		leftActions.add(colorPicker, BorderLayout.EAST);
		int actionsWidth = (leftActions.getComponentCount() * (iconSize + 6));
		leftActions.setPreferredSize(new Dimension(actionsWidth, iconSize));

		JButton exportButton = PanelBuildUtils.createExportButton(iconSize, iconSize, plugin.gson, panelTypeText);
		exportButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				PanelBuildUtils.getExportButtonAction(plugin.gson, getGroupName(), groupToExportJson(plugin));
			}
		});

		JButton deleteButton = PanelBuildUtils.createDeleteButton(plugin, iconSize, iconSize, this, groupName, "group", true);


		JPanel rightActions = new JPanel();
		rightActions.setLayout(new BorderLayout(2, 0));
		rightActions.setBorder(BorderFactory.createEmptyBorder(actionBorder,actionBorder,actionBorder,actionBorder));
		rightActions.add(exportButton,  BorderLayout.WEST);
		rightActions.add(deleteButton,  BorderLayout.EAST);
		actionsWidth = (rightActions.getComponentCount() * (iconSize + 6));
		rightActions.setPreferredSize(new Dimension(actionsWidth, iconSize));

		JPanel topPanel = new JPanel(new BorderLayout(0, 0));
		topPanel.setPreferredSize(new Dimension(0, iconSize + actionBorder * 2));

		topPanel.add(leftActions, BorderLayout.WEST);
		topPanel.add(rightActions, BorderLayout.EAST);

		groupTextField.setPreferredSize(new Dimension(0, 20)); // PanelBuildUtils.PANEL_WIDTH - actionsWidth
		topPanel.add(groupTextField, BorderLayout.CENTER);

		setColor(group.color);

		add(topPanel);

		memberPanel.setLayout(new BoxLayout(memberPanel, BoxLayout.Y_AXIS));
		memberPanel.add(firstPathEntry);
		add(memberPanel);
	}

	JsonObject groupToExportJson(PathmakerPlugin plugin)
	{
		JsonObject groupJson = new JsonObject();
		JsonObject membersJson = new JsonObject();
		for (Component member : getPathPanels())
		{
			String pathName = ((PathPanel) member).getPathLabel();
			//log.debug("exporting member " + pathName);
			membersJson.add(pathName, plugin.pathToJson(pathName));
		}
		groupJson.add("members", membersJson);
		groupJson.add("expanded", plugin.gson.toJsonTree(group.expanded, boolean.class));
		groupJson.add("hidden", plugin.gson.toJsonTree(group.hidden, boolean.class));
		groupJson.add("color", plugin.gson.toJsonTree(group.color, Color.class));
		return groupJson;
	}

	void importGroupData(PathmakerPluginPanel.PathGroup group)
	{
		this.group = group;
		setExpanded(this.group.expanded);
		setHidden(this.group.hidden);
		setColor(this.group.color);
	}

	void setColor(Color newColor)
	{
		group.color = newColor;
		Color backgroundColor = getBackgroundColor();

		JPanel topPanel = (JPanel) groupTextField.getParent();

		for (Component member : topPanel.getComponents())
			member.setBackground(backgroundColor);

		colorPicker.setIcon(PanelBuildUtils.getRecoloredBrushIcon(this.group.color));

		topPanel.setBackground(backgroundColor);
		setBackground(newColor);
		setBorder(createDefaultBorder());
		repaint();
	}

	Color getBackgroundColor()
	{
		return group.color.darker().darker().darker();
	}

	void setHidden(boolean hidden)
	{
		group.hidden = hidden;
		PanelBuildUtils.getVisibilityAction(visibilityToggle, hidden, "group");

		for (Component panel : memberPanel.getComponents())
		{
			((PathPanel) panel).setVisibility(hidden);
		}
	}

	void setExpanded(boolean expanded)
	{
		group.expanded = expanded;
		PanelBuildUtils.getExpandToggleAction(memberPanel, expandToggle, expanded, "group");
	}

	void finalizeEditing()
	{
		groupTextField.getTextField().setEnabled(false);
		groupTextField.setBackground(getBackgroundColor());
	}

	Border createDefaultBorder()
	{
		return BorderFactory.createEmptyBorder(2, 2, 2, 2);
	}

	void addPathPanel(PathPanel panel)
	{
		memberPanel.add(panel);
		groupTextField.getTextField().setToolTipText(getGroupName() + ": " + memberPanel.getComponents().length + " paths");
	}

	Component[] getPathPanels()
	{
		return memberPanel.getComponents();
	}

	int getPathPanelCount()
	{
		return memberPanel.getComponentCount();
	}

	PathPanel getPathPanel(int index)
	{
		return (PathPanel) memberPanel.getComponent(index);
	}

	String getGroupName()
	{
		return groupTextField.getText();
	}
}
