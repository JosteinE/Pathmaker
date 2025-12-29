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
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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

	class PathGroup extends JPanel
	{
		FlatTextField groupTextField = new FlatTextField();
		JPanel memberPanel = new JPanel();

		PathGroup(PathPanel firstPathEntry, String groupName)
		{
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBorder(BorderFactory.createEmptyBorder(0, 1, 1, 1));
			setBackground(Color.BLUE);

			groupTextField.setText(groupName);
			add(groupTextField);
			groupTextField.setBackground(Color.BLUE);

			memberPanel.setLayout(new BoxLayout(memberPanel, BoxLayout.Y_AXIS));
			memberPanel.setBackground(Color.BLUE);
			memberPanel.add(firstPathEntry);
			add(memberPanel);
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

			pathEntry.getPathLabel().addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e) {}

				@Override
				public void mouseReleased(MouseEvent e)
				{
					pathEntry.setBorder(BorderFactory.createEmptyBorder());
					pathEntry.repaint();

					int targetIndex = getHoveredPathIndex(e);

					if (targetIndex == -1 || targetIndex == entryIndex)
						return;

					// Correct target index for gaps
					JPanel targetPanel = (JPanel) pathView.getComponent(targetIndex);
					int mouseOnBorder = isMouseHoveringPathBorder(e, targetPanel, DRAG_DROP_Y_MARGIN);

					// Add group if hovering another path
					if (mouseOnBorder == 0)
					{
						PathmakerPath draggedPath = plugin.getStoredPaths().get(pathLabel);

						if (targetPanel instanceof PathPanel)
						{
							String targetName = ((PathPanel) targetPanel).getPathLabel().getText();
							PathmakerPath targetPath = plugin.getStoredPaths().get(targetName);

							// create new group
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
								draggedPath.pathGroup = targetPath.pathGroup;
						}
						else
							draggedPath.pathGroup = ((PathGroup) targetPanel).getGroupName();

						plugin.rebuildPanel(true);
						return;
					}

					if (mouseOnBorder == 1 && targetIndex > entryIndex)
						targetIndex -= 1;
					else if (mouseOnBorder == -1 && targetIndex < entryIndex)
						targetIndex += 1;


					// Erase old group if it has 1 member left
					LinkedHashMap<String, PathmakerPath> storedPaths = plugin.getStoredPaths();
					String oldGroup = storedPaths.get(pathLabel).pathGroup;
					if(oldGroup != null)
					{
						JPanel memberPanel = pathGroups.get(oldGroup).memberPanel;
						log.debug("memberPanelComponentCount: " + memberPanel.getComponentCount());
						if (memberPanel.getComponentCount() == 2)
						{
							storedPaths.get(((PathPanel) memberPanel.getComponent(0)).getPathLabel().getText()).pathGroup = null;
							storedPaths.get(((PathPanel) memberPanel.getComponent(1)).getPathLabel().getText()).pathGroup = null;
						}
						else
							storedPaths.get(pathLabel).pathGroup = null;
					}

					// Create new LinkedHashMap to replace the old plugin.paths map
					ArrayList<Map.Entry<String, PathmakerPath>> paths = new ArrayList<>(storedPaths.entrySet());
					Map.Entry<String, PathmakerPath> movedPath = paths.remove(entryIndex);

					paths.add(targetIndex, movedPath);

					// Reorder paths
					storedPaths.clear();
					for (Map.Entry<String, PathmakerPath> path : paths)
					{
						storedPaths.put(path.getKey(), path.getValue());
					}

					plugin.rebuildPanel(true);
				}
			});

			pathEntry.getPathLabel().addMouseMotionListener(new MouseMotionListener()
			{
				@Override
				public void mouseMoved(MouseEvent e)
				{
				}

				@Override
				public void mouseDragged(MouseEvent e)
				{
					// Reset border
					for (int i = 0; i < pathView.getComponentCount(); i++)
					{
						if (!(pathView.getComponents()[i] instanceof PathPanel)) continue;
						PathPanel pathPanel = ((PathPanel) pathView.getComponents()[i]);
						pathPanel.setBorder(BorderFactory.createEmptyBorder());
					}
					// Set the dragged colour
					setPanelInnerBorderColor(pathEntry, Color.RED);
					pathView.repaint();

					int targetIndex = getHoveredPathIndex(e);
					if (targetIndex == -1 || targetIndex == entryIndex) return;

					// Is mouse on the path border?
					JPanel targetPanel = (JPanel) pathView.getComponent(targetIndex);
					int mouseOnBorder = isMouseHoveringPathBorder(e, targetPanel, DRAG_DROP_Y_MARGIN);

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
					else
						setPanelInnerBorderColor(targetPanel, Color.GREEN);
					targetPanel.repaint();
				}
			});

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
					PathGroup group = new PathGroup(pathEntry, groupName);
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

	void setPanelInnerBorderColor(JPanel pathPanel, Color color)
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
	int isMouseHoveringPathBorder(MouseEvent e, JPanel targetPanel, int margin)
	{
		int entryPosY = (int) (e.getYOnScreen() - pathView.getLocationOnScreen().getY());
		int minY = (int) targetPanel.getBounds().getMinY();
		int maxY = (int) targetPanel.getBounds().getMaxY();

		if (entryPosY < minY + margin) return 1;
		if (entryPosY > maxY - margin) return -1;
		return 0;
	}

	int getHoveredPathIndex(MouseEvent e)
	{
		int entryPosY = (int) (e.getYOnScreen() - pathView.getLocationOnScreen().getY());

		if (entryPosY < pathView.getComponent(0).getBounds().getMinY())
			return 0;
		if (entryPosY > pathView.getComponent(pathView.getComponentCount() - 1).getBounds().getMaxY())
			return pathView.getComponentCount() - 1;

		Component[] otherComps = pathView.getComponents();

		for (int i = 0; i < otherComps.length; i++)
		{
			Component comp = otherComps[i];

			int minY = (int) comp.getBounds().getMinY();
			int maxY = (int) comp.getBounds().getMaxY();

			if (entryPosY >= minY && entryPosY < maxY)
			{
				return i;
			}
		}
		return -1;
	}
}
