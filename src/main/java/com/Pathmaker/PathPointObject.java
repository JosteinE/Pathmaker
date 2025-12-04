package com.Pathmaker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nonnull;
import java.awt.*;

@Slf4j
public class PathPointObject extends PathPoint
{
    // Should be assigned / unassigned as the player enters the area
    //private TileObject tileObject;

    private final int id;
    private final boolean isNpc;

    PathPointObject(@Nonnull TileObject tileObject)
    {
        super(tileObject.getWorldLocation().getRegionID(), tileObject.getWorldLocation().getRegionX(),
                tileObject.getWorldLocation().getRegionY(), tileObject.getWorldView().getPlane());

        isNpc = false;
        id = tileObject.getId();
    }

    PathPointObject(@Nonnull NPC npc, int id)//, Client client)
    {
        super(npc.getWorldLocation().getRegionID(),
                npc.getWorldLocation().getRegionX(),
                npc.getWorldLocation().getRegionY(),
                npc.getWorldView().getPlane());

        this.isNpc = true;
        this.id = id;
    }

    PathPointObject(int r, int x, int y, int z, int id, boolean isNpc)//, Client client)
    {
        super(r, x, y, z);

        this.isNpc = isNpc;
        this.id = id;
    }


//    private Renderable getRenderableObject(TileObject tileObject)
//    {
//        Renderable renderObj = null;
//
//        if (tileObject instanceof GameObject) {renderObj = ((GameObject) tileObject).getRenderable();} // Boxes, trees
//        else if (tileObject instanceof GroundObject) {renderObj = ((GroundObject) tileObject).getRenderable();} // Grass
//        else if (tileObject instanceof ItemLayer) {renderObj = ((ItemLayer) tileObject).getBottom();}  // Items held by tile
//        else if (tileObject instanceof DecorativeObject) {renderObj = ((DecorativeObject) tileObject).getRenderable();}
//        else if (tileObject instanceof WallObject) {renderObj = ((WallObject) tileObject).getRenderable1();}
//        return renderObj;
//    }

    int getEntityId()
    {
        return id;
    }

    boolean isNpc()
    {
        return isNpc;
    }

//    TileObject getObject()
//    {
//        return tileObject;
//    }
//
//
//    TileObject loadObject(TileObject object)
//    {
//        this.tileObject = object;
//        return this.tileObject;
//    }
//
//    void unloadObject()
//    {
//        this.tileObject = null;
//    }

//    void loadNpc (NPC npc)
//    {
//        this.npc = npc;
//    }
//
//    void unloadNpc()
//    {
//        npc = npc;
//    }
}
