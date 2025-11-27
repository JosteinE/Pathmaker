package com.Pathmaker;

import javax.annotation.Nullable;
import java.awt.Color;
import net.runelite.api.coords.WorldPoint;

// Ref: GroundMarkerPoint - https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/groundmarkers/GroundMarkerPoint.java#L38
public class PathPoint
{
    private int regionId;
    private int regionX;
    private int regionY;
    private int z;

    @Nullable
    private Color color;
    @Nullable
    private String label;

    PathPoint(int regID, int regX, int regY, int plane)//, @Nullable Color color)
    {
        this.regionId = regID;
        this.regionX = regX;
        this.regionY = regY;
        this.z = plane;
        //this.color = color;
        //this.label = label; should just be the point index in the path
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

    @Nullable
    Color getColor()
    {
        return color;
    }
}
