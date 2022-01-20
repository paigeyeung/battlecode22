package m10;

import battlecode.common.*;

strictfp class ResourceLocationsManager {
    /**
     * NOTE: A local copy of data is NOT kept! Getting data reads from the shared array each time
     */

    static final int MIN_SQUARED_DISTANCE_BETWEEN_LOCATIONS = 15;
    static final int MIN_LEAD = 10;
    static final int MAX_SQUARED_DISTANCE_MARK_DEPLETED = 5;

    static class ResourceLocation {
        boolean isUsed;
        MapLocation location;
        boolean isGold;
        int score;

        ResourceLocation (boolean _isUsed, MapLocation _location, boolean _isGold, int _score) {
            if (_isUsed && _score == 0) {
                DebugManager.log("SOMETHING WENT WRONG: Is used but score is 0!");
            }
            isUsed = _isUsed;
            location = _location;
            isGold = _isGold;
            score = _score;
        }
        ResourceLocation (boolean _isUsed) {
            isUsed = _isUsed;
            if (isUsed) {
                DebugManager.log("SOMETHING WENT WRONG: Why are you using this constructor?");
            }
            location = new MapLocation(0, 0);
            isGold = false;
            score = 0;
        }
    }

    /** Updates shared array, call whenever possible */
    static void updateResourceLocations() throws GameActionException {
        boolean updateGold = false;
        if (Clock.getBytecodesLeft() > 5000 && GeneralManager.turnsAlive % 5 == 2) {
            updateGold = true;
        }

        MapLocation[] nearbyGold;
        if (updateGold) {
            nearbyGold = RobotPlayer.rc.senseNearbyLocationsWithGold();
        }
        else {
            nearbyGold = new MapLocation[0];
        }
        MapLocation[] nearbyLead = RobotPlayer.rc.senseNearbyLocationsWithLead(GeneralManager.myType.visionRadiusSquared, MIN_LEAD);
        ResourceLocation[] resourceLocations = readResourceLocations();

//        boolean allResourceLocationsUsed = areAllResourceLocationsUsed(resourceLocations);

        // If I'm near somewhere mentioned in the shared array
        int nearestResourceLocationIndex = getNearestResourceLocationIndex(resourceLocations);
        if (nearestResourceLocationIndex != -1) {
            ResourceLocation nearestResourceLocation = resourceLocations[nearestResourceLocationIndex];
            if (!nearestResourceLocation.isGold || updateGold) {
                if (nearestResourceLocation.location.distanceSquaredTo(GeneralManager.myLocation) <= MAX_SQUARED_DISTANCE_MARK_DEPLETED) {
                    if ((nearestResourceLocation.isGold && nearbyGold.length == 0)
                        || (!nearestResourceLocation.isGold && nearbyLead.length == 0)) {
                        // If there aren't any resources anymore, remove it
//                        DebugManager.log("Removing res loc at " + nearestResourceLocation.location);
                        ResourceLocation removeResourceLocation = new ResourceLocation(false);
                        writeResourceLocation(nearestResourceLocationIndex, removeResourceLocation);
                        resourceLocations[nearestResourceLocationIndex] = removeResourceLocation;
                    }
                    else {
                        // If the score changed a lot, update it
                        int newScore;
                        if (nearestResourceLocation.isGold) {
                            newScore = computeGoldResourceLocationScore(nearbyGold.length, false);
                        }
                        else {
                            newScore = computeLeadResourceLocationScore(nearbyLead.length, false);
                        }
                        if (Math.abs(nearestResourceLocation.score - newScore) >= 2) {
//                            DebugManager.log("Updating res loc score at " + nearestResourceLocation.location + " from " + nearestResourceLocation.score + " to " + newScore);
                            nearestResourceLocation.score = newScore;
                            writeResourceLocation(nearestResourceLocationIndex, nearestResourceLocation);
                        }
                    }
                }
            }
        }
        if (Clock.getBytecodesLeft() < 1000) return;

        // If I see lots of resources and it isn't in the shared array, add it
        if (updateGold && nearbyGold.length > 0) {
            MapLocation nearestGold = GeneralManager.getNearestLocation(nearbyGold);
            if (nearestGold == null) {
                DebugManager.log("SOMETHING WENT WRONG: There is gold at " + nearbyGold + " but there is no nearest!");
            }
            if (!existsResourceLocationNearby(resourceLocations, nearestGold)) {
                int newResourceLocationScore = computeGoldResourceLocationScore(nearbyGold.length, true);
                int newResourceLocationIndex = getLowestScoreResourceLocationIndex(resourceLocations, newResourceLocationScore);
                if (newResourceLocationIndex != -1) {
                    ResourceLocation newResourceLocation = new ResourceLocation(true, nearestGold, true, newResourceLocationScore);
//                    DebugManager.log("New gold res loc at " + newResourceLocation.location + " score " + newResourceLocationScore);
                    writeResourceLocation(newResourceLocationIndex, newResourceLocation);
                    resourceLocations[newResourceLocationIndex] = newResourceLocation;
                }
            }
            if (Clock.getBytecodesLeft() < 1000) return;
        }
        if (nearbyLead.length > 0) {
            MapLocation nearestLead = GeneralManager.getNearestLocation(nearbyLead);
            if (nearestLead == null) {
                DebugManager.log("SOMETHING WENT WRONG: There is lead at " + nearbyLead + " but there is no nearest!");
            }
            if (!existsResourceLocationNearby(resourceLocations, nearestLead)) {
                int newResourceLocationScore = computeLeadResourceLocationScore(nearbyLead.length, true);
                int newResourceLocationIndex = getLowestScoreResourceLocationIndex(resourceLocations, newResourceLocationScore);
                if (newResourceLocationIndex != -1) {
                    ResourceLocation newResourceLocation = new ResourceLocation(true, nearestLead, false, newResourceLocationScore);
//                    DebugManager.log("New lead res loc at " + newResourceLocation.location + " score " + newResourceLocationScore);
                    writeResourceLocation(newResourceLocationIndex, newResourceLocation);
                    resourceLocations[newResourceLocationIndex] = newResourceLocation;
                }
            }
        }
    }

    /** Read functions */
    static ResourceLocation decodeResourceLocation(int encoded) {
        int score = encoded & 0x7;
        boolean isUsed = score != 0;
        if (!isUsed) {
            return new ResourceLocation(false);
        }
        MapLocation location = new MapLocation((encoded >>> 10) & 0x3F, (encoded >> 4) & 0x3F);
        boolean isGold = ((encoded >>> 3) & 0x1) == 1;
        return new ResourceLocation(true, location, isGold, score);
    }
    static ResourceLocation[] readResourceLocations() throws GameActionException {
        ResourceLocation[] resourceLocations = new ResourceLocation[CommunicationManager.RESOURCE_LOCATIONS_NUM_ELEMENTS];
        for (int i = 0; i < resourceLocations.length; i++) {
            resourceLocations[i] = decodeResourceLocation(RobotPlayer.rc.readSharedArray(CommunicationManager.RESOURCE_LOCATIONS_INDEX + i));
        }
        return resourceLocations;
    }

    /** Write functions */
    static int encodeResourceLocation(ResourceLocation resourceLocation) {
        return (resourceLocation.location.x << 10) | (resourceLocation.location.y << 4) | ((resourceLocation.isGold ? 1 : 0) << 3) | resourceLocation.score;
    }
    static void writeResourceLocation(int index, ResourceLocation newResourceLocation) throws GameActionException {
//        DebugManager.log("Wrote res loc index: " + index + ", loc: " + newResourceLocation.location + ", score: " + newResourceLocation.score);
        int encoded = encodeResourceLocation(newResourceLocation);
        RobotPlayer.rc.writeSharedArray(CommunicationManager.RESOURCE_LOCATIONS_INDEX + index, encoded);
    }

    /** Helper functions */
    static boolean areAllResourceLocationsUsed(ResourceLocation[] resourceLocations) {
        for (int i = 0; i < resourceLocations.length; i++) {
            if (!resourceLocations[i].isUsed) {
                return false;
            }
        }
        return true;
    }
    static int getLowestScoreResourceLocationIndex(ResourceLocation[] resourceLocations, int minScore) {
        int chosenIndex = -1;
        int chosenIndexScore = 100;
        for (int i = 0; i < resourceLocations.length; i++) {
            if (!resourceLocations[i].isUsed) {
                chosenIndex = i;
                break;
            }
            if (resourceLocations[i].score >= minScore
                && resourceLocations[i].score < chosenIndexScore) {
                chosenIndex = i;
                chosenIndexScore = resourceLocations[i].score;
            }
        }
        return chosenIndex;
    }
    static boolean existsResourceLocationNearby(ResourceLocation[] resourceLocations, MapLocation location) {
        for (int i = 0; i < resourceLocations.length; i++) {
            if (!resourceLocations[i].isUsed) {
                continue;
            }
            if (resourceLocations[i].location.distanceSquaredTo(location) <= MIN_SQUARED_DISTANCE_BETWEEN_LOCATIONS) {
                return true;
            }
        }
        return false;
    }
    static int getNearestResourceLocationIndex(ResourceLocation[] resourceLocations) {
        int nearestIndex = -1;
        int nearestDistanceSquared = 10000;
        for (int i = 0; i < resourceLocations.length; i++) {
            if (!resourceLocations[i].isUsed) {
                continue;
            }
            int distanceSquared = GeneralManager.myLocation.distanceSquaredTo(resourceLocations[i].location);
            if (nearestIndex == -1 || distanceSquared < nearestDistanceSquared) {
                nearestIndex = i;
                nearestDistanceSquared = distanceSquared;
            }
        }
        return nearestIndex;
    }

    /** Compute score, MUST BE BETWEEN 1-7 INCLUSIVE */
    static int computeGoldResourceLocationScore(int numNearbyGold, boolean newlySeen) {
        int score = 7;
        return clampResourceLocationScore(score);
    }
    static int computeLeadResourceLocationScore(int numNearbyLead, boolean newlySeen) {
        int score = numNearbyLead;
        if (newlySeen) {
            score += 3;
        }
        return clampResourceLocationScore(score);
    }
    static int clampResourceLocationScore(int score) {
        return Math.max(Math.min(score, 7), 1);
    }

    /** Called by miner if no resources in sight, can return null */
    static MapLocation minerGetWhereToGo() throws GameActionException {
        ResourceLocation[] resourceLocations = readResourceLocations();
        int chosenIndex = -1;
        double chosenCombinedScore = -1;
        for (int i = 0; i < resourceLocations.length; i++) {
            if (!resourceLocations[i].isUsed) {
                continue;
            }
            double combinedScore = 100 * Math.log(resourceLocations[i].score + 1) / Math.log(GeneralManager.myLocation.distanceSquaredTo(resourceLocations[i].location) + 1);
            if (chosenIndex == -1 || combinedScore > chosenCombinedScore) {
                chosenIndex = i;
                chosenCombinedScore = combinedScore;
            }
        }
        // Only return the location if score is high enough
        if (chosenCombinedScore >= 30) {
//            DebugManager.log("Miner at " + GeneralManager.myLocation + " go to resource location " + resourceLocations[chosenIndex].location);
            RobotPlayer.rc.setIndicatorLine(GeneralManager.myLocation, resourceLocations[chosenIndex].location, 0, 255, 0);
            return resourceLocations[chosenIndex].location;
        }
        if (chosenIndex != -1) {
            RobotPlayer.rc.setIndicatorLine(GeneralManager.myLocation, resourceLocations[chosenIndex].location, 255, 0, 0);
        }
        return null;
    }
}
