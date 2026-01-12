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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DropAdapter extends MouseAdapter
{
	PathmakerPlugin plugin;
	JPanel parentPanel;
	JPanel panel;
	@Nullable String panelLabel;
	int margin;
	int trueIndexInParent;

	@Nullable String availableGroupName = null;

	DropAdapter(PathmakerPlugin plugin, JPanel parentPanel, JPanel panel, int trueIndexInParent, @Nullable String panelLabel, int margin)
	{
		this.plugin = plugin;
		this.parentPanel = parentPanel;
		this.panel = panel;
		this.trueIndexInParent = trueIndexInParent;
		this.panelLabel = panelLabel;
		this.margin = margin;
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		panel.setBorder(BorderFactory.createEmptyBorder());
		panel.repaint();

		int targetIndex = MouseAdapterUtils.getHoveredPathIndex(e, parentPanel);
		JPanel targetPanel = MouseAdapterUtils.getHoveredPathPanel(parentPanel, targetIndex);
		int mouseOnBorder = MouseAdapterUtils.isMouseHoveringPathBorder(e, parentPanel, targetPanel, margin);

		targetIndex = MouseAdapterUtils.getTrueIndexInView(parentPanel, targetIndex);

		// DEBUG
		//printDebug(targetPanel, targetIndex);

		// Mouse is on a PathPanel
		if (mouseOnBorder == 0)
		{
			// Skip if a group is dropped directly on top of another group or path
			if (panel instanceof GroupPanel)
				return;

			PathmakerPath draggedPath = plugin.getStoredPaths().get(panelLabel);

			// Create group if dropped on another path
			if (targetPanel instanceof PathPanel)
			{
				String targetName = ((PathPanel) targetPanel).getPathLabel();
				PathmakerPath targetPath = plugin.getStoredPaths().get(targetName);

				// Create a new group with target as the first member
				if(availableGroupName == null)
				{
					log.debug("Available group name is null");
				}
				targetPath.pathGroup = availableGroupName;
				draggedPath.pathGroup = availableGroupName;

				// Moving target path position below target panel.
				targetIndex += trueIndexInParent > targetIndex ? 1 : 0;

			}
			else // Add panel to existing group
			{
				// Skip if it already belongs to the group
				String groupName = ((GroupPanel) targetPanel).getGroupName();
				if (groupName.equals(draggedPath.pathGroup))
					return;
				// Set panel index to be bottom of the group
				draggedPath.pathGroup = groupName;
				targetIndex += ((GroupPanel) targetPanel).getPathPanelCount();
				targetIndex -= trueIndexInParent >= targetIndex ? 0 : 1;
			}
		}

		// Correct target index for specific gap scenarios
		else if (mouseOnBorder == 1 && targetIndex > trueIndexInParent)
		{
			targetIndex -= 1;
		}
		else if (mouseOnBorder == -1)
		{
			if (targetPanel instanceof GroupPanel)
			{
				targetIndex += ((GroupPanel) targetPanel).getPathPanelCount();
				targetIndex -= trueIndexInParent >= targetIndex ? 0 : 1;
			}
			else if (targetIndex < trueIndexInParent)
			{
				targetIndex += 1;
			}
		}

		// Remove from existing group if path is dropped on a gap directly next to itself.
		if ((mouseOnBorder == -1 || mouseOnBorder == 1) && panel instanceof PathPanel)
		{
			int compIndex = MouseAdapterUtils.getIndexInView(parentPanel, trueIndexInParent);
			int compTargetIndex = MouseAdapterUtils.getIndexInView(parentPanel, targetIndex);

			if (compTargetIndex == compIndex)
			{
				plugin.getStoredPaths().get(panelLabel).pathGroup = null;
			}
		}

		LinkedHashMap<String, PathmakerPath> storedPaths = plugin.getStoredPaths();

		// Create new LinkedHashMap to replace the old plugin.paths map
		ArrayList<Map.Entry<String, PathmakerPath>> paths = new ArrayList<>(storedPaths.entrySet());

		// Move single path
		if (panelLabel != null)
		{
			//debugMoveTo(targetPanel, targetIndex);
			Map.Entry<String, PathmakerPath> movedPath = paths.remove(trueIndexInParent);
			if(targetIndex > paths.size() -1)
				paths.add(movedPath);
			else
				paths.add(targetIndex, movedPath);
		}
		else // Move group
		{
			//targetIndex += targetPanel instanceof PathGroup && trueIndexInParent < targetIndex ? ((PathGroup) targetPanel).getPathPanelCount() - 1 : 0;
			int numPanels = ((GroupPanel) panel).getPathPanelCount();
			int start = 	trueIndexInParent > targetIndex ? numPanels - 1 : 0;
			int end = 		trueIndexInParent > targetIndex ? -1 : numPanels;
			int itValue = 	trueIndexInParent > targetIndex ? -1 : 1;

			//log.debug("numPanels: " + numPanels + " start: " + start + " end: " + end + " itValue: " + itValue);

			for (int i = start; i != end; i+= itValue)
			{
				Map.Entry<String, PathmakerPath> movedGroupedPath = paths.remove(trueIndexInParent + start);
				if(targetIndex > paths.size() -1)
					paths.add(movedGroupedPath);
				else
					paths.add(targetIndex, movedGroupedPath);
			}
		}

		// Reorder paths
		storedPaths.clear();
		for (Map.Entry<String, PathmakerPath> path : paths)
		{
			storedPaths.put(path.getKey(), path.getValue());
		}

		plugin.rebuildPanel(true);
	}

	void debugThisAndTarget(JPanel targetPanel, int targetIndex)
	{
		if (panel instanceof PathPanel)
		{
			if (targetPanel instanceof GroupPanel)
			{
				log.debug("Panel ({}) is {}, TargetGroup ({}) is: {}", trueIndexInParent, ((PathPanel) panel).getPathLabel(), targetIndex, ((GroupPanel) targetPanel).getGroupName());
			}
			else
			{
				log.debug("Panel ({}) is {}, TargetPanel ({}) is: {}", trueIndexInParent, ((PathPanel) panel).getPathLabel(), targetIndex, ((PathPanel) targetPanel).getPathLabel());
			}
		}
		else if (panel instanceof GroupPanel)
		{
			if (targetPanel instanceof GroupPanel)
			{
				log.debug("Group ({}) is {}, TargetGroup ({}) is: {}", trueIndexInParent, ((GroupPanel) panel).getGroupName(), targetIndex, ((GroupPanel) targetPanel).getGroupName());
			}
			else
			{
				log.debug("Group ({}) is {}, TargetPanel ({}) is: {}", trueIndexInParent, ((GroupPanel) panel).getGroupName(), targetIndex, ((PathPanel) targetPanel).getPathLabel());
			}
		}
	}

	void debugMoveTo(JPanel targetPanel, int targetIndex)
	{
		if(targetPanel instanceof PathPanel)
			log.debug("Moved ({}) {} to ({}) {}", trueIndexInParent, ((PathPanel) panel).getPathLabel(), targetIndex, ((PathPanel) targetPanel).getPathLabel());
		else
			log.debug("Moved ({}) {} to ({}) {}", trueIndexInParent, ((PathPanel) panel).getPathLabel(), targetIndex, ((GroupPanel) targetPanel).getGroupName());
	}

	// Injected on drop just before mouse release if panel is PathPanel
	void setAvailableGroupName(@Nullable String availableGroupName)
	{
		this.availableGroupName = availableGroupName;
	}
}
