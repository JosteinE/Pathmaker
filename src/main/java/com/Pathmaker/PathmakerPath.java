package com.Pathmaker;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

// Collection of path points
public class PathmakerPath
{
    private final String pathName;

    // Map with RegionIDs for keys with an ArrayList<PathPoint> for the specified region.
    private Map<Integer, ArrayList<PathPoint>> pathPoints;

    PathmakerPath(String name)
    {
        pathName = name;
    }

    void addPathPoint(PathPoint pathPoint)
    {
        int regionID = pathPoint.getRegionId();
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

    String getPathName()
    {
        return pathName;
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

