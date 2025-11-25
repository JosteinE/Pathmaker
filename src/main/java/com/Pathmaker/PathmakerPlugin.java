package com.Pathmaker;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
//import net.runelite.api.events.GameTick;
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

//    @Subscribe
//    public void onGameTick(GameTick gameTick)
//    {
//    }

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
}
