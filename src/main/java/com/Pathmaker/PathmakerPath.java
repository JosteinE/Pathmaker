package com.Pathmaker;

import com.google.common.collect.ListMultimap;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.*;
import java.util.stream.Collectors;

// Collection of path points
@Slf4j
public class PathmakerPath
{
    // Map with RegionIDs for keys with an ArrayList<PathPoint> for the specified region
    // Because a path might be spread across multiple regions
    private final HashMap<Integer, ArrayList<PathPoint>> pathPoints = new HashMap<>();
    Color color;
    boolean loopPath = false;
    boolean hidden = false;

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
        pathPoint.setDrawIndex(getSize()-1);
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

        reconstructDrawOrder();
    }

    void reconstructDrawOrder()
    {
        ArrayList<PathPoint> drawOrder = getDrawOrder(null);
        boolean startRearrangement = false;
        for  (int i = 0; i < drawOrder.size(); i++)
        {
            if (drawOrder.get(i).getDrawIndex() != i)
            {
                startRearrangement = true;
            }
            if (startRearrangement)
            {
                drawOrder.get(i).setDrawIndex(i);
            }
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

    boolean hasPointsInRegion(int regionID)
    {
        return  pathPoints.containsKey(regionID);
    }

    void setNewIndex(PathPoint point, final int newIndex)
    {
        int oldIndex = point.getDrawIndex();

        if (oldIndex == newIndex){return;}

        boolean newGreater = newIndex > oldIndex;
        int startIndex = newGreater ? (oldIndex + 1) : newIndex;
        int targetIndex = newGreater ? newIndex : (oldIndex - 1);
        int otherIndexMoveDir = newGreater ? -1 : 1;


        // New index = 1
        // old index = 2

        // start = 1
        // target = 1

        // moveDir += 1

//        if(newIndex > oldIndex)
//        {
//            // 3p
//            // 1 -> 3
//            for(int i = oldIndex + 1; i <= newIndex; i++)
//            {
//                drawOrder.get(newIndex).setIndex(i - 1);
//            }
//            point.setIndex(newIndex);
//        }
//        else if (newIndex < oldIndex)
//        {
//            // 3p
//            // 3 -> 0
//            for(int i = newIndex; i <= oldIndex - 1; i++)
//            {
//                drawOrder.get(newIndex).setIndex(i + 1);
//            }
//            point.setIndex(newIndex);
//        }
//        else
//        {
//            return;
//        }


        // 2 -> 1
        // start: 1
        // target: 1
        // move: +1

        ArrayList<Integer> regionsToReconstruct = new ArrayList<>();
        regionsToReconstruct.add(point.getRegionId());

        for (int i = startIndex; i <= targetIndex; i++)
        {
            PathPoint drawPoint = getPointAtDrawIndex(i);
            log.debug("Index {} was assigned index: {}", drawPoint.getDrawIndex(), i+otherIndexMoveDir);
            drawPoint.setDrawIndex(i + otherIndexMoveDir);

            if(!regionsToReconstruct.contains(drawPoint.getRegionId()))
            {
                regionsToReconstruct.add(drawPoint.getRegionId());
            }
        }
        log.debug("Index {} was assigned index: {}", point.getDrawIndex(), newIndex);
        point.setDrawIndex(newIndex);

        for (int regionId : regionsToReconstruct)
        {
            reconstructRegionOrder(regionId);
        }

        for(PathPoint drawPoint : getDrawOrder(null))
        {
            log.debug("DrawIndex: {}", drawPoint.getDrawIndex());
        }
    }

    void reconstructRegionOrder(int regionId)
    {
        if (pathPoints.get(regionId).size() < 2) {return;}

        while(true)
        {
            boolean regionOrdered = true;
            for (int i = 1; i < pathPoints.get(regionId).size(); i++)
            {
                if(pathPoints.get(regionId).get(i).getDrawIndex() < pathPoints.get(regionId).get(i-1).getDrawIndex())
                {
                    Collections.swap(pathPoints.get(regionId), i, i-1);
                    regionOrdered = false;
                }
            }

            if(regionOrdered){break;}
        }

        for (PathPoint point : pathPoints.get(regionId))
        {
            log.debug("Reconstructed array draw order: {}", point.getDrawIndex());
        }
    }

    boolean isPointInRegions(PathPoint point, int[] regionIDs)
    {
        for (int regionID : regionIDs)
        {
            if (point.getRegionId() == regionID) {
                return true;
            }
        }
        return false;
    }

    boolean isPointInRegions(PathPoint point, ArrayList<Integer>regionIDs)
    {
        //return pathPoints.containsKey(regionId) && pathPoints.get(regionId).contains(point);
        return regionIDs.contains(point.getRegionId());
    }

    PathPoint getPointAtDrawIndex(int index)
    {
        for(int regionId : pathPoints.keySet())
        {
            for (PathPoint point : pathPoints.get(regionId))
            {
                if (point.getDrawIndex() == index)
                {
                    return point;
                }
            }
        }

        log.debug("Could not find point at index: {}", index);
        return null;
    }

    int getSize()
    {
        int numPoints = 0;
        if(!pathPoints.isEmpty())
        {
            for (int regionId : pathPoints.keySet()) {
                numPoints += pathPoints.get(regionId).size();
            }
        }
        return numPoints;
    }

    // Return an ArrayList with the PathPoints in the order they should be drawn
    // NB! If param loadedRegions is null, then getDrawOrder will return the tiles also
    // NOT in loaded regions. (which you don't want to render)
    ArrayList<PathPoint> getDrawOrder(@Nullable ArrayList<Integer> loadedRegions)
    {
        ArrayList<PathPoint> drawOrder = new ArrayList<>();

        // Calculate the number of points to collect (points that are inside the loaded regions)
        int numPointsInRegion = loadedRegions == null ? getSize() : 0;
        int indexToFind = 0;

        // Creating a map for tracking the last index checked in each of the RegionIDs
        // (which is used as keys for pathPoint) so the loops do not start at 0 every time
        final HashMap<Integer, Integer> loopIndexTracker = new HashMap<>();
        if(loadedRegions == null)
        {
            for (int regionId : pathPoints.keySet())
            {
                loopIndexTracker.put(regionId, 0);
            }

        }
        else
        {
            for (int i = 0; i < loadedRegions.size(); i++)
            {
                if (pathPoints.containsKey(loadedRegions.get(i))) {
                    loopIndexTracker.put(loadedRegions.get(i), 0);
                }
            }

            indexToFind = getSize();

            for (int relevantRegionId : loopIndexTracker.keySet())
            {
                numPointsInRegion += pathPoints.get(relevantRegionId).size();

                // Determine the starting index (may not be 0 if that tile is in an unloaded region)
                for (PathPoint point : pathPoints.get(relevantRegionId))
                {
                    if(point.getDrawIndex() < indexToFind)
                    {
                        indexToFind = point.getDrawIndex();
                    }
                }
            }
        }

        // Iterate through the list of points (including points in regions not currently loaded)
        // but only adding points to drawOrder if the points are in loaded regions.
        int lastSize = -1;
        while(drawOrder.size() < numPointsInRegion)
        {
            // If the next draw index cant be found, increase the index search gap
            if (lastSize == drawOrder.size())
            {
                indexToFind += 1;
                if (indexToFind >= numPointsInRegion)
                {
                    log.debug("Missing draw indices {}, out of: {}", numPointsInRegion- drawOrder.size(), numPointsInRegion);
                    break;
                }
            }

            lastSize = drawOrder.size();
            for (int relevantRegionId : loopIndexTracker.keySet())
            {
                for (int i = loopIndexTracker.get(relevantRegionId); i < pathPoints.get(relevantRegionId).size(); i++)
                {
                    PathPoint point = pathPoints.get(relevantRegionId).get(i);
                    int pointIndex = point.getDrawIndex();

                    if (pointIndex == indexToFind)
                    {
                        drawOrder.add(point);
                        indexToFind += 1;
                    }
                    else if (pointIndex > drawOrder.size())
                    {
                        loopIndexTracker.put(relevantRegionId, i);
                        break;
                    }
                    loopIndexTracker.put(relevantRegionId, i+1);
                }
            }
        }
        //log.debug("Draw Order: {}", drawOrder.size());
        return drawOrder;
    }
}

