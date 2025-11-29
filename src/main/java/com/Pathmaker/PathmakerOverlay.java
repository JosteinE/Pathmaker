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
    private LocalPoint endPoint;


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
        final WorldPoint playerPos = client.getLocalPlayer().getWorldLocation(); // doing getWorldLocation instead of getLocalLocation, because world loc. is server-side.
        LocalPoint playerPosLocal = playerPos == null ? null : LocalPoint.fromWorld(client, playerPos);
        startPoint = playerPosLocal == null ? startPoint : playerPosLocal;

        // Current tile
        if (config.highlightPlayerTile())
        {
            highlightTile(graphics, startPoint, config.highlightPlayerColor(), config.playerTileBorderWidth(), config.playerTileFillColor());
        }

        // Fetch hovered tile and if successful, assign it to endPoint
        WorldView wv = client.getLocalPlayer().getWorldView();

        // Highlight tiles marked by the right-click menu and draw lines between them
        drawPath(graphics, wv);

        // Fetch hovered tile and if successful, assign it to endPoint
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
            highlightTile(graphics, tile.getLocalLocation(), config.highlightHoveredColor(), config.hoveredTileBorderWidth(), config.hoveredTileFillColor());
        }

        // Add label to hovered tile
        if (tile != null)
        {
            String endLabel = constructHoveredTileString(tile);
            addLabel(graphics, endPoint, 0, endLabel, config.hoveredTileLabelColor());
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
                    PathPoint lastTile = activePath.getPointAtIndex(activePath.getSize() - 1);

                    if (!activePath.isPointInRegions(lastTile, client.getTopLevelWorldView().getMapRegions()))
                    {
                        break;
                    }

                    drawLine(graphics, pathPointToLocal(wv, lastTile), endPoint, hoverLineColor, (float) config.pathLineWidth());
                    break;
                }
                case TRUE_TILE: {
                    drawLine(graphics, startPoint, endPoint, hoverLineColor, (float) config.pathLineWidth());
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

                // Draw both line and points
                if (config.drawPath() && pathSize > 1 && config.drawPathPoints()) {
                    LocalPoint lastLocalP = null;
                    for (PathPoint point : drawOrder) {
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
                        LocalPoint localP = pathPointToLocal(wv, point);
                        highlightTile(graphics, localP, config.pathLinePointColor(), config.pathLinePointWidth(), config.pathLinePointFillColor());
                    }
                }

                // Draw label
                if (config.drawPathPointLabel())
                {
                    for (PathPoint point : drawOrder)
                    {
                        LocalPoint localP = pathPointToLocal(wv, point);
                        addLabel(graphics, localP, 0, "p" + (point.getIndex()+1), config.hoveredTileLabelColor());
                    }
                }
            }

            // Loop path
            if (path.loopPath && path.getSize() > 2 && config.drawPath())
            {
                // Making sure both ends are loaded
                if(path.isPointInRegions(path.getPointAtIndex(path.getSize() -1), loadedRegions) &&
                        path.isPointInRegions(path.getPointAtIndex(0), loadedRegions))
                {
                    LocalPoint lastP = pathPointToLocal(wv, path.getPointAtIndex(path.getSize() - 1));
                    LocalPoint firstP = pathPointToLocal(wv, path.getPointAtIndex(0));
                    drawLine(graphics, lastP, firstP, path.color, (float) config.pathLineWidth());
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

    void highlightTile(final Graphics2D graphics, final LocalPoint tile, final Color color, final double borderWidth, final Color fillColor)
    {
        if (tile == null)
        {
            log.debug("Failed to highlight tile, TILE is null.");
            return;
        }

        final Polygon poly = Perspective.getCanvasTilePoly(client, tile);

        // poly will be null i the tile is within a loaded region, but outside the camera's frustum.
        if (poly == null)
        {
            //log.debug("Failed to highlight tile, POLY is null. LocalPoint wv: {}, x: {}, y: {}", tile.getWorldView(), tile.getX(),  tile.getY());
            return;
        }

        OverlayUtil.renderPolygon(graphics, poly, color, fillColor, new BasicStroke((float) borderWidth));
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
            case DISTANCE: returnString = getTileDistanceString(startPoint, endPoint); break;
            case ALL: returnString = "R: " + getTileRegionString(tile) +
                    ", L: " + getTileLocationString(tile) +
                    ", D: " + getTileDistanceString(startPoint, endPoint); break;
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

    void addLabel(Graphics2D graphics, LocalPoint tileLoc, int zOffset, String labelText, Color color)
    {
        Point canvasTextLocation = Perspective.getCanvasTextLocation(client, graphics, tileLoc, labelText, zOffset);

        if (canvasTextLocation != null)
        {
            OverlayUtil.renderTextLocation(graphics, canvasTextLocation, labelText, color);
        }
    }

//    // HashMap<String, PathmakerPath> paths
//    void importTilesToHighlight(Collection<PathPoint> pathPoints)
//    {
//        tilesToHighlight.clear();
//        for (PathPoint point : pathPoints)
//        {
////            if(!tilesToHighlight.contains(point))
////            {
//                tilesToHighlight.add(point);
////            }
//        }
        //log.debug("Number of tiles to highlight: {}", tilesToHighlight.size());
        //tilesToHighlight.addAll(pathPoints);
   // }
}
