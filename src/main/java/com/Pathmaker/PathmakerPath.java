package com.Pathmaker;

import com.google.common.collect.ListMultimap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

// Collection of path points
public class PathmakerPath
{
    // Map with RegionIDs for keys with an ArrayList<PathPoint> for the specified region
    // Because a path might be spread across multiple regions
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
        pathPoint.setIndex(getSize());
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

    void setNewIndex(PathPoint point, int newIndex)
    {
        point.setIndex(newIndex);

        // Implement reordering!!

//        outerLoop:
//        for(int regionId : pathPoints.keySet())
//        {
//            for (PathPoint point : pathPoints.get(regionId))
//            {
//                if (point.getIndex() == currentIndex)
//                {
//                    point.setIndex(newIndex);
//                    break outerLoop;
//                }
//            }
//        }
    }

    int getSize()
    {
        int numPoints = 0;
        for (int regionId : pathPoints.keySet())
        {
            numPoints += pathPoints.get(regionId).size();
        }
        return numPoints;
    }
}

