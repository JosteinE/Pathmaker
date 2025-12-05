package com.Pathmaker;

import net.runelite.api.NPC;
import javax.annotation.Nullable;

public class PathPointNPC extends PathPoint
{
    private final NPC npc;

    PathPointNPC(NPC npc)
    {
        super(npc.getWorldLocation().getRegionID(), npc.getWorldLocation().getRegionX(),
                npc.getWorldLocation().getRegionY(), npc.getWorldLocation().getPlane());
        this.npc = npc;
    }

    @Nullable
    Object getNPC()
    {
        return npc;
    }
}
