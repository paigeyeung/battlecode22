package m10;

import battlecode.common.*;

import java.util.Random;

strictfp class GeneralManager {
    static int[][] visitedTurns;

    static final Direction[] DIRECTIONS = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };
    static final RobotType[] BUILDINGS = {
        RobotType.ARCHON,
        RobotType.LABORATORY,
        RobotType.WATCHTOWER
    };
    static final RobotType[] DROIDS = {
        RobotType.MINER,
        RobotType.BUILDER,
        RobotType.SOLDIER,
        RobotType.SAGE
    };
    static final RobotType[] NONCOMBAT = {
        RobotType.ARCHON,
        RobotType.LABORATORY,
        RobotType.MINER,
        RobotType.BUILDER
    };
    static final RobotType[] COMBAT = {
        RobotType.WATCHTOWER,
        RobotType.SOLDIER,
        RobotType.SAGE
    };

    static final Random rng = new Random(2);
    static Direction getRandomDirection() {
        return DIRECTIONS[rng.nextInt(DIRECTIONS.length)];
    }

    /** Statics that are set once at start of game */
    static RobotType myType;
    static MapLocation startingLocation;
    static int mapWidth, mapHeight;

    /** Statics that are updated once per turn at start of turn */
    static int turnsAlive = 0;
    static MapLocation myLocation;

    /** Get center the map */
    static MapLocation getMapCenter() {
        return new MapLocation(mapWidth / 2, mapHeight / 2);
    }

    /** Get horizontally and vertically mirrored location on the map */
    static MapLocation getOppositeLocation(MapLocation location, boolean flipHorizontal, boolean flipVertical) {
        return new MapLocation(flipHorizontal ? (mapWidth - location.x - 1) : location.x, flipVertical ? (mapHeight - location.y - 1) : location.y);
    }

    /**
     * Get squared distance between two locations
     */
    static int getSqDistance(MapLocation loc1, MapLocation loc2) {
        return (loc1.x - loc2.x)*(loc1.x - loc2.x) + (loc1.y - loc2.y)*(loc1.y - loc2.y);
    }

    /**
     * Get midpoint between two map locations
     */

    static MapLocation getMidpoint(MapLocation loc1, MapLocation loc2) {
        return new MapLocation((int)((loc1.x+loc2.x)/2),(loc1.y+loc2.y)/2);
    }

    /**
     * Get opposite direction to a direction
     */
    static Direction getOppositeDirection(Direction dir) {
        switch(dir) {
            case NORTH: return Direction.SOUTH;
            case SOUTH: return Direction.NORTH;
            case NORTHWEST: return Direction.SOUTHEAST;
            case NORTHEAST: return Direction.SOUTHWEST;
            case EAST: return Direction.WEST;
            case WEST: return Direction.EAST;
            case SOUTHEAST: return Direction.NORTHWEST;
            case SOUTHWEST: return Direction.NORTHEAST;
            default: return null;
        }
    }

    /**
     * Get direction to get to destination with less rubble
     */

    static Direction getNextDir(MapLocation dest) throws GameActionException {
        MapLocation myLoc = RobotPlayer.rc.getLocation();

        if(myLoc.equals(dest) || dest == null) return null;

        Direction movementDir = null;
        int minDist = getSqDistance(myLoc, dest);
        int f = Integer.MAX_VALUE;

        for(Direction dir : DIRECTIONS) {
            if(RobotPlayer.rc.canMove(dir)) {
                MapLocation adj = RobotPlayer.rc.adjacentLocation(dir);
                int newDist = getSqDistance(adj,dest);
                int newRubble = RobotPlayer.rc.senseRubble(adj);
                int newF = newDist + newRubble;

                if(newF < f) {
                    f = newF;
                    movementDir = dir;
                }
                else if(newF == f){
                    if(((int)Math.random()*2)==0) {
                        f = newF;
                        movementDir = dir;
                    }
                }
            }
        }
        return movementDir;
    }

    /**
     * Get direction to encircle a location from a given radius
     */
    static Direction getDirToEncircle(MapLocation loc, int rSq) throws GameActionException {
        double r = Math.sqrt(rSq);

        int xOffset = 1000, yOffset = 1000;

        while (!onMap(loc.x + xOffset, loc.y + yOffset)){
            xOffset = (int) (Math.random() * ((int) r + 1)) * ((int) (Math.random() * 3) - 1);
            yOffset = (int) Math.ceil(Math.sqrt(rSq - xOffset * xOffset)) * ((int) (Math.random() * 3) - 1);
        }

        return getNextDir(new MapLocation(loc.x + xOffset, loc.y + yOffset));
    }

    /**
     * Returns whether a location is on the map
     */
    static boolean onMap(MapLocation loc) {
        return loc.x >= 0 && loc.x <= RobotPlayer.rc.getMapWidth() && loc.y >= 0 && loc.y <= RobotPlayer.rc.getMapHeight();
    }

    static boolean onMap(int x, int y) {
        return x >= 0 && x <= RobotPlayer.rc.getMapWidth() && y >= 0 && y <= RobotPlayer.rc.getMapHeight();
    }

    /**
     * Get nearest corner to a location
     */
    static MapLocation getNearestCorner(MapLocation loc) {
        int x = 0, y = 0;
        if(Math.abs(loc.x - (RobotPlayer.rc.getMapWidth() - 1)) < loc.x) {
            x = RobotPlayer.rc.getMapWidth() - 1;
        }
        if(Math.abs(loc.y - (RobotPlayer.rc.getMapHeight() - 1)) < loc.y) {
            y = RobotPlayer.rc.getMapHeight() - 1;
        }
        return new MapLocation(x,y);
    }

    /** Get best build direction relative to preferred direction, returns null if no direction is found */
    static Direction getBuildDirection(RobotType robotType, Direction preferredDirection) throws GameActionException {
        if (preferredDirection == null) {
            preferredDirection = getRandomDirection();
        }
        int preferredDirectionIndex = -1;
        for (int i = 0; i < DIRECTIONS.length; i++) {
            if (DIRECTIONS[i] == preferredDirection) {
                preferredDirectionIndex = i;
            }
        }
        if (preferredDirectionIndex == -1) {
            DebugManager.log("SOMETHING WENT WRONG: getBuildDirection didn't find index of preferred direction " + preferredDirection);
            return null;
        }

        Direction lowestScoreDirection = null;
        int lowestScore = 10000;
        for (int i = 0; i < DIRECTIONS.length; i++) {
            if (!RobotPlayer.rc.canBuildRobot(robotType, DIRECTIONS[i])) {
                continue;
            }
            int angleAwayFromPreferredDirection = Math.abs(preferredDirectionIndex - i);
            int rubble = RobotPlayer.rc.senseRubble(myLocation.add(DIRECTIONS[i]));
            int score = angleAwayFromPreferredDirection + rubble / 10;
            if (lowestScoreDirection == null || score < lowestScore) {
                lowestScoreDirection = DIRECTIONS[i];
                lowestScore = score;
            }
        }
        return lowestScoreDirection;
    }

    /** Try to move, returns boolean if successful */
    static boolean tryMove(Direction direction, boolean moveRandomlyIfFailed) throws GameActionException {
        if (direction != null && RobotPlayer.rc.canMove(direction)) {
            RobotPlayer.rc.move(direction);
            GeneralManager.myLocation = RobotPlayer.rc.getLocation();
            return true;
        }

        // If the attempted move didn't work, move randomly
        if (moveRandomlyIfFailed) {
            int randAttempts = 0;
            while ((direction == null || !RobotPlayer.rc.canMove(direction)) && randAttempts <= 10) {
                direction = getRandomDirection();
                randAttempts++;
            }
            if (RobotPlayer.rc.canMove(direction)) {
                RobotPlayer.rc.move(direction);
                GeneralManager.myLocation = RobotPlayer.rc.getLocation();
                return true;
            }
        }
        return false;
    }

    /** Get nearest location with lead, expensive bytecode, will not return myLocation even if I'm sitting on lead */
    static MapLocation getNearestLeadLocation() throws GameActionException {
        MapLocation[] locations = RobotPlayer.rc.senseNearbyLocationsWithLead(GeneralManager.myType.visionRadiusSquared);
        MapLocation nearestLocation = null;
        for (int i = 0; i < locations.length; i++) {
            if ((nearestLocation == null || myLocation.distanceSquaredTo(locations[i]) < myLocation.distanceSquaredTo(nearestLocation))
                && !myLocation.equals(locations[i])) {
                nearestLocation = locations[i];
            }
        }
        return nearestLocation;
    }
}
