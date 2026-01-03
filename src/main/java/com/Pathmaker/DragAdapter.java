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
