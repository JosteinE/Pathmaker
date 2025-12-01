package com.Pathmaker;

import javax.annotation.Nullable;
import net.runelite.api.coords.WorldPoint;

// Ref: GroundMarkerPoint - https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/groundmarkers/GroundMarkerPoint.java#L38
public class PathPoint
{
    private int pathDrawIndex = 0;
    private int regionId;
    private int regionX;
    private int regionY;
    private int z;

    @Nullable
    private String label;

    PathPoint(int regID, int regX, int regY, int plane)
    {
        this.regionId = regID;
        this.regionX = regX;
        this.regionY = regY;
        this.z = plane;
    }

    PathPoint(WorldPoint worldPoint)
    {
        this.regionId = worldPoint.getRegionID();
        this.regionX = worldPoint.getRegionX();
        this.regionY = worldPoint.getRegionY();
        this.z = worldPoint.getPlane();
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

    void setLabel(@Nullable String newLabel)
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
}
