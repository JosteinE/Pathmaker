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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.util.ImageUtil;

public class PanelBuildUtils
{
	private static final ImageIcon EXPORT_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "export.png"));
	private static final BufferedImage BRUSH_IMAGE = ImageUtil.loadImageResource(PathmakerPlugin.class, "brush.png");
	private static final ImageIcon EXPAND_ICON;
	private static final ImageIcon COLLAPSE_ICON;
	//	private static final ImageIcon LOOP_ON_ICON;
//	private static final ImageIcon LOOP_OFF_ICON;
	private static final ImageIcon EYE_OPEN_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "eye_open.png"));
	private static final ImageIcon EYE_CLOSED_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "eye_closed.png"));
//	private static final ImageIcon OFFSET_LEFT_ICON;
//	private static final ImageIcon OFFSET_MIDDLE_ICON;
//	private static final ImageIcon OFFSET_RIGHT_ICON;
//	private static final ImageIcon PERSON_ICON;
//	private static final ImageIcon PERSON_GREEN_ICON;
//	private static final ImageIcon PERSON_GREEN_LINES_ICON;

	static final int PANEL_WIDTH = 225; // Client.PANEL_WIDTH = 225
	static final int ICON_SIZE = 18;

	static
	{
		BufferedImage upArrowImage = ImageUtil.loadImageResource(PathmakerPlugin.class, "up_arrow.png");
		COLLAPSE_ICON = new ImageIcon(upArrowImage);
		EXPAND_ICON = new ImageIcon(ImageUtil.flipImage(upArrowImage, false, true));
	}

	// EXPAND

	static JButton createExpandToggleButton(JPanel expandPanel, boolean expanded, int width, int height, String panelTypeText)
	{
		width = width < 0 ? ICON_SIZE : width;
		height = height < 0 ? ICON_SIZE : height;

		JButton expandToggle = new JButton(expanded ? COLLAPSE_ICON : EXPAND_ICON);
		expandToggle.setPreferredSize(new Dimension(width, height));
		expandToggle.setToolTipText((expanded ? "Collapse" : "Expand") + " " + panelTypeText);

		return expandToggle;
	}

	static void getExpandToggleAction(JPanel panel, JButton expandToggleButton, boolean expanded, String panelTypeText)
	{
		for (int i = 0; i < panel.getComponentCount(); i++)
		{
			panel.getComponent(i).setVisible(expanded);
		}

		expandToggleButton.setIcon(expanded ? COLLAPSE_ICON : EXPAND_ICON);
		expandToggleButton.setToolTipText((expanded ? "Collapse" : "Expand") + " " + panelTypeText);
	}

	static void toggleExpandPath(PathmakerPath path, JPanel pathContainer, JButton expandToggle)
	{
		path.panelExpanded = !path.panelExpanded;
		getExpandToggleAction(pathContainer, expandToggle, path.panelExpanded, "path");
	}

	// VISIBILITY

	static JButton createVisibilityToggleButton(boolean hidden, int width, int height, String panelTypeText)
	{
		width = width < 0 ? ICON_SIZE : width;
		height = height < 0 ? ICON_SIZE : height;

		JButton visibilityToggle = new JButton(hidden ? EYE_CLOSED_ICON : EYE_OPEN_ICON);
		visibilityToggle.setPreferredSize(new Dimension(width, height));
		visibilityToggle.setToolTipText((hidden ? "Show" : "Hide") + " " + panelTypeText);
//		visibilityToggle.addActionListener(actionEvent ->
//		{
//			//path.hidden = !path.hidden;
//			getVisibilityAction(visibilityToggle, hidden, panelTypeText);
//		});
		return visibilityToggle;
	}

	static void toggleVisibilityPath(PathmakerPath path, JButton visibilityToggle)
	{
		path.hidden = !path.hidden;
		getVisibilityAction(visibilityToggle, path.hidden, "path");
	}

	static void getVisibilityAction(JButton visibilityToggle, boolean hidden, String panelTypeText)
	{
		visibilityToggle.setIcon(hidden ? EYE_CLOSED_ICON : EYE_OPEN_ICON);
		visibilityToggle.setToolTipText((hidden ? "Show" : "Hide") + " " + panelTypeText);
	}

	// COLOR PICKER

	static JButton createColorPickerButton(int width, int height, Color currentColor, String panelTypeText)
	{
		width = width < 0 ? ICON_SIZE : width;
		height = height < 0 ? ICON_SIZE : height;

		JButton colorPickerButton = new JButton();
		colorPickerButton.setPreferredSize(new Dimension(width, height));
		colorPickerButton.setIcon(new ImageIcon(ImageUtil.recolorImage(BRUSH_IMAGE, currentColor)));
		colorPickerButton.setToolTipText("Pick " + panelTypeText + " color");
		return colorPickerButton;
	}

	static RuneliteColorPicker getColorPicker(ColorPickerManager colorPickerManager, Color color, JPanel owner, Component relativeTo, String panelTypeText)
	{
		RuneliteColorPicker colorPicker = colorPickerManager.create(
			SwingUtilities.windowForComponent(owner), color, panelTypeText + " color",true);
		colorPicker.setLocationRelativeTo(relativeTo);
		return colorPicker;
	}

	static ImageIcon getRecoloredBrushIcon(Color color)
	{
		return new ImageIcon(ImageUtil.recolorImage(PanelBuildUtils.BRUSH_IMAGE, color));
	}

	// EXPORT

	static JButton createExportButton(int width, int height, Gson gson, String panelTypeText)
	{
		JButton exportButton = new JButton();
		exportButton.setIcon(EXPORT_ICON);
		exportButton.setToolTipText("Export " + panelTypeText + " to clipboard");
		exportButton.setPreferredSize(new Dimension(width, height));
		return exportButton;
	}

	static void getExportButtonAction(Gson gson, String exportName, JsonObject object)
	{
		JsonObject exportPath = new JsonObject();
		exportPath.add(exportName, object);

		StringSelection json = new StringSelection(gson.toJson(exportPath));
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(json, null);
	}
}
