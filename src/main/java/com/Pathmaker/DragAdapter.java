package com.Pathmaker;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DragAdapter extends MouseMotionAdapter
{
	JPanel parentPanel;
	JPanel panel;
	boolean isGroupPanel;
	int panelMargin;

	DragAdapter(JPanel parentPanel, JPanel panel, int panelMargin)
	{
		this.parentPanel = parentPanel;
		this.panel = panel;
		this.panelMargin = panelMargin;
	}

	@Override
	public void mouseDragged(MouseEvent e)
	{
		super.mouseDragged(e);
		int panelIndex = -1;
		int pathCounter = 0;

		// Reset borders and find panelIndex
		for (int i = 0; i < parentPanel.getComponentCount(); i++)
		{
			JPanel p = ((JPanel) parentPanel.getComponents()[i]);

			if(p instanceof PathGroup)
				p.setBorder(((PathGroup) p).createDefaultBorder());
			else
				p.setBorder(BorderFactory.createEmptyBorder());

			if (p == panel)
			{
				panelIndex = pathCounter;
			}
			else if (p instanceof PathGroup)
			{
				int groupedPathCount = ((PathGroup) p).getPathPanelCount();
				for (int ii = 0; ii < groupedPathCount; ii++)
				{
					if (((PathGroup) p).getPathPanel(ii) == panel)
					{
						panelIndex = pathCounter;
					}
				}
				pathCounter += groupedPathCount;
			}
			else
			{
				pathCounter++;
			}
		}

		// Set the dragged colour
		MouseAdapterUtils.setPanelInnerBorderColor(panel, Color.RED);
		parentPanel.repaint();

		int targetIndex = MouseAdapterUtils.getHoveredPathIndex(e, parentPanel);
		//log.debug("panelIndex: " + panelIndex + " targetIndex: " + targetIndex);
		if (!MouseAdapterUtils.isIndexValidDropTarget(panelIndex, MouseAdapterUtils.getTrueIndexInView(parentPanel, targetIndex))) return;

		// Is mouse on the path border?
		JPanel targetPanel = (JPanel) parentPanel.getComponent(targetIndex);
		int mouseOnBorder = MouseAdapterUtils.isMouseHoveringPathBorder(e, parentPanel, targetPanel, panelMargin);

		// Set the hovered path color
		if (mouseOnBorder == 1)
		{
			JPanel topPanel = targetIndex > 0 ? (JPanel) parentPanel.getComponent(targetIndex - 1) : null;
			MouseAdapterUtils.createGapBorders(topPanel, targetPanel, Color.GREEN);
		}
		else if (mouseOnBorder == -1)
		{
			JPanel bottomPanel = targetIndex < parentPanel.getComponentCount() - 1 ? (JPanel) parentPanel.getComponent(targetIndex + 1) : null;
			MouseAdapterUtils.createGapBorders(targetPanel, bottomPanel, Color.GREEN);
		}
		// Not allowing group placement directly on other pathView Entries.
		else if (!(panel instanceof PathGroup))
			MouseAdapterUtils.setPanelInnerBorderColor(targetPanel, Color.GREEN);
		targetPanel.repaint();
	}
}
