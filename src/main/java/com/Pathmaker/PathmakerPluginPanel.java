package com.Pathmaker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.ImageUtil;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;

@Slf4j
public class PathmakerPluginPanel extends PluginPanel
{
	private static final ImageIcon IMPORT_ICON;
	private static final ImageIcon EXPORT_ICON;

	private final PluginErrorPanel noPathPanel = new PluginErrorPanel();
	private final JPanel pathView = new JPanel();
	HashMap<String, PathGroup> pathGroups = new HashMap<>();

	Client client;
	PathmakerPlugin plugin;

	FlatTextField activePath;
	final int MAX_PATH_NAME_LENGTH = 20;
	final int DRAG_DROP_Y_MARGIN = 10;

	static
	{
		IMPORT_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "import.png"));
		EXPORT_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "export.png"));
	}

	// Inner PathGroup Class
	class PathGroup extends JPanel
	{
		FlatTextField groupTextField = new FlatTextField();
		JPanel memberPanel = new JPanel();
		int viewPanelIndex;

		PathGroup(PathPanel firstPathEntry, String groupName, int viewPanelIndex)
		{
			JPanel groupPanel = this;
			this.viewPanelIndex = viewPanelIndex;

			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			setBackground(Color.BLUE);

			groupTextField.setText(groupName);
			groupTextField.getTextField().setEnabled(false);
			groupTextField.setBackground(Color.BLUE);

			// Add drag and drop
			groupTextField.getTextField().addMouseMotionListener(dragAdapter(groupPanel, true));
			groupTextField.getTextField().addMouseListener(dropAdapter(groupPanel, viewPanelIndex, null));

			groupTextField.getTextField().addFocusListener(new FocusAdapter()
			{
				@Override
				public void focusLost(FocusEvent e)
				{
					finalizeEditing();
				}
			});
			groupTextField.getTextField().addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					super.mouseClicked(e);
					groupTextField.getTextField().setEnabled(true);
					groupTextField.requestFocusInWindow();
					groupTextField.getTextField().selectAll();
					groupTextField.setBackground(Color.DARK_GRAY);
				}
			});
			groupTextField.getTextField().addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyPressed(KeyEvent e)
				{
					super.keyPressed(e);
					if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE)
					{
						finalizeEditing();
					}
				}
			});
			add(groupTextField);

			memberPanel.setLayout(new BoxLayout(memberPanel, BoxLayout.Y_AXIS));
			memberPanel.setBackground(Color.BLUE);
			memberPanel.add(firstPathEntry);
			add(memberPanel);
		}

		private void finalizeEditing()
		{
			groupTextField.getTextField().setEnabled(false);
			groupTextField.setBackground(Color.BLUE);
		}

		void addPathPanel(PathPanel panel)
		{
			memberPanel.add(panel);
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

	PathmakerPluginPanel(Client client, PathmakerPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;

		// Define standard client panel layout
		setLayout(new BorderLayout());
		//setBorder(new EmptyBorder(10, 10, 10, 10));

		// Create title panel
		JPanel titlePanel = new JPanel();
		//titlePanel.setBorder(new EmptyBorder(1, 0, 10, 0));

		// Create label and add to title panel
		JLabel title = new JLabel();
		title.setText("Pathmaker");
		title.setPreferredSize(new Dimension(80, 20)); //getGraphics().getFontMetrics().stringWidth(title.getText()) + 10
		title.setForeground(Color.WHITE);
		title.setToolTipText("by Fraph");
		titlePanel.add(title, BorderLayout.CENTER);

		// EXPORT / IMPORT
		JButton exportButton = new JButton();
		exportButton.setIcon(EXPORT_ICON);
		exportButton.setToolTipText("Export active path to clipboard");
		exportButton.setPreferredSize(new Dimension(18, 18));
		exportButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				if (!plugin.getStoredPaths().containsKey(activePath.getText()))
				{
					return;
				}

				//new Pair<String, PathmakerPath>(activePath.getText(), plugin.getStoredPaths().get(activePath.getText()));
				JsonObject exportPath = new JsonObject();
				exportPath.add(activePath.getText(), plugin.pathToJson(activePath.getText()));

				StringSelection json = new StringSelection(plugin.gson.toJson(exportPath));
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(json, null);
			}
		});

		JButton importButton = new JButton();
		importButton.setIcon(IMPORT_ICON);
		importButton.setToolTipText("Import path from clipboard");
		importButton.setPreferredSize(new Dimension(18, 18));
		importButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				String json = "";
				try
				{
					json = (String) getToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
				}
				catch (IllegalArgumentException | UnsupportedFlavorException | IllegalStateException |
					   NullPointerException | IOException ex)
				{
					log.debug("Clipboard is unavailable or has invalid content");
				}

				if (json.isEmpty())
				{
					return;
				}

				JsonObject object;
				try
				{
					JsonParser parser = new JsonParser();
					JsonElement element = parser.parse(json);
					if (element == null || !element.isJsonObject())
					{
						log.debug("Imported element is not a JsonObject");
						return;
					}

					object = element.getAsJsonObject();
				}
				catch (Exception e)
				{
					log.debug("String was not a valid JSON");
					return;
				}

				String jsonPathName = object.keySet().iterator().next();

				JLabel centeredNameText = new JLabel("Path name", JLabel.CENTER);
				centeredNameText.setHorizontalTextPosition(SwingConstants.CENTER);
				//String pathName = JOptionPane.showInputDialog(importButton, centeredNameText, loadedPath.getKey());
				String inputPathName = JOptionPane.showInputDialog(importButton, centeredNameText, jsonPathName);

				// Return if the window was cancelled or closed
				if (inputPathName == null)
				{
					return;
				}

				inputPathName = inputPathName.length() > MAX_PATH_NAME_LENGTH ?
					inputPathName.substring(0, MAX_PATH_NAME_LENGTH) : inputPathName;

				// Show warning if imported path exists
				if (plugin.getStoredPaths().containsKey(inputPathName))
				{
					JLabel centeredWarningText = new JLabel("The path name " + inputPathName + " already exist.", JLabel.CENTER);
					JLabel centeredReplaceText = new JLabel("Replace it?", JLabel.CENTER);

					centeredWarningText.setHorizontalTextPosition(SwingConstants.CENTER);
					centeredReplaceText.setHorizontalTextPosition(SwingConstants.CENTER);

					JPanel centeredTextFrame = new JPanel(new GridLayout(0, 1));
					centeredTextFrame.setAlignmentX(Component.CENTER_ALIGNMENT);
					centeredTextFrame.add(centeredWarningText);
					centeredTextFrame.add(centeredReplaceText);

					int confirm = JOptionPane.showConfirmDialog(null,
						centeredTextFrame, "Warning", JOptionPane.YES_NO_OPTION);

					if (confirm == JOptionPane.YES_OPTION)// || confirm == JOptionPane.CLOSED_OPTION)
					{
						plugin.removePath(inputPathName);
						plugin.loadPathFromJson(object.get(jsonPathName).getAsJsonObject(), inputPathName);
						plugin.rebuildPanel(true);
						activePath.setText(inputPathName);
					}
				}
				else
				{
					plugin.loadPathFromJson(object.get(jsonPathName).getAsJsonObject(), inputPathName);
					plugin.rebuildPanel(true);
					activePath.setText(inputPathName);
				}
			}
		});

		JPanel rightActionTitlePanel = new JPanel();
		rightActionTitlePanel.add(importButton, BorderLayout.WEST);
		rightActionTitlePanel.add(exportButton, BorderLayout.EAST);
		titlePanel.add(rightActionTitlePanel, BorderLayout.EAST);

		// Create body panel and add titlePanel
		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.setBorder(new EmptyBorder(1, 0, 10, 0));
		northPanel.add(titlePanel, BorderLayout.NORTH);

		// Add Active Path text field
		JLabel activePathLabel = new JLabel("Active Path: ");
		activePathLabel.setPreferredSize(new Dimension(75, 20));
		activePathLabel.setForeground(Color.WHITE);
		northPanel.add(activePathLabel, BorderLayout.WEST);

		activePath = new FlatTextField();
		((AbstractDocument) activePath.getDocument()).setDocumentFilter(new MaxLengthFilter(MAX_PATH_NAME_LENGTH));
		activePath.setText("unnamed");
		activePath.setForeground(Color.WHITE);
		activePath.setBackground(Color.DARK_GRAY);

		northPanel.add(activePath);

		// Add panel to client panel
		add(northPanel, BorderLayout.NORTH);

		// Configure path view panel
		pathView.setLayout(new BoxLayout(pathView, BoxLayout.Y_AXIS));

		// Configure PluginErrorPanel
		noPathPanel.setVisible(false);
		//noPathPanel.setPreferredSize(new Dimension(50, 30));
		noPathPanel.setContent("No stored paths", "Shift right-click a tile to add a path point.");

		// Add body panel
		JPanel centerPanel = new JPanel(new BorderLayout());
		//centerPanel.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH-5, 0));
		centerPanel.add(noPathPanel, BorderLayout.NORTH);

		centerPanel.add(pathView, BorderLayout.CENTER);

		add(centerPanel, BorderLayout.CENTER);

		rebuild();
	}

	void rebuild()
	{
		pathView.removeAll();
		pathGroups.clear();

		for (final String pathLabel : plugin.getStoredPaths().keySet())
		{
			// Create new path entry
			int entryIndex = pathView.getComponentCount();

			PathPanel pathEntry = new PathPanel(plugin, pathLabel);

			// Set as active path on label click
			pathEntry.getPathLabel().addActionListener(actionEvent ->
			{
				activePath.setText(pathEntry.getPathLabel().getText());
			});

			pathEntry.getPathLabel().addMouseListener(dropAdapter(pathEntry, entryIndex, pathLabel));
			pathEntry.getPathLabel().addMouseMotionListener(dragAdapter(pathEntry, false));

			String groupName = plugin.getStoredPaths().get(pathLabel).pathGroup;
			if (groupName == null)
			{
				pathView.add(pathEntry);
			}
			else
			{
				if (pathGroups.containsKey(groupName))
				{
					pathGroups.get(groupName).addPathPanel(pathEntry);
				}
				else
				{
					PathGroup group = new PathGroup(pathEntry, groupName, entryIndex);

					pathGroups.put(groupName, group);
					pathView.add(group);
				}
			}
		}

		boolean empty = pathView.getComponentCount() == 0;
		noPathPanel.setVisible(empty);

		repaint();
		revalidate();
	}

	// ------------- Helpers -------------

	void setPanelInnerBorderColor(JComponent pathPanel, Color color)
	{
		Border outerBorder, innerBorder;
		outerBorder = BorderFactory.createEmptyBorder();
		innerBorder = BorderFactory.createLineBorder(color, 2);
		pathPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
	}

	void createGapBorders(JPanel topPathPanel, JPanel bottomPathPanel, Color color)
	{
		Border outerBorder, innerBorder;

		// Only drawing inner borders, so leaving outer empty.
		outerBorder = BorderFactory.createEmptyBorder();
		if (topPathPanel != null)
		{
			innerBorder = BorderFactory.createMatteBorder(0, 0, 2, 0, color);
			topPathPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
		}

		if (bottomPathPanel != null)
		{
			innerBorder = BorderFactory.createMatteBorder(2, 0, 0, 0, color);
			bottomPathPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
		}
	}

	// 1 = top, -1 = bottom, 0 = false
	int isMouseHoveringPathBorder(MouseEvent e, JPanel listPanel, JPanel targetPanel)
	{
		int entryPosY = (int) (e.getYOnScreen() - listPanel.getLocationOnScreen().getY());
		int minY = (int) targetPanel.getBounds().getMinY();
		int maxY = (int) targetPanel.getBounds().getMaxY();

		if (entryPosY < minY + DRAG_DROP_Y_MARGIN) return 1;
		if (entryPosY > maxY - DRAG_DROP_Y_MARGIN) return -1;
		return 0;
	}

	int getHoveredPathIndex(MouseEvent e, JPanel listPanel)
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

	// Returns the actual path index, also counting grouped paths.
	// If viewIndex is a group, then the true index is trueIndex + comp index
	int getTrueIndexInView(int viewIndex)
	{
		int trueIndex = 0;
		for (int i = 0; i < pathView.getComponentCount(); i++)
		{
			Component comp = pathView.getComponent(i);

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

	boolean isMouseHoveringPath(int mouseY, int panelMinY, int panelMaxY)
	{
		return mouseY >= panelMinY && mouseY < panelMaxY;
	}

	boolean isIndexValidDropTarget(int index, int targetIndex)
	{
		return index != -1 && index != targetIndex;
	}

	JPanel getHoveredPathPanel(JPanel listPanel, int index)
	{
		return (JPanel) listPanel.getComponent(index);
	}

	private MouseMotionAdapter dragAdapter(JComponent panel, boolean isGroupPanel)
	{
		return new MouseMotionAdapter()
		{
			@Override
			public void mouseDragged(MouseEvent e)
			{
				super.mouseDragged(e);
				int panelIndex = -1;
				int pathCounter = 0;
				// Reset borders and find panelIndex
				for (int i = 0; i < pathView.getComponentCount(); i++)
				{
					JPanel p = ((JPanel) pathView.getComponents()[i]);
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
						pathCounter++;
				}

				// Set the dragged colour
				setPanelInnerBorderColor(panel, Color.RED);
				pathView.repaint();

				int targetIndex = getHoveredPathIndex(e, pathView);
				log.debug("panelIndex: " + panelIndex + " targetIndex: " + targetIndex);
				if (!isIndexValidDropTarget(panelIndex, targetIndex)) return;

				// Is mouse on the path border?
				JPanel targetPanel = (JPanel) pathView.getComponent(targetIndex);
				int mouseOnBorder = isMouseHoveringPathBorder(e, pathView, targetPanel);

				// Set the hovered path color
				if (mouseOnBorder == 1)
				{
					JPanel topPanel = targetIndex > 0 ? (JPanel) pathView.getComponent(targetIndex - 1) : null;
					createGapBorders(topPanel, targetPanel, Color.GREEN);
				}
				else if (mouseOnBorder == -1)
				{
					JPanel bottomPanel = targetIndex < pathView.getComponentCount() - 1 ? (JPanel) pathView.getComponent(targetIndex + 1) : null;
					createGapBorders(targetPanel, bottomPanel, Color.GREEN);
				}
				// Not allowing group placement directly on other pathView Entries.
				else if (!isGroupPanel)
					setPanelInnerBorderColor(targetPanel, Color.GREEN);
				targetPanel.repaint();
			}
		};
	}

	private MouseAdapter dropAdapter(JComponent panel, int pathViewIndex, @Nullable String pathLabel)
	{
		return new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				panel.setBorder(BorderFactory.createEmptyBorder());
				panel.repaint();

				int targetIndex = getHoveredPathIndex(e, pathView);
				JPanel targetPanel = getHoveredPathPanel(pathView, targetIndex);

				int mouseOnBorder = isMouseHoveringPathBorder(e, pathView, targetPanel);

				// Add group if hovering another path
				if (mouseOnBorder == 0 && pathLabel != null)
				{
					PathmakerPath draggedPath = plugin.getStoredPaths().get(pathLabel);

					if (targetPanel instanceof PathPanel)
					{
						String targetName = ((PathPanel) targetPanel).getPathLabel().getText();
						PathmakerPath targetPath = plugin.getStoredPaths().get(targetName);

						// Create a new group with target as the first member
						if (targetPath.pathGroup == null)
						{
							String newGroupName = "group 1";
							if (!pathGroups.isEmpty())
							{
								int num = 1;
								while (pathGroups.containsKey(newGroupName))
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
							draggedPath.pathGroup = targetPath.pathGroup;
						}

						// Moving target path position below target panel.
						targetIndex = getTrueIndexInView(targetIndex);
						targetIndex += pathViewIndex > targetIndex ? 1 : 0;

					}
					else // Add panel to existing group
					{
						// Set panel index to be bottom of the group
						draggedPath.pathGroup = ((PathGroup) targetPanel).getGroupName();


						targetIndex = getTrueIndexInView(targetIndex) + ((PathGroup) targetPanel).getPathPanelCount();
						targetIndex -= pathViewIndex > targetIndex ? 0 : 1;
					}
				}
				// Correct target index for specific gap scenarios
				else if (mouseOnBorder == 1 && targetIndex > pathViewIndex)
				{
					targetIndex = getTrueIndexInView(targetIndex -1);
				}
				else if (mouseOnBorder == -1 && targetIndex < pathViewIndex)
				{
					targetIndex = getTrueIndexInView(targetIndex + 1);
				}

				LinkedHashMap<String, PathmakerPath> storedPaths = plugin.getStoredPaths();

//				// Erase old group if it has 1 member left
//				if (pathLabel != null)
//				{
//					String oldGroup = storedPaths.get(pathLabel).pathGroup;
//					if (oldGroup != null)
//					{
//						JPanel memberPanel = pathGroups.get(oldGroup).memberPanel;
//						if (memberPanel.getComponentCount() == 2)
//						{
//							storedPaths.get(((PathPanel) memberPanel.getComponent(0)).getPathLabel().getText()).pathGroup = null;
//							storedPaths.get(((PathPanel) memberPanel.getComponent(1)).getPathLabel().getText()).pathGroup = null;
//						}
//						else
//							storedPaths.get(pathLabel).pathGroup = null;
//					}
//				}

				// Create new LinkedHashMap to replace the old plugin.paths map
				ArrayList<Map.Entry<String, PathmakerPath>> paths = new ArrayList<>(storedPaths.entrySet());

				// Move single path
				if (pathLabel != null)
				{
					Map.Entry<String, PathmakerPath> movedPath = paths.remove(pathViewIndex);
					paths.add(targetIndex, movedPath);
				}
				else // Move group
				{
					int numPanels = ((PathGroup) panel).getPathPanelCount();
					int start = 	pathViewIndex > targetIndex ? numPanels - 1 : 0;
					int end = 		pathViewIndex > targetIndex ? -1 : numPanels;
					int itValue = 	pathViewIndex > targetIndex ? -1 : 1;

					log.debug("numPanels: " + numPanels + " start: " + start + " end: " + end + " itValue: " + itValue);

					for (int i = start; i != end; i+= itValue)
					{
						Map.Entry<String, PathmakerPath> movedGroupedPath = paths.remove(pathViewIndex + start);
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
		};
	}

	void rearrangePaths(ArrayList<Integer> oldPathIndices, ArrayList<Integer> newPathIndices)
	{
//		if (oldPathIndices.size() != newPathIndices.size())
//		{
//			log.debug("Did not provide an equal amount of indices for path rearrangement.");
//		};
//
//		LinkedHashMap<String, PathmakerPath> storedPaths = plugin.getStoredPaths();
//
//		// REPLACE: CHECK INSTEAD WHILE ITERATING IF TWO PATHS NEXT TO EACH OTHER ARE IN THE SAME GROUP
//		if (pathLabel != null)
//		{
//			String oldGroup = storedPaths.get(pathLabel).pathGroup;
//			if (oldGroup != null)
//			{
//				JPanel memberPanel = pathGroups.get(oldGroup).memberPanel;
//				if (memberPanel.getComponentCount() == 2)
//				{
//					storedPaths.get(((PathPanel) memberPanel.getComponent(0)).getPathLabel().getText()).pathGroup = null;
//					storedPaths.get(((PathPanel) memberPanel.getComponent(1)).getPathLabel().getText()).pathGroup = null;
//				}
//				else
//					storedPaths.get(pathLabel).pathGroup = null;
//			}
//		}
//
//		// Create new LinkedHashMap to replace the old plugin.paths map
//		ArrayList<Map.Entry<String, PathmakerPath>> paths = new ArrayList<>(storedPaths.entrySet());
//
//		if (pathLabel != null)
//		{
//			Map.Entry<String, PathmakerPath> movedPath = paths.remove(pathViewIndex);
//			paths.add(targetIndex, movedPath);
//		}
//		else
//		{
//			for (int i = ((PathGroup) panel).getPathPanelCount() - 1; i >= 0; i--)
//			{
//				Map.Entry<String, PathmakerPath> movedGroupedPath = paths.remove(pathViewIndex + i);
//				paths.add(targetIndex, movedGroupedPath);
//			}
//		}
//
//		// Reorder paths
//		storedPaths.clear();
//		for (Map.Entry<String, PathmakerPath> path : paths)
//		{
//			storedPaths.put(path.getKey(), path.getValue());
//		}
	}
}
