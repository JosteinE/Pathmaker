package com.Pathmaker;

import javax.annotation.Nullable;
import java.awt.Color;

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
}
