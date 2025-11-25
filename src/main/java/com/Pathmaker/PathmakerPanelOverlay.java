package com.Pathmaker;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Color;
import javax.inject.Inject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.api.Client;

public class PathmakerPanelOverlay extends OverlayPanel
{
    private final PathmakerConfig config;
    private final Client client;

    private final String speedLabelPrefix = "Tiles pr/tick: ";
    WorldPoint lastPos = null;

    float moveSpeed = 0;

    @Inject
    private PathmakerPanelOverlay(PathmakerPlugin plugin, Client client, PathmakerConfig config)
    {
        super(plugin);

        setResizable(true);
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        this.client = client;
        this.config = config;
        this.panelComponent.setPreferredSize(new Dimension(20,0)); //graphics.getFontMetrics().stringWidth(speedLabelPrefix) +10
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Info panel
        if (config.infoPanel())
        {
//            calculateCurrentSpeed(startPoint, playerPosLocal);
//            // pathmakerPanelOverlay.render(graphics); Renders on its own
//            log.debug("moveSpeed: {}", pathmakerPanelOverlay.moveSpeed);

            calculateCurrentSpeed();
            this.panelComponent.getChildren().add(TitleComponent.builder().text(speedLabelPrefix + String.format("%.2f", moveSpeed)).color(Color.WHITE).build());
            this.panelComponent.setPreferredSize(new Dimension(20,0));
            return super.render(graphics);
        }
        return null;
    }

    public void calculateCurrentSpeed()
    {
        WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();

        if (lastPos != null)
        {
            float distance = lastPos.distanceTo(playerPos);
            if (distance != 0) {
                // float tileSize = 128;
                moveSpeed = distance;// / tileSize;
                lastPos = playerPos;
            }
        }
        lastPos = playerPos;
    }
}
