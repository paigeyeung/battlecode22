package m1w1;

import battlecode.common.*;

import java.util.Random;

strictfp class GeneralManager {
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

    static final Random rng = new Random(4);
    static Direction getRandomDirection() {
        return DIRECTIONS[rng.nextInt(DIRECTIONS.length)];
    }

    static int turnsAlive = 0;
    static MapLocation startingLocation = null;
    static int mapWidth = 0, mapHeight = 0;

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

    // Get direction to get to destination with less rubble
    static Direction getNextDir(RobotController rc, MapLocation dest) throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        if(currLoc.equals(dest)) return null;
        Direction movementDir = null;
        int minDist = getSqDistance(currLoc, dest);
        int f = Integer.MAX_VALUE;//minDist + rubble;

        for(Direction dir : DIRECTIONS) {
            if(rc.canMove(dir)) {
                MapLocation adj = rc.adjacentLocation(dir);
                int newDist = getSqDistance(adj,dest);
                int newRubble = rc.senseRubble(adj);
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

    /** Get build direction closest to preferred direction, returns null if no direction is found */
    static Direction getBuildDirection(RobotController rc, RobotType robotType, Direction preferredDirection) {
        if (preferredDirection == null) {
            preferredDirection = getRandomDirection();
        }
        if (rc.canBuildRobot(robotType, preferredDirection)) {
            return preferredDirection;
        }
        int preferredDirectionIndex = -1;
        for (int i = 0; i < DIRECTIONS.length; i++) {
            if (DIRECTIONS[i] == preferredDirection) {
                preferredDirectionIndex = i;
            }
        }
        if (preferredDirectionIndex == -1) {
            DebugManager.log(rc, "SOMETHING WENT WRONG: getBuildDirection didn't find index of preferred direction");
            return null;
        }
        int leftDirectionIndex = preferredDirectionIndex - 1;
        int rightDirectionIndex = preferredDirectionIndex + 1;
        for (int i = 0; i < 4; i++) {
            if (leftDirectionIndex < 0) {
                leftDirectionIndex = DIRECTIONS.length - 1;
            }
            Direction leftDirection = DIRECTIONS[leftDirectionIndex];
            if (rc.canBuildRobot(robotType, leftDirection)) {
                return leftDirection;
            }
            leftDirectionIndex--;

            if (rightDirectionIndex >= DIRECTIONS.length) {
                rightDirectionIndex = 0;
            }
            Direction rightDirection = DIRECTIONS[rightDirectionIndex];
            if (rc.canBuildRobot(robotType, rightDirection)) {
                return rightDirection;
            }
            rightDirectionIndex++;
        }
        return null;
    }

    /** Try to move, returns boolean if successful */
    static boolean tryMove(RobotController rc, Direction direction, boolean moveRandomlyIfFailed) throws GameActionException {
        if (direction != null && rc.canMove(direction)) {
            rc.move(direction);
            return true;
        }

        // If the attempted move didn't work, move randomly
        if (moveRandomlyIfFailed) {
            int randAttempts = 0;
            while ((direction == null || !rc.canMove(direction)) && randAttempts <= 10) {
                direction = getRandomDirection();
                randAttempts++;
            }
            if (rc.canMove(direction)) {
                rc.move(direction);
                return true;
            }
        }
        return false;
    }
}
