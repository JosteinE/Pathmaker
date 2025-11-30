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

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import lombok.extern.slf4j.Slf4j; // https://projectlombok.org/features/log
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Slf4j
public class PathmakerOverlay extends Overlay
{
    private static final int MAX_DRAW_DISTANCE = 32;

    // The integer represents RegionID
    //private final ArrayList<PathPoint> tilesToHighlight = new ArrayList<>();

    private final Client client;
    private final PathmakerPlugin plugin;
    private final PathmakerConfig config;

    private final float tileSize = 128;

    private LocalPoint startPoint;
    private LocalPoint hoveredTile;


    // For ref: net.runelite.client.ui.overlay.OverlayRenderer

    @Inject
    private PathmakerOverlay(Client client, PathmakerPlugin plugin, PathmakerConfig config)//, WorldView worldview)
    {
        this.client = client;
        this.plugin = plugin;
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
        // Doing getWorldLocation instead of getLocalLocation, because world loc. is server-side.
        final WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
        LocalPoint playerPosLocal = playerPos == null ? null : LocalPoint.fromWorld(client, playerPos);
        startPoint = playerPosLocal == null ? startPoint : playerPosLocal;

        // Current tile
        if (config.highlightPlayerTile())
        {
            highlightTile(graphics, startPoint, config.highlightPlayerColor(), config.playerTileBorderWidth(), config.playerTileFillColor());
        }

        // Fetch hovered tile and if successful, assign it to endPoint
        WorldView wv = client.getTopLevelWorldView();//getLocalPlayer().getWorldView();

        // Highlight tiles marked by the right-click menu and draw lines between them
        drawPath(graphics, wv);

        // Fetch hovered tile
        Tile tile = wv.getSelectedSceneTile();
        hoveredTile = tile == null ? hoveredTile : tile.getLocalLocation();

        // Return here if the distance to hovered tile exceeds the user interactable area.
        // If endPoint height = 0, it likely means it's out of bounds
//        if(startPoint.distanceTo(endPoint) / tileSize >= MAX_DRAW_DISTANCE)
//        {
//            return null;
//        }

        // Hovered tile
        if (config.highlightHoveredTile() && tile != null && isRegionLoaded(tile.getWorldLocation().getRegionID()))
        {
            highlightTile(graphics, hoveredTile, config.highlightHoveredColor(), config.hoveredTileBorderWidth(), config.hoveredTileFillColor());

            // Add label
            String hoveredTileLabel = constructHoveredTileString(tile);
            if(!hoveredTileLabel.isEmpty())
            {
                addLabel(graphics, hoveredTile, 0, hoveredTileLabel, config.hoveredTileLabelColor());
            }
        }

        // Draw line to hovered line
        if(config.hoveredTileLineDrawModeSelect() == PathmakerConfig.hoveredTileLineDrawMode.ALWAYS
                || (config.hoveredTileLineDrawModeSelect() == PathmakerConfig.hoveredTileLineDrawMode.SHIFT_DOWN
                && plugin.hotKeyPressed))
        {
            // Set hover line to match the active path color if true
            Color hoverLineColor = config.hoverLineColorMatchPath() ?
                    plugin.getStoredPaths().get(plugin.getActivePathName()).color :
                    config.hoveredTileLineColor();

            switch (config.hoveredTileLineOriginSelect()) {
                case PATH_END: {

                    if (plugin.getStoredPaths().isEmpty())
                    {
                        break;
                    }
                    PathmakerPath activePath = plugin.getStoredPaths().get(plugin.getActivePathName());
                    PathPoint lastTile = activePath.getPointAtDrawIndex(activePath.getSize() - 1);

                    if (!activePath.isPointInRegions(lastTile, client.getTopLevelWorldView().getMapRegions()))
                    {
                        break;
                    }

                    drawLine(graphics, pathPointToLocal(wv, lastTile), hoveredTile, hoverLineColor, (float) config.pathLineWidth());
                    break;
                }
                case TRUE_TILE: {
                    drawLine(graphics, startPoint, hoveredTile, hoverLineColor, (float) config.pathLineWidth());
                    break;
                }
                default:
                    break;
            }
        }

        return null;
    }

    // Highlight tiles marked by the right-click menu and draw lines between them
    void drawPath(Graphics2D graphics, WorldView wv) {
        HashMap<String, PathmakerPath> paths = plugin.getStoredPaths();

        for (String pathName : paths.keySet())
        {
            PathmakerPath path = paths.get(pathName);
            int pathSize = path.getSize();

            ArrayList<Integer> loadedRegions = new ArrayList<>();
            for (int regionId : client.getTopLevelWorldView().getMapRegions())
            {
                loadedRegions.add(regionId);
            }

            for (int loadedRegionID : loadedRegions)
            {
                // Skip if path does not contain points in the given region
                if (!path.hasPointsInRegion(loadedRegionID)) {
                    continue;
                }

                ArrayList<PathPoint> drawOrder = paths.get(pathName).getDrawOrder(loadedRegions);
                if (drawOrder.isEmpty())
                {
                    log.debug("WorldView: {}", wv);
                    log.debug("Environment: {}", client.getEnvironment());
                    log.debug("IsInstance, {}", client.getTopLevelWorldView().isInstance());
                    log.debug("getDraw2DMask, {}", client.getDraw2DMask());
                }

                // Draw both line and points
                if (config.drawPath() && pathSize > 1 && config.drawPathPoints()) {
                    LocalPoint lastLocalP = null;
                    for (PathPoint point : drawOrder)
                    {
                        LocalPoint localP = pathPointToLocal(wv, point);
                        highlightTile(graphics, localP, config.pathLinePointColor(), config.pathLinePointWidth(), config.pathLinePointFillColor());
                        drawLine(graphics, lastLocalP, localP, path.color, (float) config.pathLineWidth());
                        lastLocalP = localP;
                    }

                    // Only draw line
                } else if (config.drawPath() && pathSize > 1 ) {
                    LocalPoint lastLocalP = null;
                    for (PathPoint point : drawOrder) {
                        LocalPoint localP = pathPointToLocal(wv, point);
                        drawLine(graphics, lastLocalP, localP, path.color, (float) config.pathLineWidth());
                        lastLocalP = localP;
                    }

                    // Only Draw points
                } else if (config.drawPathPoints()) {
                    for (PathPoint point : drawOrder) {
                        highlightTile(graphics, wv, point, config.pathLinePointColor(), config.pathLinePointWidth(), config.pathLinePointFillColor());
                    }
                }

                // Draw label
                if (config.drawPathPointLabel())
                {
                    for (PathPoint point : drawOrder)
                    {
                        addLabel(graphics, wv, point, 0, "p" + (point.getDrawIndex()+1), config.hoveredTileLabelColor());
                    }
                }
            }

            // Loop path
            if (path.loopPath && path.getSize() > 2 && config.drawPath())
            {
                // Making sure both ends are loaded
                if(path.isPointInRegions(path.getPointAtDrawIndex(path.getSize() -1), loadedRegions) &&
                        path.isPointInRegions(path.getPointAtDrawIndex(0), loadedRegions))
                {
                    PathPoint lastP = path.getPointAtDrawIndex(path.getSize() - 1);
                    PathPoint firstP = path.getPointAtDrawIndex(0);
                    drawLine(graphics, wv, lastP, firstP, path.color, (float) config.pathLineWidth());
                }
            }
        }
    }

    // Convert PathPoint (region point) to local
    LocalPoint pathPointToLocal(WorldView wv, PathPoint point)
    {
        WorldPoint wp = WorldPoint.fromRegion(point.getRegionId(), point.getX(), point.getY(), point.getZ());
        return LocalPoint.fromWorld(wv, wp);
    }

    boolean highlightTile(final Graphics2D graphics, final WorldView wv, final PathPoint point, final Color color, final double borderWidth, final Color fillColor)
    {
        return highlightTile(graphics, pathPointToLocal(wv, point), color, borderWidth, fillColor);
    }

    boolean highlightTile(final Graphics2D graphics, final LocalPoint tile, final Color color, final double borderWidth, final Color fillColor)
    {
        if (tile == null || !isLocalPointInScene(tile))
        {
            //log.debug("Failed to highlight tile, TILE is null.");
            return false;
        }

        final Polygon poly = Perspective.getCanvasTilePoly(client, tile);

        // poly will be null i the tile is within a loaded region, but outside the camera's frustum.
        if (poly == null)
        {
            //log.debug("Failed to highlight tile, POLY is null. LocalPoint wv: {}, x: {}, y: {}", tile.getWorldView(), tile.getX(),  tile.getY());
            return false;
        }

        OverlayUtil.renderPolygon(graphics, poly, color, fillColor, new BasicStroke((float) borderWidth));
        return true;
    }

    private void drawLine(final Graphics2D graphics, final WorldView wv, final PathPoint startPoint, final PathPoint endPoint, final Color color, float lineWidth)
    {
        LocalPoint lineStart = pathPointToLocal(wv, startPoint);
        LocalPoint lineEnd = pathPointToLocal(wv, endPoint);
        drawLine(graphics, lineStart, lineEnd, color, lineWidth);
    }

    // Draw a line between the provided start and end points
    private void drawLine(final Graphics2D graphics, final LocalPoint startLoc, final LocalPoint endLoc, final Color color, float lineWidth){ //, int counter) {
        if (startLoc == null || endLoc == null)
        {
            return;
        }

        int z = client.getLocalPlayer().getWorldView().getPlane();

        final int startHeight = Perspective.getTileHeight(client, startLoc, z);
        final int endHeight = Perspective.getTileHeight(client, endLoc, z);

        Point p1 = Perspective.localToCanvas(client, startLoc.getX(), startLoc.getY(), startHeight);
        Point p2 = Perspective.localToCanvas(client, endLoc.getX(), endLoc.getY(), endHeight);

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

    String constructHoveredTileString(Tile tile)
    {
        String returnString = "";
        switch (config.hoveredTileLabelModeSelect())
        {
            case TILE_REGION: returnString = getTileRegionString(tile); break;
            case TILE_LOCATION: returnString = getTileLocationString(tile); break;
            case DISTANCE: returnString = getTileDistanceString(startPoint, hoveredTile); break;
            case ALL: returnString = "R: " + getTileRegionString(tile) +
                    ", L: " + getTileLocationString(tile) +
                    ", D: " + getTileDistanceString(startPoint, hoveredTile); break;
            default: break;
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


    boolean addLabel(Graphics2D graphics, WorldView wv, PathPoint point, int zOffset, String labelText, Color color)
    {
        return addLabel(graphics, pathPointToLocal(wv, point), zOffset, labelText, color);
    }

    boolean addLabel(Graphics2D graphics, LocalPoint tileLoc, int zOffset, String labelText, Color color)
    {
        if (tileLoc == null || !isLocalPointInScene(tileLoc))
            return false;

        Point canvasTextLocation = Perspective.getCanvasTextLocation(client, graphics, tileLoc, labelText, zOffset);

        if (canvasTextLocation != null)
        {
            OverlayUtil.renderTextLocation(graphics, canvasTextLocation, labelText, color);
            return true;
        }
        return false;
    }

    boolean isLocalPointInScene(final LocalPoint point)
    {
        WorldPoint wp = WorldPoint.fromLocal(client, point);
        return WorldPoint.isInScene(client.getTopLevelWorldView(), wp.getX(), wp.getY());
    }

    boolean isRegionLoaded(int regionId)
    {
        for(int loadedRegionID : client.getTopLevelWorldView().getMapRegions())
        {
            if (loadedRegionID == regionId)
            {
                return true;
            }
        }
        return false;
    }
}
