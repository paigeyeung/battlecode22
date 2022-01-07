package p4;

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
    static final RobotType[] HOSTILE = {
        RobotType.WATCHTOWER,
        RobotType.SOLDIER,
        RobotType.SAGE
    };

    static int turnCount = 0;
    static MapLocation startingLocation = null;
    static int mapWidth = 0, mapHeight = 0;

    static final Random rng = new Random(1);
    static Direction getRandomDirection() {
        return DIRECTIONS[rng.nextInt(DIRECTIONS.length)];
    }

    /**
     * Get squared distance between two locations
     */
    static int getSqDistance(MapLocation loc1, MapLocation loc2) {
        return (loc1.x - loc2.x)*(loc1.x - loc2.x) + (loc1.y - loc2.y)*(loc1.y - loc2.y);
    }

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
     * Select an enemy to attack
     * Returns null if no enemy is found
     */
    static MapLocation getAttackTarget(RobotController rc, int radius) {
        RobotInfo[] actionableEnemies = rc.senseNearbyRobots(radius, rc.getTeam().opponent());
        RobotInfo targetEnemy = null;
        double targetEnemyScore = -1;
        for (int i = 0; i < actionableEnemies.length; i++) {
            RobotInfo thisEnemy = actionableEnemies[i];
            // Increase score by 0-10 based on percent of missing health
            double thisEnemyScore = 10 * (1 - thisEnemy.getHealth() / thisEnemy.getType().getMaxHealth(thisEnemy.level));
            // Increase score by 100 if I can one shot kill
            if (rc.getType().getDamage(rc.getLevel()) >= thisEnemy.getHealth()) {
                thisEnemyScore += 100;
            }
            // Increase score by 0-50 based on target type
            switch (thisEnemy.getType()) {
                case SAGE: thisEnemyScore += 50; break;
                case SOLDIER: thisEnemyScore += 35; break;
                case WATCHTOWER: thisEnemyScore += 30; break;
                case MINER: thisEnemyScore += 25; break;
                case ARCHON: thisEnemyScore += 20; break;
                case LABORATORY: thisEnemyScore += 10; break;
                case BUILDER: thisEnemyScore += 0; break;
            }
            if ((targetEnemy == null || thisEnemyScore > targetEnemyScore) && rc.canAttack(thisEnemy.location)) {
                targetEnemy = thisEnemy;
                targetEnemyScore = thisEnemyScore;
            }
        }
        if (targetEnemy == null) {
            return null;
        }
        return targetEnemy.location;
    }
    /**
     * Try to perform an attack
     * Returns true if successful
     */
    static boolean tryAttack(RobotController rc) throws GameActionException {
        MapLocation attackLocation = getAttackTarget(rc, rc.getType().actionRadiusSquared);
        if (attackLocation != null) {
            rc.attack(attackLocation);
            return true;
        }
        return false;
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
//                if (visited[adj.x][adj.y]) newF += 100;

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

    static Direction moveTo(RobotController rc, MapLocation dest) {
        MapLocation currLoc = rc.getLocation();
        if(currLoc.equals(dest)) return null;
        Direction destDir = Direction.CENTER;
        if(dest.x < currLoc.x && dest.y < currLoc.y) destDir = Direction.SOUTHWEST;
        else if(dest.x < currLoc.x && dest.y == currLoc.y) destDir = Direction.WEST;
        else if(dest.x < currLoc.x && dest.y > currLoc.y) destDir = Direction.NORTHWEST;
        else if(dest.x == currLoc.x && dest.y < currLoc.y) destDir = Direction.SOUTH;
        else if(dest.x == currLoc.x && dest.y > currLoc.y) destDir = Direction.NORTH;
        else if(dest.x > currLoc.x && dest.y < currLoc.y) destDir = Direction.SOUTHEAST;
        else if(dest.x > currLoc.x && dest.y == currLoc.y) destDir = Direction.EAST;
        else if(dest.x > currLoc.x && dest.y > currLoc.y) destDir = Direction.NORTHEAST;

        int indexOfDest = -1;
        for(int j = 0; j < DIRECTIONS.length; j++) {
            if(destDir.equals(DIRECTIONS[j])) {
                indexOfDest = j;
                break;
            }
        }
        int l = indexOfDest, r = indexOfDest, i = 0;

        while(i < 4) {
            if(rc.canMove(DIRECTIONS[l])) {
                return DIRECTIONS[l];
            }
            else if(rc.canMove(DIRECTIONS[r])) {
                return DIRECTIONS[r];
            }
            if(l<0)
                l = DIRECTIONS.length-l-2;
            else
                l--;
            r = DIRECTIONS.length % (r+1);
            i++;
        }
        return null;
    }

    /**
     * Evaluates combat score
     */
    static double calculateRobotCombatScore(RobotController rc, RobotInfo robot, boolean defensive) throws GameActionException {
        double score = 0;
        // Increase score by 0-100 based on target type
        if (defensive) {
            switch (robot.getType()) {
                case SAGE: score += 100; break;
                case SOLDIER: score += 10; break;
                case WATCHTOWER: score += 30; break;
                case ARCHON: score += 2; break;
                case BUILDER: score += 1; break;
                case LABORATORY: score += 0; break;
                case MINER: score -= 1; break;
            }
        }
        else {
            switch (robot.getType()) {
                case SAGE: score += 100; break;
                case SOLDIER: score += 10; break;
            }
        }
        // Decrease score based on percent of missing health
        score *= robot.getHealth() / robot.getType().getMaxHealth(robot.level);
        // Decrease score based on rubble
        score *= 1 / (1 + rc.senseRubble(robot.getLocation()) / 10);
        return score;
    }
    static double evaluateLocalCombatScore(RobotController rc, Team team, boolean defensive) throws GameActionException {
        RobotInfo[] visibleRobots = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, team);
        double combatScore = 0;
        for (int i = 0; i < visibleRobots.length; i++) {
            combatScore += calculateRobotCombatScore(rc, visibleRobots[i], defensive);
        }
        // Include self in calculation if on same team
        if (team == rc.getTeam()) {
            combatScore += calculateRobotCombatScore(rc, new RobotInfo(rc.getID(), rc.getTeam(), rc.getType(), rc.getMode(), rc.getLevel(), rc.getHealth(), rc.getLocation()), defensive);
        }
        return combatScore;
    }

    /**
     * Decides what a hostile droid should do
     */
    enum HOSTILE_DROID_ACTIONS {
        RETREAT,
        HOLD,
        ATTACK
    }
    static HOSTILE_DROID_ACTIONS getHostileDroidAction(RobotController rc) throws GameActionException {
        double myCombatScore = evaluateLocalCombatScore(rc, rc.getTeam(), false);
        double enemyCombatScore = evaluateLocalCombatScore(rc, rc.getTeam().opponent(), true);
        HOSTILE_DROID_ACTIONS chosenAction = HOSTILE_DROID_ACTIONS.ATTACK;
        if (enemyCombatScore > myCombatScore * 0.5) {
            chosenAction = HOSTILE_DROID_ACTIONS.RETREAT;
        }
        return chosenAction;
    }
}
