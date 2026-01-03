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

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nonnull;
import java.awt.*;

@Slf4j
public class PathPointObject extends PathPoint
{
    // Should be assigned / unassigned as the player enters the area
    //private TileObject tileObject;

    private int id = -1; // active instance
	private final int baseId; // for lookup
    private final boolean isNpc;
    private int toCenterVectorX = 64;
    private int toCenterVectorY = 64;

	PathPointObject(String path, @Nonnull TileObject tileObject, int baseId)
	{
		super(path, tileObject.getWorldLocation().getRegionID(), tileObject.getWorldLocation().getRegionX(),
			tileObject.getWorldLocation().getRegionY(), tileObject.getWorldView().getPlane());

		isNpc = false;
		id = tileObject.getId();
		this.baseId = baseId;
	}

    PathPointObject(String path, @Nonnull NPC npc, int id, int baseId)
    {
        super(path, npc.getWorldLocation().getRegionID(),
                npc.getWorldLocation().getRegionX(),
                npc.getWorldLocation().getRegionY(),
                npc.getWorldView().getPlane());

        this.isNpc = true;
        this.id = id;
		this.baseId = baseId;
    }

    PathPointObject(String p, int r, int x, int y, int z, int id, int baseId, boolean isNpc)
    {
        super(p, r, x, y, z);

        this.isNpc = isNpc;
        this.id = id;
		this.baseId = baseId;
    }

    // footprint * TileSize / 2
    void setToCenterVector(int x, int y)
    {
        this.toCenterVectorX = x;
        this.toCenterVectorY = y;
    }

    int getToCenterVectorX()
    {
        return this.toCenterVectorX;
    }
    int getToCenterVectorY()
    {
        return this.toCenterVectorY;
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

	int getBaseId()
	{
		return this.baseId;
	}

    int getEntityId()
    {
        return this.id;
    }

	void setEntityId(int entityId)
	{
		this.id = entityId;
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
