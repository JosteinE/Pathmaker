/*
 * Copyright (c) 2025, JosteinE
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *	  list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	  this list of conditions and the following disclaimer in the documentation
 *	  and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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

    // Move Speed - Add config toggle for move speed!
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
//        // InfoBox
//        if (config.infoBoxEnabled())
//        {
//            if (config.infoBoxSpeed())
//            {
//                this.panelComponent.getChildren().add(TitleComponent.builder().text(getSpeedLabelString(moveSpeed)).color(Color.WHITE).build());
//            }
//
//            // Render infobox
//            this.panelComponent.setPreferredSize(new Dimension(20,0));
//            return super.render(graphics);
//        }
        return null;
    }

    // Called by PathmakerPlugin.onGameTick()
    public void calculateCurrentSpeed()
    {
        WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();

        if (lastPos != null)
        {
            float distance = lastPos.distanceTo(playerPos);
            //if (distance != 0) {
            moveSpeed = distance;
            //}
        }
        lastPos = playerPos;
    }

    public String getSpeedLabelString(float speed)
    {
        return speedLabelPrefix + String.format("%.2f", moveSpeed);
    }
}
