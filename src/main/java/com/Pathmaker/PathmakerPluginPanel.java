package com.Pathmaker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.ImageUtil;
import com.google.gson.reflect.TypeToken;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.HashMap;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;

@Slf4j
public class PathmakerPluginPanel extends PluginPanel
{
    private static final ImageIcon IMPORT_ICON;
    private static final ImageIcon EXPORT_ICON;

    private final PluginErrorPanel noPathPanel = new PluginErrorPanel();
    private final JLayeredPane pathView = new JLayeredPane();

    Client client;
    PathmakerPlugin plugin;

    FlatTextField activePath;
    final int MAX_PATH_NAME_LENGTH = 20;

	JPanel dragGhost = null;

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
        title.setPreferredSize(new Dimension(80,20)); //getGraphics().getFontMetrics().stringWidth(title.getText()) + 10
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
                if(!plugin.getStoredPaths().containsKey(activePath.getText())) {return;}

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
					JsonElement element =  parser.parse(json);
					if (element == null || !element.isJsonObject())
					{
						log.debug("Imported element is not a JsonObject");
						return;
					}

					object = element.getAsJsonObject();
				}
				catch(Exception e)
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

        // Add link to config button
        // But how? Watchdogs implementation
        // https://github.com/adamk33n3r/runelite-watchdog/blob/master/src/main/java/com/adamk33n3r/runelite/watchdog/NotificationOverlay.java
//        JButton configButton = new JButton();
//        configButton.setIcon(new ImageIcon(ImageUtil.loadImageResource(PathmakerPlugin.class, "cross.png")));
//        configButton.setToolTipText("Config");
//        configButton.addChangeListener(ce ->
//        {
//            plugin.getEventBus().post(
//                    new OverlayMenuClicked(
//                    new OverlayMenuEntry(
//                    RUNELITE_OVERLAY_CONFIG,
//                            null,
//                            null),
//                            notificationOverlay)); <---- What in this is relevant? (link above)
//        });
//        northPanel.add(configButton);

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

		JPanel pathViewOverlay = new JPanel(null);
		pathViewOverlay.setOpaque(true);
		pathViewOverlay.setBackground(new Color(0, 0, 0, 0));
		pathViewOverlay.setVisible(true);
		pathView.add(pathViewOverlay, JLayeredPane.DRAG_LAYER);

		pathView.add(new JPanel(null), JLayeredPane.DEFAULT_LAYER);

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

        for (final String pathLabel : plugin.getStoredPaths().keySet())
        {
            // Create new path entry
			int entryIndex = pathView.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER).length;
            PathPanel pathEntry = new PathPanel(plugin, pathLabel);

			// Set as active path on label click
            pathEntry.getPathLabel().addActionListener(actionEvent ->
            {
                activePath.setText(pathEntry.getPathLabel().getText());
            });

			pathEntry.getPathLabel().addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
//					for (int i = 0; i < pathView.getComponents().length ; i++)
//					{
//						Component comp = pathView.getComponents()[i];
//						if (comp == pathEntry)
//						{
//							entryIndex = i;
//							break;
//						}
//					}

					dragGhost = new JPanel();
					dragGhost.setOpaque(true);
					dragGhost.setBackground(new Color(0, 255, 0, 150));
					dragGhost.setSize(e.getComponent().getWidth() - 20, e.getComponent().getHeight());

					int posY = (int) (e.getComponent().getLocationOnScreen().getY() - pathView.getLocationOnScreen().getY());
					//log.debug("posY: "+ posY);
					//log.debug("dragGhostY: "+ dragGhost.getY());
					//pathView.add(dragGhost);
					pathView.setLayer(dragGhost, JLayeredPane.DRAG_LAYER);
					pathView.repaint();
					//log.debug("dragGhostY: "+ dragGhost.getY());
				}

				@Override
				public void mouseReleased(MouseEvent e)
				{

					// Move to back, so getComponentAt doesn't select the dragged component
					//pathView.moveToBack(pathEntry);
					int entryPosY = dragGhost.getY();// new Point(pathEntry.getX(), pathEntry.getY());
					pathView.remove(dragGhost);
					dragGhost = null;

					Component[] otherComps = pathView.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER);
					int targetIndex = -1;
					for (int i = 0; i < otherComps.length; i++)
					{
						Component comp = otherComps[i];

						int minY = (int) comp.getBounds().getMinY();
						int maxY = (int) comp.getBounds().getMaxY();
						//log.debug("minY: " + minY + ", maxY: " + maxY);
						// If on other comp
						if (entryPosY >= minY && entryPosY < maxY)
						{
							// ADD GROUP
							log.debug("target is a path");
							targetIndex = i;
						}
					}
					log.debug("entryPosY: " + entryPosY);
					log.debug("targetIndex: {}", targetIndex);

					if (targetIndex == -1 || targetIndex == entryIndex)
					{
						plugin.rebuildPanel(false);
						return;
					}

					LinkedHashMap<String, PathmakerPath> storedPaths = plugin.getStoredPaths();

					ArrayList<Map.Entry<String, PathmakerPath>> paths = new ArrayList<>(storedPaths.entrySet());

					Map.Entry<String, PathmakerPath> movedPath = paths.remove(entryIndex);
					paths.add(targetIndex, movedPath);

					storedPaths.clear();
					for (Map.Entry<String, PathmakerPath> path : paths)
					{
						storedPaths.put(path.getKey(), path.getValue());
					}

					plugin.rebuildPanel(false);
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
					int entryHeight = (int) e.getComponent().getBounds().getHeight();

					int maxY = (int) (pathView.getSize().getHeight() - entryHeight);
					int newY = Math.max(0, Math.min(maxY, pathEntry.getY() + e.getY()));

					dragGhost.setLocation(pathEntry.getX(), newY);

					//log.debug("entryPosY: " + newY);
				}
			});

			pathView.add(pathEntry);
			pathView.setLayer(pathEntry, JLayeredPane.DEFAULT_LAYER);
        }

        boolean empty = pathView.getComponentCount() == 0;
        noPathPanel.setVisible(empty);

        repaint();
        revalidate();
    }
}
