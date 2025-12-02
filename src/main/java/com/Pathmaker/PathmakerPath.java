package com.Pathmaker;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

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
    boolean panelExpanded = true;

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

    void removePathPoint(PathPoint point)
    {
        int regionID = point.getRegionId();

        int removedIndex = point.getDrawIndex();

        //log.debug("Removing point: " + removedIndex);
        // Remove pathPoint from the ArrayList<PathPoint>
        pathPoints.get(regionID).remove(point);

        // Remove RegionID key if the ArrayList is empty.
        if (pathPoints.get(regionID).isEmpty())
        {
            pathPoints.remove(regionID);
        }

        ArrayList<PathPoint> drawOrder = getDrawOrder(null);
        for(int i = removedIndex; i < drawOrder.size(); i++)
        {
            drawOrder.get(i).setDrawIndex(i);
        }
    }

    // Fetch all path points and close any draw index gaps
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

    // Set new draw index for a specific point and move the other point's indices accordingly
    void setNewIndex(PathPoint point, final int newIndex)
    {
        int oldIndex = point.getDrawIndex();

        if (oldIndex == newIndex){return;}

        boolean newGreater = newIndex > oldIndex;

        int indexMoveDir = newGreater ? -1 : 1;
        int startIndex = newGreater ? oldIndex + 1 : newIndex;
        int targetIndex = newGreater ? newIndex + 1 : oldIndex;

        // !newGreater
        // 2 -> 0
        // start = newIndex
        // target = oldIndex
        // i < target
        // idx += 1

        // newGreater
        // 0 -> 2
        // start = oldIndex + 1
        // target = newIndex + 1
        // i < target
        // idx += -1

// An easier look at what's going on in the uncommented for loop below
//        if(newGreater)
//        {
//            // 3p
//            // 0 -> 2
//
//            idx 1 -> 0
//
//            for(int i = oldIndex + 1; i <= newIndex; i++)
//            {
//                getPointAtDrawIndex(i).setDrawIndex(i - 1);
//            }
//        }
//        else
//        {
//            // 3p
//            // 2 -> 0
//            for(int i = oldIndex - 1; i >= newIndex; i--)
//            {
//                getPointAtDrawIndex(i).setDrawIndex(i + 1);
//            }
//        }

        ArrayList<PathPoint> pointsToMove = new ArrayList<>();
        ArrayList<Integer> regionsToReconstruct = new ArrayList<>();

        regionsToReconstruct.add(point.getRegionId());

        for(int i = startIndex; i < targetIndex; i ++)
        {
            pointsToMove.add(getPointAtDrawIndex(i));
        }

        // Changing draw index above is total bait, as it confuses getPointAtDrawIndex
        // So doing it here.
        for(int i = 0; i < pointsToMove.size(); i ++)
        {
            pointsToMove.get(i).setDrawIndex(pointsToMove.get(i).getDrawIndex() + indexMoveDir); // int indexMoveDir = newGreater ? -1 : 1;

            if(!regionsToReconstruct.contains(pointsToMove.get(i).getRegionId()))
            {
                regionsToReconstruct.add(pointsToMove.get(i).getRegionId());
            }
        }

        point.setDrawIndex(newIndex);

        // 6 -> 0
        // i = 0
        // e = 5 (oldIndex - 1)

        // move affected indices up/down depending on if the new specified index is greater or less than the old one.
//        for (int i = startIndex; i <= targetIndex; i++)
//        {
//            PathPoint drawPoint = getPointAtDrawIndex(i);
//            //log.debug("Index {} was assigned index: {}", drawPoint.getDrawIndex(), i+otherIndexMoveDir);
//            drawPoint.setDrawIndex(i + indexMoveDir);
//
//            if(!regionsToReconstruct.contains(drawPoint.getRegionId()))
//            {
//                regionsToReconstruct.add(drawPoint.getRegionId());
//            }
//        }

        // Finally move the chosen point to the desired index
        //log.debug("Index {} was assigned index: {}", point.getDrawIndex(), newIndex);
        point.setDrawIndex(newIndex);

        // Once the points have been reassigned their draw order, reorder the affected ArrayList to match
        // as this will make it easier for our getDrawOrder later
        for (int regionId : regionsToReconstruct)
        {
            reconstructRegionOrder(regionId);
        }
    }

    // Sort the specified ArrayList in the order of draw indices
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
        return regionIDs.contains(point.getRegionId());
    }

    boolean containsPoint(PathPoint point)
    {
        for (ArrayList<PathPoint> regionPoints : pathPoints.values())
            if (regionPoints.contains(point))
                return true;

        return false;
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

    public ArrayList<PathPoint> getReversedDrawOrder()
    {
        ArrayList<PathPoint> reverseDrawOrder = new ArrayList<>(getDrawOrder(null));
        Collections.reverse(reverseDrawOrder);
        return reverseDrawOrder;
    }

    public ArrayList<PathPoint> reverseDrawOrder()
    {
        ArrayList<PathPoint> reverseDrawOrder = getDrawOrder(null);
        for(int i = 0; i < getSize(); i++)
        {
            reverseDrawOrder.get(i).setDrawIndex(getSize() - 1 - i);
        }

        for (int regionId : pathPoints.keySet())
        {
            Collections.reverse(pathPoints.get(regionId));
        }

        return reverseDrawOrder;
    }

    // Return the size of all stored points (across all relevant regions) for this path
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
    // NOT in loaded regions. (which you don't want to render, but is for sorting. See reconstructDrawOrder())
    ArrayList<PathPoint> getDrawOrder(@Nullable ArrayList<Integer> loadedRegions)
    {
        ArrayList<PathPoint> drawOrder = new ArrayList<>();

        // Calculate the number of points to collect (points that are inside the loaded regions)
        int numPointsToLoad = loadedRegions == null ? getSize() : 0;
        int indexToFind = 0;

        // Creating a map for tracking the last index checked in each of the RegionIDs
        // (which is used as keys for pathPoint) so the loops do not start at 0 every time
        // This works because stored points are sorted in their individual region ArrayLists
        // based on their draw order.
        final HashMap<Integer, Integer> loopIndexTracker = new HashMap<>();


        // Get the highest index value to be used as target, mostly in case of gaps
        int finalIndex = 0;

        // If loadedRegions is null then return the full list of points in draw order regardless of region
        if(loadedRegions == null)
        {
            for (int regionId : pathPoints.keySet())
            {
                loopIndexTracker.put(regionId, 0);

                int numInRegion = pathPoints.get(regionId).size();
                int lastRegionIndex = pathPoints.get(regionId).get(numInRegion - 1).getDrawIndex();
                finalIndex = Math.max(lastRegionIndex, finalIndex);
            }

        }
        else
        {
            indexToFind = getSize();

            for (Integer loadedRegion : loadedRegions)
            {
                // Skip if region isn't loaded
                if (!pathPoints.containsKey(loadedRegion))
                {continue;}

                // Add regionID to loop tracker
                loopIndexTracker.put(loadedRegion, 0);

                // Get final draw index
                int numInRegion = pathPoints.get(loadedRegion).size();
                int lastRegionIndex = pathPoints.get(loadedRegion).get(numInRegion - 1).getDrawIndex();
                finalIndex = Math.max(lastRegionIndex, finalIndex);

                numPointsToLoad += numInRegion;

                // Determine the starting index (may not be 0 if that tile is in an unloaded region)
                indexToFind = Math.min(pathPoints.get(loadedRegion).get(0).getDrawIndex(), indexToFind);

            }
        }

        // Iterate through the relevant list of points, collecting the points in the order of their draw index
        int lastSize = -1;
        while(drawOrder.size() < numPointsToLoad)
        {
            // If the next draw index cant be found, increase the index search gap
            if (lastSize == drawOrder.size())
            {
                indexToFind += 1;

                // Break it if failed to find point within the scope
                if (indexToFind > finalIndex)
                {
                    log.debug("Missing draw indices {}, out of: {}", numPointsToLoad- drawOrder.size(), numPointsToLoad);
                    break;
                }
            }


            lastSize = drawOrder.size();

            // Look for point with draw index equal to indexToFind. Store current location for a given ArrayList in
            // loopIndexTracker and break - if the next index is greater than indexToFind.
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
        return drawOrder;
    }
}

