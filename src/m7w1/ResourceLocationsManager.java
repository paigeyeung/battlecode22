package m7w1;

import battlecode.common.*;

strictfp class ResourceLocationsManager {
    /**
     * NOTE: A local copy of data is NOT kept! Getting data reads from the shared array each time
     */

    static final int MIN_SQUARED_DISTANCE_BETWEEN_LOCATIONS = 15;
    static final int MIN_LEAD = 10;

    static class ResourceLocation {
        boolean isUsed;
        MapLocation location;
        boolean isGold;
        int score;

        ResourceLocation (boolean _isUsed, MapLocation _location, boolean _isGold, int _score) {
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
        }
    }

    /** Updates shared array, call whenever possible */
    static void updateResourceLocations() throws GameActionException {
        MapLocation[] nearbyGold = RobotPlayer.rc.senseNearbyLocationsWithGold();
        MapLocation[] nearbyLead = RobotPlayer.rc.senseNearbyLocationsWithLead(GeneralManager.myType.visionRadiusSquared, MIN_LEAD);
        ResourceLocation[] resourceLocations = readResourceLocations();

        // If I see lots of resources and it isn't in the shared array, add it
        for (int i = 0; i < nearbyGold.length; i++) {
            if (!existsResourceLocationNearby(resourceLocations, nearbyGold[i], true)) {
                int newResourceLocationScore = computeResourceLocationScore(nearbyGold[i], true);
                int newResourceLocationIndex = getLowestScoreResourceLocationIndex(resourceLocations);
                if (newResourceLocationIndex != -1 && (!resourceLocations[newResourceLocationIndex].isUsed || resourceLocations[newResourceLocationIndex].score < newResourceLocationScore)) {
                    ResourceLocation newResourceLocation = new ResourceLocation(true, nearbyGold[i], true, newResourceLocationScore);
                    writeResourceLocation(newResourceLocationIndex, newResourceLocation);
                    resourceLocations[newResourceLocationIndex] = newResourceLocation;
                }
            }
        }
        if (Clock.getBytecodesLeft() < 500) return;
        for (int i = 0; i < nearbyLead.length; i++) {
            if (!existsResourceLocationNearby(resourceLocations, nearbyLead[i], false)) {
                int newResourceLocationScore = computeResourceLocationScore(nearbyLead[i], false);
                int newResourceLocationIndex = getLowestScoreResourceLocationIndex(resourceLocations);
                if (newResourceLocationIndex != -1 && (!resourceLocations[newResourceLocationIndex].isUsed || resourceLocations[newResourceLocationIndex].score < newResourceLocationScore)) {
                    ResourceLocation newResourceLocation = new ResourceLocation(true, nearbyLead[i], false, newResourceLocationScore);
                    writeResourceLocation(newResourceLocationIndex, newResourceLocation);
                    resourceLocations[newResourceLocationIndex] = newResourceLocation;
                }
            }
        }
        if (Clock.getBytecodesLeft() < 500) return;

        // If I'm at somewhere mentioned in the shared array and there aren't a lot of resources anymore, remove it
    }

    /** Read functions */
    static ResourceLocation decodeResourceLocation(int encoded) {
        boolean isUsed = (encoded & 0x1) == 1;
        if (!isUsed) {
            return new ResourceLocation(isUsed);
        }
        MapLocation location = new MapLocation((encoded >>> 10) & 0x3F, (encoded >> 4) & 0x3F);
        boolean isGold = ((encoded >>> 3) & 0x1) == 1;
        int score = encoded & 0x7;
        return new ResourceLocation(isUsed, location, isGold, score);
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
        DebugManager.log("Wrote resource location index: " + index + ", location: " + newResourceLocation.location + ", isGold: " + newResourceLocation.isGold + ", score: " + newResourceLocation.score);
        int encoded = encodeResourceLocation(newResourceLocation);
        RobotPlayer.rc.writeSharedArray(CommunicationManager.RESOURCE_LOCATIONS_INDEX + index, encoded);
    }

    /** Helper functions */
    static int getLowestScoreResourceLocationIndex(ResourceLocation[] resourceLocations) {
        int chosenIndex = -1;
        int chosenIndexScore = 1000;
        for (int i = 0; i < resourceLocations.length; i++) {
            if (!resourceLocations[i].isUsed) {
                chosenIndex = i;
                break;
            }
            if (resourceLocations[i].score < chosenIndexScore) {
                chosenIndex = i;
                chosenIndexScore = resourceLocations[i].score;
            }
        }
        return chosenIndex;
    }
    static boolean existsResourceLocationNearby(ResourceLocation[] resourceLocations, MapLocation location, boolean isGold) {
        for (int i = 0; i < resourceLocations.length; i++) {
            if (!resourceLocations[i].isUsed) {
                continue;
            }
            if (resourceLocations[i].location.distanceSquaredTo(location) <= MIN_SQUARED_DISTANCE_BETWEEN_LOCATIONS
                && (!isGold || resourceLocations[i].isGold)) {
                return true;
            }
        }
        return false;
    }

    /** Compute score, MUST BE BETWEEN 1-7 INCLUSIVE */
    static int computeResourceLocationScore(MapLocation location, boolean isGold) {
        if (isGold) {
            return 7;
        }
        return 3;
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
            double combinedScore = 100 * resourceLocations[i].score / (GeneralManager.myLocation.distanceSquaredTo(resourceLocations[i].location));
            if (chosenIndex == -1 || combinedScore > chosenCombinedScore) {
                chosenIndex = i;
                chosenCombinedScore = combinedScore;
            }
        }
        // Only return the location if score is high enough
        if (chosenCombinedScore >= 3) {
            DebugManager.log("Miner at " + GeneralManager.myLocation + " go to resource location " + resourceLocations[chosenIndex].location);
            return resourceLocations[chosenIndex].location;
        }
        return null;
    }
}
