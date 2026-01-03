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

import javax.annotation.Nullable;
import net.runelite.api.coords.WorldPoint;

// Ref: GroundMarkerPoint - https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/groundmarkers/GroundMarkerPoint.java#L38
public class PathPoint
{
    private int pathDrawIndex = -1;
    private int regionId;
    private int regionX;
    private int regionY;
    private int z;
	boolean drawToPrevious = true;

	private String pathOwner;
    private String label;

    PathPoint(String path, int regID, int regX, int regY, int plane)
    {
		this.pathOwner = path;
        this.regionId = regID;
        this.regionX = regX;
        this.regionY = regY;
        this.z = plane;
    }

    PathPoint(String path, WorldPoint worldPoint)
    {
		this.pathOwner = path;
        this.regionId = worldPoint.getRegionID();
        this.regionX = worldPoint.getRegionX();
        this.regionY = worldPoint.getRegionY();
        this.z = worldPoint.getPlane();
    }

	String getPathOwnerName()
	{
		return this.pathOwner;
	}

	void setPathOwnerName(String pathOwnerName)
	{
		this.pathOwner = pathOwnerName;
	}

    int getRegionId()
    {
        return regionId;
    }

    int getX()
    {
        return regionX;
    }
    int getY()
    {
        return regionY;
    }
    int getZ()
    {
        return z;
    }

    WorldPoint getWorldPoint()
    {
        return WorldPoint.fromRegion(regionId, regionX, regionY, z);
    }

    void setLabel(String newLabel)
    {
        this.label = newLabel;
    }

    @Nullable
    String getLabel()
    {
        return label;
    }

    void setDrawIndex(int index)
    {
        this.pathDrawIndex = index;
    }

    int getDrawIndex()
    {
        return pathDrawIndex;
    }

    void updateRegionLocation(int region, int x, int y, int z)
    {
        // DONT FORGET TO ALSO UPDATE BELONGING PathmakerPath! USE plugin.updatePointLocation
        this.regionId = region;
        this.regionX = x;
        this.regionY = y;
        this.z = z;
    }
}
