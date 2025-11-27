package com.Pathmaker;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Pathmaker",
    description = "Draw lines between marked tiles.",
    tags = {"object,path,line,draw,tile,indicator,navigation"}
)

public class PathmakerPlugin extends Plugin
{
    private static final String CONFIG_GROUP = "linePath";
    private static final String REGION_PREFIX = "region_";

    private final HashMap<String, PathmakerPath> paths = new HashMap<>();

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
    private PathmakerSharingManager sharingManager;

    @Inject
    private EventBus eventBus;

    boolean drawTiles;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Example started!");
        overlayManager.add(overlay);
        overlayManager.add(panelOverlay);
        if (config.showMapOrbMenuOptions())
        {
            sharingManager.addMenuOptions();
        }
        //loadPoints();
        eventBus.register(sharingManager);
	}

	@Override
	protected void shutDown() throws Exception
	{
        eventBus.unregister(sharingManager);
		log.debug("Example stopped!");
        overlayManager.remove(overlay);
        overlayManager.remove(panelOverlay);
        sharingManager.removeMenuOptions();
        //points.clear();
	}

    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
        panelOverlay.calculateCurrentSpeed();
        overlay.importTilesToHighlight(getTilesToHighlight());
    }

    // Get marked tiles within the rendered regions
    Collection<PathPoint> getTilesToHighlight()
    {
        Collection<PathPoint>  pathPoints = new ArrayList<>();
        int[] regionsToLoad = client.getTopLevelWorldView().getMapRegions();
        //log.debug("Number of regions to load: {}", regionsToLoad.length);
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

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (event.getGroup().equals(PathmakerConfig.GROUND_MARKER_CONFIG_GROUP))
        {
            if (event.getKey().equals(PathmakerConfig.SHOW_MAP_ORB_MENU_OPTIONS)) {
                sharingManager.removeMenuOptions();
                if (config.showMapOrbMenuOptions())
                {
                    sharingManager.addMenuOptions();
                    //sharingManager.addClearMenuOption();
                }
            }
        }
    }

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
//		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
//		{
//			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
//		}
	}

	@Provides
    PathmakerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PathmakerConfig.class);
	}

    @Subscribe
    public void onMenuEntryAdded(final MenuEntryAdded event)
    {
        // Only add menu option if shift is being held
        MenuAction menuAction = event.getMenuEntry().getType();
        final boolean hotKeyPressed = client.isKeyPressed(KeyCode.KC_SHIFT);

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
            if (pathPoint == null) {
                client.getMenu().createMenuEntry(-1)
                        .setOption("Add to path")
                        .setTarget(event.getTarget())
                        .setType(MenuAction.RUNELITE)
                        .onClick(e -> createOrAddToPath(new PathPoint(
                                worldPoint.getRegionID(),
                                worldPoint.getRegionX(),
                                worldPoint.getRegionY(),
                                worldPoint.getPlane(),
                                config.pathLineColor())));
            }
            else // Add remove function for existing tiles!
            {
//            client.createMenuEntry(-1)
//                    .setOption("Remove from path")
//                    .setTarget("Tile")
//                    .setType(MenuAction.RUNELITE)
//                    .onClick(e ->
//                            FUNC:REMOVETILE(new PathPoint(
//                                    worldPoint.getRegionID(),
//                                    worldPoint.getRegionX(),
//                                    worldPoint.getRegionY(),
//                                    worldPoint.getPlane())));
            }


//        if (existingOpt.isPresent())
//        {
//            var existing = existingOpt.get();
//
//            client.createMenuEntry(-2)
//                    .setOption("Label")
//                    .setTarget("Tile")
//                    .setType(MenuAction.RUNELITE)
//                    .onClick(e -> labelTile(existing));
//
//            MenuEntry menuColor = client.createMenuEntry(-3)
//                    .setOption("Color")
//                    .setTarget("Tile")
//                    .setType(MenuAction.RUNELITE);
//            Menu submenu = menuColor.createSubMenu();
//
//            if (regionPoints.size() > 1) {
//                submenu.createMenuEntry(-1)
//                        .setOption("Reset all")
//                        .setType(MenuAction.RUNELITE)
//                        .onClick(e ->
//                                chatboxPanelManager.openTextMenuInput("Are you sure you want to reset the color of " + regionPoints.size() + " tiles?")
//                                        .option("Yes", () ->
//                                        {
//                                            var newPoints = regionPoints.stream()
//                                                    .map(p -> new GroundMarkerPoint(p.getRegionId(), p.getRegionX(), p.getRegionY(), p.getZ(), config.markerColor(), p.getLabel()))
//                                                    .collect(Collectors.toList());
//                                            savePoints(regionId, newPoints);
//                                            loadPoints();
//                                        })
//                                        .option("No", Runnables.doNothing())
//                                        .build());
//            }
//
//            submenu.createMenuEntry(-1)
//                    .setOption("Pick")
//                    .setType(MenuAction.RUNELITE)
//                    .onClick(e ->
//                    {
//                        Color color = existing.getColor();
//                        SwingUtilities.invokeLater(() ->
//                        {
//                            RuneliteColorPicker colorPicker = colorPickerManager.create(client,
//                                    color, "Tile marker color", false);
//                            colorPicker.setOnClose(c -> colorTile(existing, c));
//                            colorPicker.setVisible(true);
//                        });
//                    });
//
//            var existingColors = points.values().stream()
//                    .map(ColorTileMarker::getColor)
//                    .distinct()
//                    .collect(Collectors.toList());
//            for (Color color : existingColors) {
//                if (!color.equals(existing.getColor())) {
//                    submenu.createMenuEntry(-1)
//                            .setOption(ColorUtil.prependColorTag("Color", color))
//                            .setType(MenuAction.RUNELITE)
//                            .onClick(e -> colorTile(existing, color));
//                }
//
//      }
        }
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

    // Return PathPoint if one was previously created on the specified tile
    PathPoint getPathPointAtRegionTile(int regionId, int relativeX,  int relativeY, int plane)
    {
        //log.debug("Starting tile fetch.");
        if(paths.isEmpty())
        {
            //log.debug("Paths map empty, returning null");
            return null;
        }

        Collection<PathPoint> regionPoints = getPathPointsInRegion(regionId);

        // Iterate through each region tile to determine if the selected tile has a PathPoint
        for (PathPoint point : regionPoints)
        {
            if(point.getX() == relativeX && point.getY() == relativeY)
            {
                log.debug("Tile found, in region id: {}, x: {}, y: {} )",
                        regionId, relativeX, relativeY);
                return point;
            }
        }

        log.debug("Tile not found, in region id: {}, x: {}, y: {} )",
                regionId, relativeX, relativeY);
        return null;
    }

    //    void createOrAddToPath(int regionId, int regionX, int regionY, int plane)
    //    {
    //        createOrAddToPath(new PathPoint(regionId, regionX, regionY, plane));
    //    }

    // Create a new path starting with the given point or add to existing path
    void createOrAddToPath(PathPoint point)
    {
        String activePath = config.activePath();
        PathmakerPath path = paths.containsKey(activePath) ? paths.get(activePath) : new PathmakerPath(point);
        path.addPathPoint(point);
        paths.put(activePath, path);
        log.debug("Point ( Region: {}, X: {}, Y: {} added path to: {}",
                point.getRegionId(), point.getX(), point.getY(), config.activePath());
    }
}
