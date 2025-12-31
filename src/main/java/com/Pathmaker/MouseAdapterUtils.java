package com.Pathmaker;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;

public class MouseAdapterUtils
{
	// 1 = top, -1 = bottom, 0 = false
	public static int isMouseHoveringPathBorder(MouseEvent e, JPanel listPanel, JPanel targetPanel, int margin)
	{
		int entryPosY = (int) (e.getYOnScreen() - listPanel.getLocationOnScreen().getY());
		int minY = (int) targetPanel.getBounds().getMinY();
		int maxY = (int) targetPanel.getBounds().getMaxY();

		if (entryPosY < minY + margin) return 1;
		if (entryPosY > maxY - margin) return -1;
		return 0;
	}

	public static void setPanelInnerBorderColor(JComponent panel, Color color)
	{
		Border outerBorder, innerBorder;
		outerBorder = BorderFactory.createEmptyBorder();
		innerBorder = BorderFactory.createLineBorder(color, 2);
		panel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
	}

	public static void createGapBorders(JPanel topPanel, JPanel bottomPanel, Color color)
	{
		Border outerBorder, innerBorder;

		// Only drawing inner borders, so leaving outer empty.
		outerBorder = BorderFactory.createEmptyBorder();
		if (topPanel != null)
		{
			innerBorder = BorderFactory.createMatteBorder(0, 0, 2, 0, color);
			topPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
		}

		if (bottomPanel != null)
		{
			innerBorder = BorderFactory.createMatteBorder(2, 0, 0, 0, color);
			bottomPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
		}
	}

	static int getHoveredPathIndex(MouseEvent e, JPanel listPanel)
	{
		int hoverY = (int) (e.getYOnScreen() - listPanel.getLocationOnScreen().getY());

		// If the mouse Y position is above or below the panel, then return the first or last index
		if (hoverY < listPanel.getComponent(0).getBounds().getMinY())
			return 0;
		if (hoverY > listPanel.getComponent(listPanel.getComponentCount() - 1).getBounds().getMaxY())
			return listPanel.getComponentCount() - 1;

		for (int i = 0; i < listPanel.getComponentCount(); i++)
		{
			Component comp = listPanel.getComponent(i);
			int minY = (int) comp.getBounds().getMinY();
			int maxY = (int) comp.getBounds().getMaxY();

			if(isMouseHoveringPath(hoverY, minY, maxY))
			{
				return i;
			}
		}
		return -1;
	}

	static JPanel getHoveredPathPanel(JPanel listPanel, int index)
	{
		return (JPanel) listPanel.getComponent(index);
	}

	static boolean isMouseHoveringPath(int mouseY, int panelMinY, int panelMaxY)
	{
		return mouseY >= panelMinY && mouseY < panelMaxY;
	}

	static boolean isIndexValidDropTarget(int index, int targetIndex)
	{
		return targetIndex != -1 && index != targetIndex;
	}

	// Returns the actual path index, also counting grouped paths.
	// If viewIndex is a group, then the true index is trueIndex + comp index
	static int getTrueIndexInView(JPanel parentPanel, int viewIndex)
	{
		int trueIndex = 0;
		for (int i = 0; i < parentPanel.getComponentCount(); i++)
		{
			Component comp = parentPanel.getComponent(i);

			if(i == viewIndex)
			{
				return trueIndex;
			}

			if (comp instanceof PathGroup)
			{
				trueIndex += ((PathGroup)comp).getPathPanelCount();
			}
			else
			{
				trueIndex++;
			}
		}
		return trueIndex;
	}

	// Return top layer component index of view (either group if in group, else path)
	static int getIndexInView(JPanel parentPanel, int trueIndex)
	{
		int index = 0;
		for (int i = 0; i < parentPanel.getComponentCount(); i++)
		{
			Component comp = parentPanel.getComponent(i);

			if (comp instanceof PathGroup)
			{
				if (trueIndex >= index && trueIndex < index + ((PathGroup) comp).getPathPanelCount())
				{
					return i;
				}
				index += ((PathGroup)comp).getPathPanelCount();
			}
			else if (index == trueIndex)
			{
				return i;
			}
			else
			{
				index++;
			}
		}
		return trueIndex;
	}
}
