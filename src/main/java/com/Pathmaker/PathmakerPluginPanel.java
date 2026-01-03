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
import java.util.Set;
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
	ArrayList<String> groupNames = new ArrayList<>();
	//HashMap<String, PathGroup> pathGroups = new HashMap<>();

	Client client;
	PathmakerPlugin plugin;

	FlatTextField activePath;
	final int MAX_PATH_NAME_LENGTH = 20;
	//final int DRAG_DROP_Y_MARGIN = 10;

	static
	{
		IMPORT_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "import.png"));
		EXPORT_ICON = new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "export.png"));
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
		groupNames.clear();


		ArrayList<String> pathKeys = new ArrayList<>(plugin.getStoredPaths().keySet());
		String validGroup = null;
		for (int i = 0;  i < pathKeys.size(); i++)
		{
			String pathLabel = pathKeys.get(i);
			PathPanel pathEntry = new PathPanel(plugin, pathLabel);

			// Set as active path on label click
			pathEntry.getPathLabel().addActionListener(actionEvent ->
			{
				activePath.setText(pathEntry.getPathLabel().getText());
			});

			int PANEL_MARGIN = 10;

			// Injecting an available group name whenever a PathPanel is dropped, to avoid passing the entire groupNames array
			DropAdapter dropAdapter = new DropAdapter(plugin, pathView, pathEntry, i, pathLabel, PANEL_MARGIN);
			pathEntry.getPathLabel().addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseReleased(MouseEvent e)
				{
					super.mouseReleased(e);
					dropAdapter.setAvailableGroupName(getAvailableGroupName());
					dropAdapter.mouseReleased(e);
					dropAdapter.setAvailableGroupName(null);
				}
			});

			pathEntry.getPathLabel().addMouseMotionListener(new DragAdapter(pathView, pathEntry, PANEL_MARGIN));

			String groupName = plugin.getStoredPaths().get(pathLabel).pathGroup;

			if (groupName == null)
			{
				validGroup = null;
				pathView.add(pathEntry);
			}
			else
			{
				// Validate a group on first occurrence, by comparing it to the following path's group
				// If they're equal, then the group is valid.
				if (validGroup == null || !validGroup.equals(groupName))
				{
					validGroup = null;

					// Group is invalid
					if (i == pathKeys.size() - 1 ||
						!groupName.equals(plugin.getStoredPaths().get(pathKeys.get(i+1)).pathGroup))
					{
						plugin.getStoredPaths().get(pathLabel).pathGroup = null;
						pathView.add(pathEntry);
					}
					else // Group is valid, create it
					{
						validGroup = groupName;
						PathGroup group = new PathGroup(plugin, pathView, pathEntry, groupName, i);
						groupNames.add(groupName);
						pathView.add(group);
					}
				}
				else // Last component is a valid group which this path belongs to
				{
					((PathGroup) pathView.getComponent(pathView.getComponentCount()-1)).addPathPanel(pathEntry);
				}
			}
		}

		boolean empty = pathView.getComponentCount() == 0;
		noPathPanel.setVisible(empty);

		repaint();
		revalidate();
	}

	String getAvailableGroupName()
	{
		String groupName = "group 1";
		if (!groupNames.isEmpty())
		{
			int num = 1;
			while (groupNames.contains(groupName))
			{
				num++;
				groupName = "group " + num;
			}
		}
		return groupName;
	}
}
