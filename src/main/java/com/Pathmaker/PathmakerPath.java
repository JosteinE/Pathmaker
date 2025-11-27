package com.Pathmaker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

// Collection of path points
public class PathmakerPath
{
    // Map with RegionIDs for keys with an ArrayList<PathPoint> for the specified region.
    private final HashMap<Integer, ArrayList<PathPoint>> pathPoints = new HashMap<>();

    PathmakerPath(PathPoint initialPathPoint)
    {
        addPathPoint(initialPathPoint);
    }

    // Add point to existing path
    void addPathPoint(PathPoint pathPoint)
    {
        int regionID = pathPoint.getRegionId();

        // Add the tile's regionID as key for the belonging tile(s) if it doesn't already exist.
        if (!pathPoints.containsKey(regionID))
        {
            pathPoints.put(regionID, new ArrayList<PathPoint>());
        }
        pathPoints.get(regionID).add(pathPoint);
    }

    void removePathPoint(PathPoint pathPoint)
    {
        int regionID = pathPoint.getRegionId();

        // Remove pathPoint from the ArrayList<PathPoint>
        pathPoints.get(regionID).remove(pathPoint);

        // Remove RegionID key if it is empty.
        if (pathPoints.get(regionID).isEmpty())
        {
            pathPoints.remove(regionID);
        }
    }

    // Return the relevant region IDs for this path
    Set<Integer> getRegionIDs()
    {
        return  pathPoints.keySet();
    }

    // Return tiles based on their regionID
    ArrayList<PathPoint> getPointsInRegion(int regionID)
    {
        return  pathPoints.get(regionID);
    }
}

