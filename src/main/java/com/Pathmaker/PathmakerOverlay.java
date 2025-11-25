package com.Pathmaker;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;                        // For perspective of tile
import net.runelite.api.Tile;                               // for game tiles
import net.runelite.api.WorldView;                          // For camera orientation and mouse pick-up
import net.runelite.api.coords.LocalPoint;                  // For player position
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

import java.awt.geom.Line2D;
import net.runelite.api.Point;

import lombok.extern.slf4j.Slf4j; // https://projectlombok.org/features/log

@Slf4j
public class PathmakerOverlay extends Overlay
{
    private final Client client;
    private final PathmakerConfig config;

    private final float tileSize = 128;

    private LocalPoint startPoint;
    private LocalPoint endPoint;
    private String endLabel;
    private Color endLabelColor = Color.YELLOW;


    // For ref: net.runelite.client.ui.overlay.OverlayRenderer

    @Inject
    private PathmakerOverlay(Client client, PathmakerConfig config)//, WorldView worldview)
    {
        this.client = client;
        this.config = config;

        //this.worldview = worldview;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(PRIORITY_MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Fetch player position
        final WorldPoint playerPos = client.getLocalPlayer().getWorldLocation(); // doing getWorldLocation instead of getLocalLocation, because world loc. is server-side.
        LocalPoint playerPosLocal = null;
        if (playerPos != null)
        {
            playerPosLocal = LocalPoint.fromWorld(client, playerPos);
        }

        // Hovered tile
        if (config.highlightHoveredTile())
        {
            WorldView wv = client.getLocalPlayer().getWorldView();
            Tile tile = wv.getSelectedSceneTile();
            // If we have tile "selected" render it
            if (tile != null)
            {
                endPoint = tile.getLocalLocation();
                renderTile(graphics, tile.getLocalLocation(), config.highlightHoveredColor(), config.hoveredTileBorderWidth(), config.hoveredTileFillColor());

                // Add label
                endLabel = "W( " + tile.getWorldLocation().getX() + ", " + tile.getWorldLocation().getY() + " )";
                if (startPoint != null)
                {
                    endLabel += ", Dist: " + (int) ((startPoint.distanceTo(endPoint)/tileSize));
                }
                Point canvasTextLocation = Perspective.getCanvasTextLocation(client, graphics, endPoint, endLabel, 0);

                if (canvasTextLocation != null)
                {
                    OverlayUtil.renderTextLocation(graphics, canvasTextLocation, endLabel, endLabelColor);
                }
            }
        }

        // Current tile
        if (config.highlightCurrentTile() && playerPosLocal != null)
        {
            startPoint = playerPosLocal;
            renderTile(graphics, playerPosLocal, config.highlightCurrentColor(), config.currentTileBorderWidth(), config.currentTileFillColor());
        }

        // Line
        if (config.drawPathLine())
        {
            drawLine(graphics, startPoint, endPoint, config.pathLineColor());
        }

        return null;
    }

    private void renderTile(final Graphics2D graphics, final LocalPoint dest, final Color color, final double borderWidth, final Color fillColor)
    {
        if (dest == null)
        {
            return;
        }

        final Polygon poly = Perspective.getCanvasTilePoly(client, dest);

        if (poly == null)
        {
            return;
        }

        OverlayUtil.renderPolygon(graphics, poly, color, fillColor, new BasicStroke((float) borderWidth));
    }

    private void drawLine(final Graphics2D graphics, final LocalPoint startLoc, final LocalPoint endLoc, final Color color){ //, int counter) {
        if (startPoint == null || endPoint == null) {
            return;
        }

        final int z = client.getLocalPlayer().getWorldView().getPlane();

        final int startHeight = Perspective.getTileHeight(client, startPoint, z);
        final int endHeight = Perspective.getTileHeight(client, endPoint, z);

        Point p1 = Perspective.localToCanvas(client, startPoint.getX(), startPoint.getY(), startHeight);
        Point p2 = Perspective.localToCanvas(client, endPoint.getX(), endPoint.getY(), endHeight);

        if (p1 == null || p2 == null) {
            return;
        }

        Line2D.Double line = new Line2D.Double(p1.getX(), p1.getY(), p2.getX(), p2.getY());

        graphics.setColor(color);
        graphics.setStroke(new BasicStroke((float) config.pathLineWidth()));
        graphics.draw(line);

//        if (counter == 1) {
//            drawCounter(graphics, p1.getX(), p1.getY(), 0);
//        }
//        drawCounter(graphics, p2.getX(), p2.getY(), counter);
    }

    public LocalPoint getCurrentPlayerPosition()
    {
        return startPoint;
    }
}
