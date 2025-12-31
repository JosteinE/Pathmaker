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
	int indexInParent;

	DropAdapter(PathmakerPlugin plugin, ArrayList<String> groupNames, JPanel parentPanel, JPanel panel, int indexInParent, @Nullable String panelLabel, int margin)
	{
		this.plugin = plugin;
		this.groupNames = groupNames;
		this.parentPanel = parentPanel;
		this.panel = panel;
		this.indexInParent = indexInParent;
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

		// Create group if hovering another path
		if (mouseOnBorder == 0 && panelLabel != null)
		{
			PathmakerPath draggedPath = plugin.getStoredPaths().get(panelLabel);

			if (targetPanel instanceof PathPanel)
			{
				String targetName = ((PathPanel) targetPanel).getPathLabel().getText();
				PathmakerPath targetPath = plugin.getStoredPaths().get(targetName);

				// Create a new group with target as the first member
				if (targetPath.pathGroup == null)
				{
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
				}
				else
				{
					// Return if already in the group
					if (targetPath.pathGroup.equals(draggedPath.pathGroup)) return;
					draggedPath.pathGroup = targetPath.pathGroup;
				}

				// Moving target path position below target panel.
				targetIndex = MouseAdapterUtils.getTrueIndexInView(parentPanel, targetIndex);
				targetIndex += indexInParent > targetIndex ? 1 : 0;

			}
			else // Add panel to existing group
			{
				// Set panel index to be bottom of the group
				draggedPath.pathGroup = ((PathGroup) targetPanel).getGroupName();

				log.debug("targetIndex: " + targetIndex);
				targetIndex = MouseAdapterUtils.getIndexInView(parentPanel, targetIndex) + ((PathGroup) targetPanel).getPathPanelCount();
				log.debug("trueTargetIndex: " + targetIndex);
				targetIndex -= indexInParent > targetIndex ? 0 : 1;
				log.debug("finalTargetIndex: " + targetIndex);
			}
		}
		// Correct target index for specific gap scenarios
		else if (mouseOnBorder == 1 && targetIndex > indexInParent)
		{
			targetIndex = MouseAdapterUtils.getTrueIndexInView(parentPanel,targetIndex -1);
		}
		else if (mouseOnBorder == -1 && targetIndex < indexInParent)
		{
			targetIndex = MouseAdapterUtils.getTrueIndexInView(parentPanel,targetIndex + 1);
		}
		else
		{
			targetIndex = MouseAdapterUtils.getTrueIndexInView(parentPanel, targetIndex);
		}

		LinkedHashMap<String, PathmakerPath> storedPaths = plugin.getStoredPaths();

		// Create new LinkedHashMap to replace the old plugin.paths map
		ArrayList<Map.Entry<String, PathmakerPath>> paths = new ArrayList<>(storedPaths.entrySet());

		// Move single path
		if (panelLabel != null)
		{
			Map.Entry<String, PathmakerPath> movedPath = paths.remove(indexInParent);
			if(targetIndex > paths.size() -1)
				paths.add(movedPath);
			else
				paths.add(targetIndex, movedPath);
		}
		else // Move group
		{
			int numPanels = ((PathGroup) panel).getPathPanelCount();
			int start = 	indexInParent > targetIndex ? numPanels - 1 : 0;
			int end = 		indexInParent > targetIndex ? -1 : numPanels;
			int itValue = 	indexInParent > targetIndex ? -1 : 1;

			log.debug("numPanels: " + numPanels + " start: " + start + " end: " + end + " itValue: " + itValue);

			for (int i = start; i != end; i+= itValue)
			{
				Map.Entry<String, PathmakerPath> movedGroupedPath = paths.remove(indexInParent + start);
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
