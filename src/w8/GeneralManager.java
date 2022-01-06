package w8;

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
    static final RobotType[] PEACEFUL = {
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

    static final Random rng = new Random(1);
    static Direction getRandomDirection() {
        return DIRECTIONS[rng.nextInt(DIRECTIONS.length)];
    }

    static int turnCount = 0;
    static MapLocation startingLocation = null;
    static int mapWidth = 0, mapHeight = 0;

    /**
     * Get the horizontally and vertically mirrored location on the map
     */
    static MapLocation getOppositeLocation(MapLocation location, boolean flipHorizontal, boolean flipVertical) {
        return new MapLocation(flipHorizontal ? (mapWidth - location.x - 1) : location.x, flipVertical ? (mapHeight - location.y - 1) : location.y);
    }

    /**
     * Get the valid build direction nearest to the preferred direction
     * Returns null if no direction is found
     */
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
            System.out.println("SOMETHING WENT WRONG: getBuildDirection didn't find index of preferred direction");
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

    /**
     * Try to move
     * Returns true if successful
     */
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
