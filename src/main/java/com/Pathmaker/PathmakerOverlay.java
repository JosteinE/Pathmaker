package com.Pathmaker;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;

import com.google.protobuf.EnumValue;
import com.google.protobuf.StringValue;
import com.sun.jna.platform.win32.Advapi32Util;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;                  // For player position
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

import java.awt.geom.Line2D;

import lombok.extern.slf4j.Slf4j; // https://projectlombok.org/features/log

import static com.Pathmaker.PathmakerConfig.hoveredTileLabelMode;

@Slf4j
public class PathmakerOverlay extends Overlay
{
    private static final int MAX_DRAW_DISTANCE = 32;

    private final Client client;
    private final PathmakerConfig config;

    private final float tileSize = 128;

    private LocalPoint startPoint;
    private LocalPoint endPoint;


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
        LocalPoint playerPosLocal = playerPos == null ? null : LocalPoint.fromWorld(client, playerPos);

        // Current tile
        if (config.highlightCurrentTile() && playerPosLocal != null)
        {
            startPoint = playerPosLocal;
            renderTile(graphics, playerPosLocal, config.highlightCurrentColor(), config.currentTileBorderWidth(), config.currentTileFillColor());
        }

        // Fetch hovered tile and if successful, assign it to endPoint
        WorldView wv = client.getLocalPlayer().getWorldView();
        Tile tile = wv.getSelectedSceneTile();
        endPoint = tile == null ? endPoint : tile.getLocalLocation();

        // Return here if the distance to hovered tile exceeds the user interactable area.
        // If endPoint height = 0, it likely means it's out of bounds
        if(startPoint.distanceTo(endPoint) / tileSize >= MAX_DRAW_DISTANCE)
        {
            return null;
        }

        // Hovered tile
        if (config.highlightHoveredTile() && tile != null)
        {
            endPoint = tile.getLocalLocation();
            renderTile(graphics, tile.getLocalLocation(), config.highlightHoveredColor(), config.hoveredTileBorderWidth(), config.hoveredTileFillColor());

            // Add label
            hoveredTileLabelMode labelMode = config.hoveredTileLabelModeSelect();
            if (labelMode != hoveredTileLabelMode.NONE) {
                String endLabel = constructHoveredTileString(labelMode, tile);
                addLabel(graphics, endPoint, 0, endLabel, config.hoveredTileLabelColor());
            }
        }

        // Line
        if (config.drawPathLine())
        {
            drawLine(graphics, startPoint, endPoint, config.pathLineColor(), (float) config.pathLineWidth());
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

    // Draw a line between the provided start and end points
    private void drawLine(final Graphics2D graphics, final LocalPoint startLoc, final LocalPoint endLoc, final Color color, float lineWidth){ //, int counter) {
        if (startPoint == null || endPoint == null) {
            return;
        }

        int z = client.getLocalPlayer().getWorldView().getPlane();

        final int startHeight = Perspective.getTileHeight(client, startPoint, z);
        final int endHeight = Perspective.getTileHeight(client, endPoint, z);

        Point p1 = Perspective.localToCanvas(client, startPoint.getX(), startPoint.getY(), startHeight);
        Point p2 = Perspective.localToCanvas(client, endPoint.getX(), endPoint.getY(), endHeight);

        if (p1 == null || p2 == null)
        {
            return;
        }

        Line2D.Double line = new Line2D.Double(p1.getX(), p1.getY(), p2.getX(), p2.getY());

        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(lineWidth));
        graphics.draw(line);

//        if (counter == 1) {
//            drawCounter(graphics, p1.getX(), p1.getY(), 0);
//        }
//        drawCounter(graphics, p2.getX(), p2.getY(), counter);
    }

    String constructHoveredTileString(hoveredTileLabelMode labelMode, Tile tile)
    {
        String returnString = "";
        switch (labelMode)
        {
            case TILE_REGION: returnString = getTileRegionString(tile); break;
            case TILE_LOCATION: returnString = getTileLocationString(tile); break;
            case DISTANCE: returnString = getTileDistanceString(startPoint, endPoint); break;
            case ALL: returnString = "R: " + getTileRegionString(tile) +
                    ", L: " + getTileLocationString(tile) +
                    ", D: " + getTileDistanceString(startPoint, endPoint); break;
        }
        return returnString;
    }

    String getTileLocationString(Tile tile)
    {
        return "( " + tile.getWorldLocation().getX() + ", " + tile.getWorldLocation().getY() + " )";
    }

    String getTileDistanceString(LocalPoint from,  LocalPoint to)
    {
        return from == null ? "" : String.valueOf((int) (from.distanceTo(to) / tileSize));
    }

    String getTileRegionString(Tile tile)
    {
        return String.valueOf(tile.getWorldLocation().getRegionID());
    }

    void addLabel(Graphics2D graphics, LocalPoint tileLoc, int zOffset, String labelText, Color color)
    {
        Point canvasTextLocation = Perspective.getCanvasTextLocation(client, graphics, tileLoc, labelText, zOffset);

        if (canvasTextLocation != null)
        {
            OverlayUtil.renderTextLocation(graphics, canvasTextLocation, labelText, color);
        }
    }
}
