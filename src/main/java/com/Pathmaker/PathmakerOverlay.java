package com.Pathmaker;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.text.html.parser.Entity;
import net.runelite.api.GameObject;
import net.runelite.api.MenuEntry;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Client;
import java.awt.geom.Point2D.Float;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.SceneTileModel;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;                  // For player position
import net.runelite.api.coords.WorldPoint;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import lombok.extern.slf4j.Slf4j; // https://projectlombok.org/features/log
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.http.api.worlds.World;


@Slf4j
public class PathmakerOverlay extends Overlay
{
    private static final int MAX_DRAW_DISTANCE = 32;

    private final Client client;
    private final PathmakerPlugin plugin;
    private final PathmakerConfig config;
    private final ModelOutlineRenderer modelOutlineRenderer;

    private final float tileSize = 128;

    private LocalPoint startPoint;
    private LocalPoint hoveredTile;

    @Inject
    private PathmakerOverlay(Client client, PathmakerPlugin plugin, PathmakerConfig config, ModelOutlineRenderer modelOutlineRenderer)//, WorldView worldview)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.modelOutlineRenderer = modelOutlineRenderer;

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
		WorldView pWv = client.getLocalPlayer().getWorldView();

        final WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
		LocalPoint playerPosLocal = playerPos == null ? null : LocalPoint.fromWorld(pWv, new WorldPoint(playerPos.getX(), playerPos.getY(), playerPos.getPlane()));

		startPoint = LocalPoint.fromWorld(pWv,client.getLocalPlayer().getWorldLocation());//playerPosLocal == null ? startPoint : playerPosLocal;

        // Current tile
        if (config.highlightPlayerTile())
        {
			Color trueTileColor = config.highlightPlayerColor();
			Color trueTileFillColor = getTileFillColor(trueTileColor);

			highlightTile(graphics, pWv, startPoint, trueTileColor, config.playerTileBorderWidth(), trueTileFillColor);
        }

//        // Fetch hovered tile and if successful, assign it to endPoint
//        WorldView wv = client.getTopLevelWorldView();//getLocalPlayer().getWorldView();

//		wv = client.getTopLevelWorldView();

		LocalPoint lastActivePathPoint = null;
        // Highlight tiles marked by the right-click menu and draw lines between them
        if(!plugin.getStoredPaths().isEmpty())
        {
			lastActivePathPoint = drawPath(graphics);
        }

		// Draw hovered tile elements
		if(!isMouseOverHud() &&
			(config.hoveredTileDrawModeSelect() == PathmakerConfig.hoveredTileDrawMode.ALWAYS ||
			(config.hoveredTileDrawModeSelect() == PathmakerConfig.hoveredTileDrawMode.SHIFT_DOWN &&
				plugin.hotKeyPressed)))
		{
			drawHoveredTile(graphics, lastActivePathPoint);
		}

        return null;
    }

	boolean isMouseOverHud()
	{
		MenuEntry[] menuEntries = client.getMenu().getMenuEntries();
		int last = menuEntries.length - 1;

		if (last < 0)
		{
			return false;
		}

		MenuEntry menuEntry = menuEntries[last];
		String option = menuEntry.getOption();

		return menuEntry.getWidget() != null || option.equals("Cancel"); // STANDARD_HUD_INTERFACES.contains(widgetGroupId)
	}

    void drawHoveredTile(Graphics2D graphics, @Nullable LocalPoint lastPathPoint)
    {
        // Fetch hovered tile
		WorldView wv = client.getLocalPlayer().getWorldView();
        Tile tile = wv.getSelectedSceneTile();

        // Set hovered tile to be last hovered tile if none is found
        if(tile == null)
		{
			wv = client.getTopLevelWorldView();
			tile = wv.getSelectedSceneTile();

			if (tile == null) return;
		}
        //hoveredTile = tile == null ? hoveredTile : tile.getLocalLocation();
        hoveredTile = tile.getLocalLocation();

        // Return here if the distance to hovered tile exceeds the user interactable area.
        // If endPoint height = 0, it likely means it's out of bounds
//        if (startPoint.distanceTo(hoveredTile) / tileSize >= MAX_DRAW_DISTANCE)
//		{
//            return;
//        }

		Color hoveredTileColor;

		if(config.hoverLineColorMatchPath())
		{
			hoveredTileColor = plugin.pathExists(plugin.getActivePathName()) ?
				plugin.getStoredPaths().get(plugin.getActivePathName()).color :
				config.pathColor();
		}
		else
		{
			hoveredTileColor = config.highlightHoveredColor();
		}

		Color hoveredTileFillColor = getTileFillColor(hoveredTileColor);

        // Highlight hovered tile
        if (config.highlightHoveredTile())
		{
			highlightTile(graphics, wv, hoveredTile, hoveredTileColor, config.hoveredTileBorderWidth(), hoveredTileFillColor);
        }

        // Add label
        if (config.hoveredTileLabelModeSelect() != PathmakerConfig.hoveredTileLabelMode.NONE) {
            String hoveredTileLabel = constructHoveredTileString(wv, tile);
            if (!hoveredTileLabel.isEmpty()) {
                addLabel(graphics, wv, hoveredTile, 0, hoveredTileLabel, config.hoveredTileLabelColor());
            }
        }

        // Draw line to hovered line
        // Set hover line to match the active path color if true
        if(config.drawHoverLine())
        {
            switch (config.hoveredTileLineOriginSelect()) {
                case PATH_END: {
                    if (!plugin.pathExists((plugin.getActivePathName()))) {
                        break;
                    }
                    PathmakerPath activePath = plugin.getStoredPaths().get(plugin.getActivePathName());
                    PathPoint lastPoint = activePath.getPointAtDrawIndex(activePath.getSize() - 1);
					//WorldView lastWv = client.getWorldView(lastPoint.getWorldViewId());

                    if (!activePath.isPointInRegions(lastPoint, client.getTopLevelWorldView().getMapRegions()))
                    {
                        break;
                    }

					if (lastPathPoint == null)
					{
						lastPathPoint = pathPointToLocal(wv,lastPoint);//lastWv, lastPoint);
						if(lastPathPoint == null) break;

						//Set line origin to be in the center of objects
						if (lastPoint instanceof PathPointObject)
						{
							lastPathPoint = lastPathPoint.dx(((PathPointObject) lastPoint).getToCenterVectorX());
							lastPathPoint = lastPathPoint.dy(((PathPointObject) lastPoint).getToCenterVectorY());
						}
					}

                    drawLine(graphics, lastPathPoint, hoveredTile, wv, wv, hoveredTileColor, (float) config.pathLineWidth());
                    break;
                }
                case TRUE_TILE: {
                    drawLine(graphics, startPoint, hoveredTile, client.getLocalPlayer().getWorldView(), wv, hoveredTileColor, (float) config.pathLineWidth());
                    break;
                }
                default:
                    break;
            }
        }
    }

    // Highlight tiles marked by the right-click menu and draw lines between them
    LocalPoint drawPath(Graphics2D graphics)
    {
		if (plugin.getStoredPaths().isEmpty()) return null;
        HashMap<String, PathmakerPath> paths = plugin.getStoredPaths();
		ArrayList<Integer> loadedRegions = new ArrayList<>();
		String activePathName = plugin.getActivePathName();
		LocalPoint lastActivePathPoint = null;

		for (int regionId : client.getTopLevelWorldView().getMapRegions())
		{
			loadedRegions.add(regionId);
		}

		if (client.getLocalPlayer().getWorldView().isInstance())
		{
			// needed for cox
			int playerRegion = client.getLocalPlayer().getWorldLocation().getRegionID();
			if (!loadedRegions.contains(playerRegion)) loadedRegions.add(playerRegion);

			// needed for sailboat
			WorldPoint baseInstancePoint = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation());
			if (!loadedRegions.contains(baseInstancePoint.getRegionID()))
				loadedRegions.add(baseInstancePoint.getRegionID());

			//log.debug("playerRegion: {}, BaseInstanceReg: {}", playerRegion, baseInstancePoint.getRegionID());
		}

        for (String pathName : paths.keySet())
        {
			//LocalPoint lastLocalP = null;
			//WorldView lastWv = client.getTopLevelWorldView();
            PathmakerPath path = paths.get(pathName);
			ArrayList<LocalPoint> line = new ArrayList<>();
			ArrayList<WorldView> lineWVs = new ArrayList<>();

            if(path.hidden)
            {
                continue;
            }

            int pathSize = path.getSize();

            ArrayList<PathPoint> drawOrder = paths.get(pathName).getDrawOrder(loadedRegions);

            if (config.drawPath() || config.drawPathPoints())
            {
				Color pathPointColor = config.pointMatchPathColor() ? path.color : config.pathLinePointColor();
				Color pathPointFillColor = getTileFillColor(pathPointColor);

                for (int i = 0; i < drawOrder.size(); i++)
				{
					PathPoint point = drawOrder.get(i);
					WorldPoint wp = toLocalInstance(point.getWorldPoint(), loadedRegions);
					WorldView wv;
					if (wp == null)
					{
						wp = point.getWorldPoint();
						wv = client.getTopLevelWorldView();
					}
					else
						wv = client.getLocalPlayer().getWorldView();

                    LocalPoint localP = LocalPoint.fromWorld(wv, wp);//pathPointToLocal(wv, point);
                    //LocalPoint centerLocation;
                    // Draw outlines first, as this also lets us conveniently update the stored point locations
                    if(point instanceof PathPointObject)
                    {
						// Updating NPC world positions AND fetching current client side position to draw on

						boolean isNpc = ((PathPointObject) point).isNpc();
						int entityId = ((PathPointObject) point).getEntityId();


						localP = getCurrentEntityPosition(wv, point, true);

						if(localP != null)
						{
							if(config.objectAndNpcOutline())
								drawOutline(wv, localP, isNpc, entityId, config.objectAndNpcOutlineWidth(), path.color, 200);

							if (config.drawPathPoints())
							{
								highlightTile(graphics, wv, plugin.getEntityPolygon(wv, localP, isNpc, entityId), pathPointColor, config.pathLinePointWidth(), pathPointFillColor);
							}

							localP = toEntityCenter((PathPointObject) point, localP);
//							localP = localP.dx(((PathPointObject) point).getToCenterVectorX());
//							localP = localP.dy(((PathPointObject) point).getToCenterVectorY());
						}

						drawLabel(graphics, wv, localP, point.getDrawIndex(), point.getLabel(), path.color);
                    }
					else if(config.drawPathPoints()) // Draw non-entity tile highlights
					{
						highlightTile(graphics, wv, localP, pathPointColor, config.pathLinePointWidth(), pathPointFillColor);
						//drawLabel(graphics, wv, point, path.color);
					}

                    // Only draw line if the previous point had a draw index that was directly behind this.
                   if ((config.drawPath()))// && pathSize > 1) && i > 0 && drawOrder.get(i - 1).getDrawIndex() == point.getDrawIndex() - 1)
				   {
						   lineWVs.add(wv);
						   line.add(localP);//drawLine(graphics, lastLocalP, localP, lastWv, wv, path.color, (float) config.pathLineWidth());
				   }

					drawLabel(graphics, wv, localP, point.getDrawIndex(), point.getLabel(), path.color);
                    //lastLocalP = localP;
					//lastWv = wv;
                }

            }

            // Loop path
            if (path.loopPath &&
				path.getSize() > 2 &&
				config.drawPath() &&
				path.isPointInRegions(path.getPointAtDrawIndex(path.getSize() -1), loadedRegions) &&
				path.isPointInRegions(path.getPointAtDrawIndex(0), loadedRegions))
            {
				line.add(line.get(0));
				lineWVs.add(lineWVs.get(0));
//                // Making sure both ends are loaded
//                if(path.isPointInRegions(path.getPointAtDrawIndex(path.getSize() -1), loadedRegions) &&
//                        path.isPointInRegions(path.getPointAtDrawIndex(0), loadedRegions))
//                {
//					PathPoint startP = path.getPointAtDrawIndex(0);
//
//					WorldPoint startWp = toLocalInstance(startP.getWorldPoint(), loadedRegions);
//					WorldView startWv;
//					if(startWp != null)
//						startWv = client.getLocalPlayer().getWorldView();
//					else
//					{
//						startWv = client.getTopLevelWorldView();
//						//startWp = startP.getWorldPoint();
//					}
//
//					LocalPoint startLp = getCurrentEntityPosition(startWv, startP, false);
//
//					if(startP instanceof PathPointObject)
//						startLp = toEntityCenter((PathPointObject) startP, startLp);
//
//					//WorldView startWv = client.getWorldView(startP.getWorldViewId());
//					//LocalPoint startLp = pathPointToLocal(wv, path.getPointAtDrawIndex(0));
//
////                    PathPoint lastP = path.getPointAtDrawIndex(path.getSize() - 1);
//
//                    drawLine(graphics, lastLocalP, startLp, lastWv, startWv, path.color, (float) config.pathLineWidth());
//                }
            }

			if (line.size() > 1)
			{
				WorldView wv = client.getTopLevelWorldView();
				int LOCAL_HALF_TILE_SIZE = Perspective.LOCAL_HALF_TILE_SIZE;
				boolean isOnBoat = client.getLocalPlayer().getWorldView().getId() != -1;

				if (path.pathDrawOffset != PathPanel.pathDrawOffset.OFFSET_MIDDLE.ordinal())
				{
					ArrayList<int[]> tileXs = new ArrayList<>();
					ArrayList<int[]> tileYs = new ArrayList<>();
					boolean buildLeft = path.pathDrawOffset == PathPanel.pathDrawOffset.OFFSET_LEFT.ordinal();
					for (int i = 0; i < line.size(); i++)
					{
						LocalPoint lp = line.get(i);
						if (lp == null) continue;

						Tile tile = plugin.getTile(wv, lp.getSceneX(), lp.getSceneY());
						if (tile == null) continue;

						int[] tileX = new int[4];
						tileX[0] = lp.getX() - LOCAL_HALF_TILE_SIZE;
						tileX[1] = lp.getX() + LOCAL_HALF_TILE_SIZE;
						tileX[2] = lp.getX() + LOCAL_HALF_TILE_SIZE;
						tileX[3] = lp.getX() - LOCAL_HALF_TILE_SIZE;

						int[] tileY = new int[4];
						tileY[0] = lp.getY() + LOCAL_HALF_TILE_SIZE;
						tileY[1] = lp.getY() + LOCAL_HALF_TILE_SIZE;
						tileY[2] = lp.getY() - LOCAL_HALF_TILE_SIZE;
						tileY[3] = lp.getY() - LOCAL_HALF_TILE_SIZE;

						tileXs.add(tileX);
						tileYs.add(tileY);
					}

					ArrayList<LocalPoint> lineVertices = PathTileOutline.build(lineWVs, tileXs, tileYs, buildLeft);

					for(int i = 1; i < lineVertices.size(); i ++)
					{
						LocalPoint startLp = lineVertices.get(i-1);
						LocalPoint endLp = lineVertices.get(i);

						if(startLp == null || endLp == null) continue;

						drawLine(graphics, startLp,endLp, wv, wv, path.color, (float) config.pathLineWidth());
					}
				}
				else
				{
					for (int i = 1; i < line.size(); i++)
					{
						drawLine(graphics,
							line.get(i - 1),
							line.get(i),
							lineWVs.get(i - 1),
							lineWVs.get(i),
							path.color,
							(float) config.pathLineWidth());
					}
				}
			}
        }
		return  lastActivePathPoint;
    }

	LocalPoint toEntityCenter(PathPointObject point, LocalPoint localPoint)
	{
		localPoint = localPoint.dx(point.getToCenterVectorX());
		localPoint = localPoint.dy(point.getToCenterVectorY());
		return localPoint;
	}

//	int correctPlaneForSailing(WorldView wv)
//	{
//		return wv.isTopLevel() ? wv.getPlane() : 0;
//	}

    // Convert PathPoint (region point) to local
    LocalPoint pathPointToLocal(WorldView wv, PathPoint point)
    {
        WorldPoint wp = WorldPoint.fromRegion(point.getRegionId(), point.getX(), point.getY(), point.getZ());
        return LocalPoint.fromWorld(wv, wp);
    }

    boolean highlightTile(final Graphics2D graphics, final WorldView wv, final PathPoint point, final Color color, final double borderWidth, final Color fillColor)
    {
        return highlightTile(graphics, wv, pathPointToLocal(wv, point), color, borderWidth, fillColor);
    }

    boolean highlightTile(final Graphics2D graphics, final WorldView wv, final LocalPoint lp, final Color color, final double borderWidth, final Color fillColor)
    {
        if (lp == null)// || !isLocalPointInScene(lp))
        {
            // Occurs on unload
            //log.debug("Failed to highlight tile, LocalPoint is null.");
            return false;
        }
        return highlightTile(graphics, wv, Perspective.getCanvasTilePoly(client, lp), color, borderWidth, fillColor);
    }

    boolean highlightTile(final Graphics2D graphics, final WorldView wv, final Polygon poly, final Color color, final double borderWidth, final Color fillColor)
    {
        // poly will be null i the tile is within a loaded region, but outside the camera's frustum or not loaded (i.e. despawning npcs)
        if (poly == null) return false;

        int boundsX = (int) poly.getBounds().getLocation().getX();
        int boundsY = (int) poly.getBounds().getLocation().getY();

        if(!isLocalPointInScene(wv, new LocalPoint(boundsX, boundsY, wv))) return false;


        OverlayUtil.renderPolygon(graphics, poly, color, fillColor, new BasicStroke((float) borderWidth));
        return true;
    }

//    private void drawLine(final Graphics2D graphics, final WorldView wv, final PathPoint startPoint, final PathPoint endPoint, final Color color, float lineWidth)
//    {
//        LocalPoint lineStart = pathPointToLocal(wv, startPoint);
//        LocalPoint lineEnd = pathPointToLocal(wv, endPoint);
//
//		// Correct for sailing tiles
//
//        drawLine(graphics, lineStart, lineEnd, wv, color, lineWidth);
//    }

    // Draw a line between the provided start and end points
    private void drawLine(final Graphics2D graphics, final LocalPoint startLoc, final LocalPoint endLoc, WorldView startWv, WorldView endWv, final Color color, float lineWidth){ //, int counter) {
        if (startLoc == null || endLoc == null)
        {
            return;
        }

        final int startHeight = Perspective.getTileHeight(client, startLoc, startWv.getPlane());
        final int endHeight = Perspective.getTileHeight(client, endLoc, endWv.getPlane());

        Point p1 = Perspective.localToCanvas(client, startLoc.getWorldView(), startLoc.getX(), startLoc.getY(), startHeight - config.pathZOffset() * 10);
        Point p2 = Perspective.localToCanvas(client, endLoc.getWorldView(), endLoc.getX(), endLoc.getY(), endHeight - config.pathZOffset() * 10);

        if (p1 == null || p2 == null)
        {
            return;
        }

        Line2D.Double line = new Line2D.Double(p1.getX(), p1.getY(), p2.getX(), p2.getY());

        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(lineWidth));
        graphics.draw(line);
    }

    // 0 tileObject, 1 npc
    void drawOutline(WorldView wv, LocalPoint lp, Boolean isNpc, int entityId, int width, Color color, int feather)
    {
//        if (point == null) {return;}

        if(isNpc)
        {
            NPC npc = wv.npcs().byIndex(entityId);
            if(npc == null){return;}

            modelOutlineRenderer.drawOutline(npc,width,color,feather);
        }
        else
        {
            TileObject tileObject = plugin.getTileObject(wv, lp, entityId);
            if(tileObject == null){return;}
            modelOutlineRenderer.drawOutline(tileObject,width,color,feather);
        }
    }

	LocalPoint getCurrentEntityPosition(WorldView wv, PathPoint p, boolean updateMovable)
    {
		if (!(p instanceof PathPointObject)) return LocalPoint.fromWorld(wv, p.getWorldPoint());

		PathPointObject entity = (PathPointObject) p;
		LocalPoint lp = null;

        if(entity.isNpc())
        {
            NPC npc = wv.npcs().byIndex(entity.getEntityId());

			if(npc == null)
			{
				for (NPC localNpc : wv.npcs())
				{
					if (localNpc.getId() == entity.getBaseId())
					{
						npc = localNpc;

						if(updateMovable)
						{
							entity.setEntityId(localNpc.getIndex());

							Point toCenterVec = plugin.getEntityToCenterVector(wv, npc.getWorldLocation(), localNpc.getId(), true);
							entity.setToCenterVector(toCenterVec.getX(), toCenterVec.getY());
						}

						break;
					}
				}
			}

            if(npc != null)
			{
				if(updateMovable)
				{
					final WorldPoint worldNpc = WorldPoint.fromLocalInstance(wv.getScene(), npc.getLocalLocation(), wv.getPlane());

					// Update the stored belonging PathPoint
					plugin.updatePointLocation(
						entity.getPathOwnerName(),
						entity,
						worldNpc.getRegionID(),
						worldNpc.getRegionX(),
						worldNpc.getRegionY(),
						worldNpc.getPlane());
				}

				lp = npc.getLocalLocation();
			}
        }
		else if (wv.getScene().isInstance())
		{
			Collection<WorldPoint> iWps = WorldPoint.toLocalInstance(wv, entity.getWorldPoint());

			if (!iWps.isEmpty())
			{
				WorldPoint wp  = iWps.iterator().next();
				lp = LocalPoint.fromWorld(wv, wp);

				if (lp != null && updateMovable)
				{
					Point toCenterVec = plugin.getEntityToCenterVector(wv, wp, entity.getEntityId(), false);
					entity.setToCenterVector(toCenterVec.getX(), toCenterVec.getY());
				}
			}
		}

		if (lp == null)
		{
			lp = LocalPoint.fromWorld(wv, entity.getWorldPoint());
		}

		return lp;
    }

	// (point.getDrawIndex() + 1)
	void drawLabel(Graphics2D graphics, WorldView wv, LocalPoint lp, int drawIndex, @Nullable String pointLabel, Color pathColor)
	{
		Color color = config.labelMatchPathColor() ? pathColor : config.pathPointLabelColor();

		String label = "";
		boolean stringEmpty = pointLabel == null || pointLabel.isEmpty();

		switch (config.pathPointLabelModeSelect())
		{
			case NONE:
				return;
			case BOTH:
			{
				label = "p" + (drawIndex + 1) + (stringEmpty ? "" : (", " + pointLabel));
				break;
			}
			case INDEX:
			{
				label = "p" + (drawIndex + 1);
				break;
			}
			case LABEL:
			{
				if(stringEmpty) return;
				label = pointLabel;
				break;
			}
		}

		addLabel(graphics, wv, lp, config.labelZOffset() * 10, label, color);
	}

//    void drawLabel(Graphics2D graphics, WorldView wv, PathPoint point, Color pathColor)//ArrayList<PathPoint> drawOrder, Color pathColor)
//    {
//		Color color = config.labelMatchPathColor() ? pathColor : config.pathPointLabelColor();
//
//		LocalPoint lp = pathPointToLocal(wv, point);
//		if(lp == null) return;
//
//		if(point instanceof PathPointObject)
//		{
//			lp = lp.dx(((PathPointObject) point).getToCenterVectorX());
//			lp = lp.dy(((PathPointObject) point).getToCenterVectorY());
//		}
//
//		drawLabel(graphics, wv, lp, point.getDrawIndex(), point.getLabel(), pathColor);

		// Draw label. Yes the split of loops here is intentional. More performant? Hopefully
//            switch (config.pathPointLabelModeSelect()) {
//                case INDEX: {
//                    for (PathPoint point : drawOrder)
//                    {
//                        LocalPoint lp = pathPointToLocal(wv, point);
//						if(lp == null) continue;
//
//                        if(point instanceof PathPointObject)
//                        {
//                            lp = lp.dx(((PathPointObject) point).getToCenterVectorX());//plugin.getEntityCenter(((PathPointObject) point).getInradius(), lp);
//                            lp = lp.dy(((PathPointObject) point).getToCenterVectorY());
//                        }
//
//                        addLabel(graphics, wv, lp, config.labelZOffset(), "p" + (point.getDrawIndex() + 1), color);
//                    }
//                    break;
//                }
//                case LABEL: {
//                    for (PathPoint point : drawOrder)
//                    {
//                        LocalPoint lp = pathPointToLocal(wv, point);
//						if(lp == null) continue;
//
//                        if(point instanceof PathPointObject)
//                        {
//                            lp = lp.dx(((PathPointObject) point).getToCenterVectorX());
//                            lp = lp.dy(((PathPointObject) point).getToCenterVectorY());
//                        }
//
//                        if (point.getLabel() != null && !point.getLabel().isEmpty())
//                            addLabel(graphics, wv, lp, config.labelZOffset(), point.getLabel(), color);
//                    }
//                    break;
//                }
//                case BOTH:
//                {
//                    for (PathPoint point : drawOrder)
//                    {
//                        LocalPoint lp = pathPointToLocal(wv, point);
//						if(lp == null) continue;
//
//                        if(point instanceof PathPointObject)
//                        {
//                            lp = lp.dx(((PathPointObject) point).getToCenterVectorX());
//                            lp = lp.dy(((PathPointObject) point).getToCenterVectorY());
//                        }
//
//                        String label = "p" + (point.getDrawIndex() + 1);
//                        if (point.getLabel() != null && !point.getLabel().isEmpty())
//                            label += ", " + point.getLabel();
//
//                        addLabel(graphics, wv, lp, config.labelZOffset() * 10, label, color);
//                    }
//                    break;
//                }
//                default:
//                    break;
//            }
//    }

	WorldPoint toLocalInstance(WorldPoint wp, ArrayList<Integer> loadedRegions)
	{
		if (client.getLocalPlayer().getWorldView().isInstance())// && !loadedRegions.contains(wp.getRegionID()))
		{
			Collection<WorldPoint> wPs = WorldPoint.toLocalInstance(client.getLocalPlayer().getWorldView(), wp);
			for (WorldPoint iWp : wPs)
			{
				if(loadedRegions.contains(iWp.getRegionID()))
				{
					return iWp;
				}
			}
		}
		return null;
	}

    String constructHoveredTileString(WorldView wv, Tile tile)
    {
        String returnString = "";
        switch (config.hoveredTileLabelModeSelect())
        {
            case REGION:
				returnString = getTileRegionString(tile);
				break;

            case LOCATION:
				returnString = getTileLocationString(tile);
				break;

            case OFFSET:
				if(config.hoveredTileLineOriginSelect() == PathmakerConfig.hoveredTileLineOrigin.PATH_END &&
					!plugin.getStoredPaths().isEmpty() && plugin.getStoredPaths().containsKey(plugin.getActivePathName()) &&
					isLocalPointInScene(wv, getLastPointInActivePath()))
				{
					returnString = getTileOffsetString(hoveredTile, getLastPointInActivePath());
				}
				else if (config.hoveredTileLineOriginSelect() == PathmakerConfig.hoveredTileLineOrigin.TRUE_TILE)
				{
					returnString = getTileOffsetString(hoveredTile, startPoint);
				}
                break;

            case DISTANCE:
				if(config.hoveredTileLineOriginSelect() == PathmakerConfig.hoveredTileLineOrigin.PATH_END &&
					!plugin.getStoredPaths().isEmpty() && plugin.getStoredPaths().containsKey(plugin.getActivePathName()) &&
					isLocalPointInScene(wv, getLastPointInActivePath()))
				{
					returnString = getTileDistanceString(getLastPointInActivePath(), hoveredTile);
				}
				else if (config.hoveredTileLineOriginSelect() == PathmakerConfig.hoveredTileLineOrigin.TRUE_TILE)
				{
					returnString = getTileDistanceString(hoveredTile, startPoint);
				}

				break;

            case ALL:
				// Region & Location
				returnString = "R: " + getTileRegionString(tile) + ", L: " + getTileLocationString(tile);

				// Offset & Distance
				if(config.hoveredTileLineOriginSelect() == PathmakerConfig.hoveredTileLineOrigin.PATH_END &&
					!plugin.getStoredPaths().isEmpty() && plugin.getStoredPaths().containsKey(plugin.getActivePathName()) &&
					isLocalPointInScene(wv, getLastPointInActivePath()))
				{
					returnString += ", O: " + getTileOffsetString(hoveredTile, getLastPointInActivePath());
					returnString += ", D: " + getTileDistanceString(getLastPointInActivePath(), hoveredTile);
				}
				else if (config.hoveredTileLineOriginSelect() == PathmakerConfig.hoveredTileLineOrigin.TRUE_TILE)
				{
					returnString += ", O: " + getTileOffsetString(hoveredTile, startPoint);
					returnString += ", D: " + getTileDistanceString(startPoint, hoveredTile);
				}
				break;

            default:
				break;
        }

        return returnString;
    }

    LocalPoint getLastPointInActivePath()
    {
        PathmakerPath activePath = plugin.getStoredPaths().get(plugin.getActivePathName());
        PathPoint lastPoint = activePath.getPointAtDrawIndex(activePath.getSize() - 1);
        WorldPoint wp = WorldPoint.fromRegion(lastPoint.getRegionId(),lastPoint.getX(), lastPoint.getY(), lastPoint.getZ());
        return LocalPoint.fromWorld(client.getTopLevelWorldView(), wp);
    }

    String getTileLocationString(Tile tile)
    {
        return "( " + tile.getWorldLocation().getX() + ", " + tile.getWorldLocation().getY() + " )";
    }

    String getTileOffsetString(LocalPoint start, LocalPoint end)
    {
        return "( " + (int) ((start.getX() - end.getX()) / tileSize) + ", " + (int) ((start.getY() - end.getY()) / tileSize) + " )";
    }

    String getTileDistanceString(LocalPoint from,  LocalPoint to)
    {
		if(from == null || to == null) return "";

		int x = Math.abs(to.getX() - from.getX());
		int y = Math.abs(to.getY() - from.getY());


        return String.valueOf((int) ((Math.max(x, y))/ tileSize)); // distance to is 1.414 to diagonal tiles
    }

	Color getTileFillColor(Color tileColor)
	{
		return new Color(tileColor.getRed(), tileColor.getGreen(), tileColor.getBlue(),  tileColor.getAlpha() / 5);
	}

    String getTileRegionString(Tile tile)
    {
        return String.valueOf(tile.getWorldLocation().getRegionID());
    }


    boolean addLabel(Graphics2D graphics, WorldView wv, LocalPoint tileLoc, int zOffset, String labelText, Color color)
    {
        if (tileLoc == null || !isLocalPointInScene(wv, tileLoc))
            return false;

        Point canvasTextLocation = Perspective.getCanvasTextLocation(client, graphics, tileLoc, labelText, zOffset);

        if (canvasTextLocation != null)
        {
            OverlayUtil.renderTextLocation(graphics, canvasTextLocation, labelText, color);
            return true;
        }
        return false;
    }

    boolean isLocalPointInScene(final WorldView wv, final LocalPoint point)
    {
		return wv.contains(point);
    }

	float average (int[] ints)
	{
		return (float) Arrays.stream(ints).sum() / ints.length;
	}

	// Return 1 if positive, 0 if zero -1 if negative
	int sign(float v)
	{
		return v > 0 ? 1 : (v < 0 ? -1 : 0);
	}

	Point2D.Float normalize(Point a, Point b)
	{
		float dist = a.distanceTo(b);
		Point2D.Float ab = new Point2D.Float(b.getX() - a.getX(), b.getY() - a.getY());
		ab.setLocation(ab.x / dist, ab.y / dist);
		return ab;
	}
}
