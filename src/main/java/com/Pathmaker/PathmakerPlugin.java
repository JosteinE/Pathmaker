package com.Pathmaker;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.WorldView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;
import net.runelite.api.MenuAction;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.KeyCode;
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

    private Collection<PathmakerPath> paths;

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
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (event.getGroup().equals(PathmakerConfig.GROUND_MARKER_CONFIG_GROUP))
        {
            if (event.getKey().equals(PathmakerConfig.SHOW_MAP_ORB_MENU_OPTIONS)) {
                sharingManager.removeMenuOptions();
                if (config.showMapOrbMenuOptions()) {
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
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        // Only add menu option if shift is being held
        MenuAction menuAction = event.getMenuEntry().getType();
        final boolean hotKeyPressed = client.isKeyPressed(KeyCode.KC_SHIFT);
        if (!(hotKeyPressed && (menuAction == MenuAction.WALK || menuAction == MenuAction.SET_HEADING)))
        {
            return;
        }

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
        final int regionId = worldPoint.getRegionID();
        Collection<PathPoint>  regionPoints = getPathPointsInRegion(regionId);
        Optional<Collection<PathPoint>> existingOpt = regionPoints.stream()
                .filter(p -> p.getRegionX() == worldPoint.getRegionX() && p.getRegionY() == worldPoint.getRegionY() && p.getZ() == worldPoint.getPlane())
                .findFirst();

        // Add contextual button to shift+right-click menu, depending on if previously marked.
        client.createMenuEntry(-1)
                .setOption(existingOpt.isPresent() ? "Unmark" : "Mark")
                .setTarget("Tile")
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                        markTile(worldPoint));

        if (existingOpt.isPresent())
        {
            var existing = existingOpt.get();

            client.createMenuEntry(-2)
                    .setOption("Label")
                    .setTarget("Tile")
                    .setType(MenuAction.RUNELITE)
                    .onClick(e -> labelTile(existing));

            MenuEntry menuColor = client.createMenuEntry(-3)
                    .setOption("Color")
                    .setTarget("Tile")
                    .setType(MenuAction.RUNELITE);
            Menu submenu = menuColor.createSubMenu();

            if (regionPoints.size() > 1) {
                submenu.createMenuEntry(-1)
                        .setOption("Reset all")
                        .setType(MenuAction.RUNELITE)
                        .onClick(e ->
                                chatboxPanelManager.openTextMenuInput("Are you sure you want to reset the color of " + regionPoints.size() + " tiles?")
                                        .option("Yes", () ->
                                        {
                                            var newPoints = regionPoints.stream()
                                                    .map(p -> new GroundMarkerPoint(p.getRegionId(), p.getRegionX(), p.getRegionY(), p.getZ(), config.markerColor(), p.getLabel()))
                                                    .collect(Collectors.toList());
                                            savePoints(regionId, newPoints);
                                            loadPoints();
                                        })
                                        .option("No", Runnables.doNothing())
                                        .build());
            }

            submenu.createMenuEntry(-1)
                    .setOption("Pick")
                    .setType(MenuAction.RUNELITE)
                    .onClick(e ->
                    {
                        Color color = existing.getColor();
                        SwingUtilities.invokeLater(() ->
                        {
                            RuneliteColorPicker colorPicker = colorPickerManager.create(client,
                                    color, "Tile marker color", false);
                            colorPicker.setOnClose(c -> colorTile(existing, c));
                            colorPicker.setVisible(true);
                        });
                    });

            var existingColors = points.values().stream()
                    .map(ColorTileMarker::getColor)
                    .distinct()
                    .collect(Collectors.toList());
            for (Color color : existingColors) {
                if (!color.equals(existing.getColor())) {
                    submenu.createMenuEntry(-1)
                            .setOption(ColorUtil.prependColorTag("Color", color))
                            .setType(MenuAction.RUNELITE)
                            .onClick(e -> colorTile(existing, color));
                }
            }
        }
    }

    Collection<PathPoint> getPathPointsInRegion(int regionId)
    {
        Collection<PathPoint> pathPoints = new ArrayList<PathPoint>();
        for (Iterator<PathmakerPath> iterator = paths.iterator(); iterator.hasNext();)
        {
            PathmakerPath path = iterator.next();
            if(path.getRegionIDs().contains(regionId))
            {
                pathPoints.addAll(path.getPointsInRegion(regionId));
            }
        }

        return pathPoints;
    }
}
