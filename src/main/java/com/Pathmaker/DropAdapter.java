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
	ArrayList<String> groupNames;
	JPanel parentPanel;
	JPanel panel;
	@Nullable String panelLabel;
	int margin;
	int trueIndexInParent;

	DropAdapter(PathmakerPlugin plugin, ArrayList<String> groupNames, JPanel parentPanel, JPanel panel, int trueIndexInParent, @Nullable String panelLabel, int margin)
	{
		this.plugin = plugin;
		this.groupNames = groupNames;
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

		// DEBUGGING
		if (panel instanceof PathPanel)
		{
			if (targetPanel instanceof PathGroup)
			{
				log.debug("Panel ({}) is {}, TargetGroup ({}) is: {}", trueIndexInParent, ((PathPanel) panel).getPathLabel().getText(), targetIndex, ((PathGroup) targetPanel).getGroupName());
			}
			else
			{
				log.debug("Panel ({}) is {}, TargetPanel ({}) is: {}", trueIndexInParent, ((PathPanel) panel).getPathLabel().getText(), targetIndex, ((PathPanel) targetPanel).getPathLabel().getText());
			}
		}
		else if (panel instanceof PathGroup)
		{
			if (targetPanel instanceof PathGroup)
			{
				log.debug("Group ({}) is {}, TargetGroup ({}) is: {}", trueIndexInParent, ((PathGroup) panel).getGroupName(), targetIndex, ((PathGroup) targetPanel).getGroupName());
			}
			else
			{
				log.debug("Group ({}) is {}, TargetPanel ({}) is: {}", trueIndexInParent, ((PathGroup) panel).getGroupName(), targetIndex, ((PathPanel) targetPanel).getPathLabel().getText());
			}
		}

		// Mouse is on a PathPanel
		if (mouseOnBorder == 0)
		{
			// Skip if a group is dropped directly on top of another group or path
			if (panel instanceof PathGroup)
				return;

			PathmakerPath draggedPath = plugin.getStoredPaths().get(panelLabel);

			// Create group if dropped on another path
			if (targetPanel instanceof PathPanel)
			{
				String targetName = ((PathPanel) targetPanel).getPathLabel().getText();
				PathmakerPath targetPath = plugin.getStoredPaths().get(targetName);

				// Create a new group with target as the first member
				String newGroupName = "group 1";
				if (!groupNames.isEmpty())
				{
					int num = 1;
					while (groupNames.contains(newGroupName))
					{
						num++;
						newGroupName = "group " + num;
					}
				}
				targetPath.pathGroup = newGroupName;
				draggedPath.pathGroup = newGroupName;

				// Moving target path position below target panel.
				targetIndex += trueIndexInParent > targetIndex ? 1 : 0;

			}
			else // Add panel to existing group
			{
				// Skip if it already belongs to the group
				String groupName = ((PathGroup) targetPanel).getGroupName();
				if (groupName.equals(draggedPath.pathGroup))
					return;
				// Set panel index to be bottom of the group
				draggedPath.pathGroup = groupName;

				//log.debug("targetIndex: " + targetIndex);
				targetIndex += ((PathGroup) targetPanel).getPathPanelCount();
				//log.debug("trueTargetIndex: " + targetIndex);
				targetIndex -= trueIndexInParent >= targetIndex ? 0 : 1;
				//log.debug("finalTargetIndex: " + targetIndex);
			}
		}

		// Correct target index for specific gap scenarios
		else if (mouseOnBorder == 1 && targetIndex > trueIndexInParent)
		{
			targetIndex -= 1;
		}
		else if (mouseOnBorder == -1)
		{
			if (targetPanel instanceof PathGroup)
			{
				if(targetIndex < trueIndexInParent)
				{
					targetIndex += ((PathGroup) targetPanel).getPathPanelCount();
				}
				else
				{
					targetIndex += ((PathGroup) targetPanel).getPathPanelCount() - 1;
				}
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
			log.debug("INDEX = {}, TARGETINDEX = {}", compIndex , compTargetIndex);
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
			if(targetPanel instanceof PathPanel)
				log.debug("Moved ({}) {} to ({}) {}", trueIndexInParent, ((PathPanel) panel).getPathLabel().getText(), targetIndex, ((PathPanel) targetPanel).getPathLabel().getText());
			else
				log.debug("Moved ({}) {} to ({}) {}", trueIndexInParent, ((PathPanel) panel).getPathLabel().getText(), targetIndex, ((PathGroup) targetPanel).getGroupName());

			Map.Entry<String, PathmakerPath> movedPath = paths.remove(trueIndexInParent);
			if(targetIndex > paths.size() -1)
				paths.add(movedPath);
			else
				paths.add(targetIndex, movedPath);
		}
		else // Move group
		{
			//targetIndex += targetPanel instanceof PathGroup && trueIndexInParent < targetIndex ? ((PathGroup) targetPanel).getPathPanelCount() - 1 : 0;
			int numPanels = ((PathGroup) panel).getPathPanelCount();
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
}
