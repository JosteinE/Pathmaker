package com.Pathmaker;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.*;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.awt.Color;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;

@Slf4j
@PluginDescriptor(
	name = "Pathmaker",
    description = "Draw lines between marked tiles.",
    tags = {"object,path,line,draw,tile,indicator,navigation"}
)

public class PathmakerPlugin extends Plugin
{
    private static final String ICON_FILE = "panel_icon.png";
    private static final String CONFIG_KEY = "paths";

    public final int  MAX_LABEL_LENGTH = 15;

    private final HashMap<String, PathmakerPath> paths = new HashMap<>();
    private PathmakerPluginPanel pluginPanel;
    private NavigationButton navButton;

    boolean hotKeyPressed = false;

	@Inject
	private Client client;

	@Inject
	private PathmakerConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PathmakerOverlay overlay;

    @Inject
    private PathmakerPanelOverlay panelOverlay;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClientToolbar clientToolbar;

    @Getter
    @Inject
    private ColorPickerManager colorPickerManager;

    @Inject
    private ClientThread clientThread;

    @Inject
    public Gson gson;

    @Inject
    private ChatboxPanelManager chatboxPanelManager;

    @Provides
    PathmakerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PathmakerConfig.class);
    }

	@Override
	protected void startUp() throws Exception
	{
        overlayManager.add(overlay);
        overlayManager.add(panelOverlay);

        pluginPanel = new PathmakerPluginPanel(client, this);

        load();

        clientThread.invokeLater(() ->
        {
            final BufferedImage icon = ImageUtil.loadImageResource(getClass(), ICON_FILE);
            navButton = NavigationButton.builder()
                    .tooltip("Pathmaker")
                    .icon(icon)
                    .priority(5)
                    .panel(pluginPanel)
                    .build();
            clientToolbar.addNavigation(navButton);
        });
	}

	@Override
	protected void shutDown() throws Exception
	{
        overlayManager.remove(overlay);
        overlayManager.remove(panelOverlay);
        clientToolbar.removeNavigation(navButton);

        paths.clear();
    }

    void save()
    {
        configManager.unsetConfiguration(PathmakerConfig.CONFIG_GROUP, CONFIG_KEY);
        String json = gson.toJson(paths);
        configManager.setConfiguration(PathmakerConfig.CONFIG_GROUP, CONFIG_KEY, json);
        configManager.sendConfig();
    }

    private void load()
    {
        paths.clear();
        String json = configManager.getConfiguration(PathmakerConfig.CONFIG_GROUP, CONFIG_KEY);

        if (Strings.isNullOrEmpty(json))
        {
            return;
        }

        try
        {
            HashMap<String, PathmakerPath> loadedPaths = gson.fromJson(json, new TypeToken<HashMap<String, PathmakerPath>>(){}.getType());
            paths.putAll(loadedPaths);
        }
        catch (IllegalStateException | JsonSyntaxException ignore)
        {
            JOptionPane.showConfirmDialog(pluginPanel,
                    "The paths you are trying to load from your config are malformed",
                    "Warning", JOptionPane.OK_CANCEL_OPTION);
        }

        rebuildPanel();
    }

    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
        if(config.infoBoxEnabled() && config.infoBoxSpeed())
            panelOverlay.calculateCurrentSpeed();
    }

    // Get marked tiles within the rendered regions
    Collection<PathPoint> getTilesToHighlight()
    {
        Collection<PathPoint>  pathPoints = new ArrayList<>();

        // Get rendered regionIDs
        int[] regionsToLoad = client.getTopLevelWorldView().getMapRegions();
        for (PathmakerPath path : paths.values())
        {
            for (int regionID : regionsToLoad)
            {
                Collection<PathPoint> pathPointsInRegion = path.getPointsInRegion(regionID);
                if  (pathPointsInRegion != null)
                {
                    pathPoints.addAll(path.getPointsInRegion(regionID));
                }
            }
        }
        return pathPoints;
    }

//    @Subscribe
//    public void onConfigChanged(ConfigChanged event)
//    {
//        if (event.getGroup().equals(PathmakerConfig.CONFIG_GROUP))
//        {
//        }
//    }

//	@Subscribe
//	public void onGameStateChanged(GameStateChanged gameStateChanged)
//	{
//		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
//		{
//			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
//		}
//	}

    @Subscribe
    public void onMenuEntryAdded(final MenuEntryAdded event)
    {
        // Only add menu option if shift is being held
        MenuAction menuAction = event.getMenuEntry().getType();
        hotKeyPressed = client.isKeyPressed(KeyCode.KC_SHIFT);

        if (hotKeyPressed && (menuAction == MenuAction.WALK || menuAction == MenuAction.SET_HEADING))
        {
            // Fetch game world
            int worldId = event.getMenuEntry().getWorldViewId();
            WorldView wv = client.getWorldView(worldId);
            if (wv == null) {
                return;
            }

            // Fetch selected tile
            final Tile selectedSceneTile = wv.getSelectedSceneTile();
            if (selectedSceneTile == null) {
                return;
            }

            // Fetch path points for region
            final WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, selectedSceneTile.getLocalLocation());
            PathPoint pathPoint = getPathPointAtRegionTile(
                    worldPoint.getRegionID(),
                    worldPoint.getRegionX(),
                    worldPoint.getRegionY(),
                    worldPoint.getPlane());

            // If tile is not previously marked, add the "add" option.
            if (pathPoint == null || !paths.containsKey(getActivePathName()) || !paths.get(getActivePathName()).containsPoint(pathPoint))
            {
                // Set the menu option text color to match the path or default
                String target = paths.containsKey(getActivePathName()) ?
                        ColorUtil.prependColorTag(Text.removeTags(getActivePathName()), paths.get(getActivePathName()).color) :
                        ColorUtil.prependColorTag(Text.removeTags(getActivePathName()), config.pathColor());

                client.getMenu().createMenuEntry(-1)
                        .setOption("Add to path")
                        .setTarget(target)
                        .setType(MenuAction.RUNELITE)
                        .onClick(e -> createOrAddToPath(new PathPoint(
                                worldPoint.getRegionID(),
                                worldPoint.getRegionX(),
                                worldPoint.getRegionY(),
                                worldPoint.getPlane())));
            }

            if (pathPoint != null)
            {
                for (String pathName : paths.keySet())
                {
                    if (!paths.get(pathName).containsPoint(pathPoint))
                    {continue;}

                    // Set menu entry color
                    String target = ColorUtil.prependColorTag(Text.removeTags(pathName), paths.get(pathName).color);

                    // Only configure add loop/unloop/label if point belongs to the active group
                    if (pathName.equals(getActivePathName()))
                    {
                        // Only allow loop/unloop with points connected to the last point
                        if ((pathPoint.getDrawIndex() == paths.get(pathName).getSize() - 2 && paths.get(pathName).loopPath) ||
                                pathPoint.getDrawIndex() == 0) {
                            client.getMenu().createMenuEntry(-1)
                                    .setOption(paths.get(pathName).loopPath ? "Unloop" : "Loop")
                                    .setTarget(target)
                                    .setType(MenuAction.RUNELITE)
                                    .onClick(e ->
                                    {
                                        // Reverse and unloop if target point is second to last in draw order (this preserves the path structure)
                                        if (pathPoint.getDrawIndex() == paths.get(pathName).getSize() - 2)
                                        {
                                            paths.get(pathName).setNewIndex(paths.get(pathName).getPointAtDrawIndex(paths.get(pathName).getSize() - 1), 0);
                                            paths.get(pathName).reverseDrawOrder();
                                        }

                                        paths.get(pathName).loopPath = !paths.get(pathName).loopPath;
                                        rebuildPanel();
                                    });
                        }

                        // Add label rename option
                        client.getMenu().createMenuEntry(-1)
                                .setOption("Point label")
                                .setTarget(target)
                                .setType(MenuAction.RUNELITE)
                                .onClick(e ->
                                {
                                    String currentLabel = pathPoint.getLabel() == null ? "" : pathPoint.getLabel();

                                    chatboxPanelManager.openTextInput("Path point label")
                                            .value(currentLabel)
                                            .onDone(label ->
                                            {
                                                pathPoint.setLabel(label.substring(0, MAX_LABEL_LENGTH)); // From
                                                rebuildPanel();
                                            })
                                            .build();
                                });
                    }

                    // Add remove option regardless of belonging path
                    client.getMenu().createMenuEntry(-1)
                            .setOption("Remove from path")
                            .setTarget(target)
                            .setType(MenuAction.RUNELITE)
                            .onClick(e -> removePoint(pathName, pathPoint));
                }
            }
        }
    }

    public HashMap<String, PathmakerPath> getStoredPaths()
    {
        return paths;
    }

    public PathmakerPath getActivePath()
    {
        return paths.get(getActivePathName());
    }

    public boolean pathExists(String pathName)
    {
        return paths.containsKey(pathName);
    }

    // Returns pathPoints from all paths within the specified region
    Collection<PathPoint> getPathPointsInRegion(int regionId)
    {
        Collection<PathPoint> pathPoints = new ArrayList<PathPoint>();

        for (String pathName : getPathsInRegionKeys(regionId))
        {
            PathmakerPath path = paths.get(pathName);
            pathPoints.addAll(path.getPointsInRegion(regionId));
        }

        return pathPoints;
    }

    // Iterate through the stored paths and find all that have tiles within the specified region
    Collection<String> getPathsInRegionKeys(int regionId)
    {
        Collection<String> pathsInRegionKeys = new ArrayList<>();
        for (String pathName : paths.keySet())
        {
            for (int pathRegionId : paths.get(pathName).getRegionIDs())
            {
                if(pathRegionId == regionId)
                {
                    pathsInRegionKeys.add(pathName);
                    break;
                }
            }
        }
        return pathsInRegionKeys;
    }

    Color getDefaultPathColor()
    {
        return config.pathColor();
    }

    // Return PathPoint if one was previously created on the specified tile
    PathPoint getPathPointAtRegionTile(int regionId, int relativeX,  int relativeY, int plane)
    {
        if(paths.isEmpty())
        {
            return null;
        }

        Collection<PathPoint> regionPoints = getPathPointsInRegion(regionId);

        // Iterate through each region tile to determine if the selected tile has a PathPoint
        for (PathPoint point : regionPoints)
        {
            if(point.getX() == relativeX && point.getY() == relativeY)
            {
//                log.debug("Tile found, in region id: {}, x: {}, y: {} )",
//                        regionId, relativeX, relativeY);
                return point;
            }
        }

//        log.debug("Tile not found, in region id: {}, x: {}, y: {} )",
//                regionId, relativeX, relativeY);
        return null;
    }

    // Create a new path starting with the given point or add to existing path
    void createOrAddToPath(PathPoint point)
    {
        String activePath = pluginPanel.activePath.getText();//config.activePath();

        if(activePath == null) return;

        //log.debug("Checking for existing path: {}", activePath);
        PathmakerPath path;
        if(paths.containsKey(activePath))
        {
            path = paths.get(activePath);
            path.addPathPoint(point);
        }
        else
        {
            // Initialize new path with the initial point
            path = new PathmakerPath(point);
            path.color = getDefaultPathColor();
        }
        paths.put(activePath, path);
        rebuildPanel();
    }

    void removePoint(String pathName, PathPoint point)
    {
        paths.get(pathName).removePathPoint(point);
        if (paths.get(pathName).getSize() == 0)
        {
            removePath(pathName);
        }
        rebuildPanel();
    }

    void removePath(String pathName)
    {
        paths.remove(pathName);
    }

    String getActivePathName()
    {
        return pluginPanel.activePath.getText();
    }

    void rebuildPanel()
    {
        pluginPanel.rebuild();
        save();
    }
}
