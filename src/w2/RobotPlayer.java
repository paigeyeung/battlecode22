package w2;

import battlecode.common.*;

import java.util.Random;
import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;
    static MapLocation startingLocation = null;
    static int mapWidth = 0, mapHeight = 0;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
//    static final Random rng = new Random(6147);
    static final Random rng = new Random();

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static final RobotType[] buildings = {
            RobotType.ARCHON,
            RobotType.LABORATORY,
            RobotType.WATCHTOWER
    };
    static final RobotType[] droids = {
            RobotType.MINER,
            RobotType.BUILDER,
            RobotType.SOLDIER,
            RobotType.SAGE
    };

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
//        System.out.println("I'm a " + rc.getType() + " and I just got created! I have health " + rc.getHealth());

        // You can also use indicators to save debug notes in replays.
//        rc.setIndicatorString("Hello world!");

        startingLocation = rc.getLocation();
        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the RobotType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                runAllEarly(rc);
                switch (rc.getType()) {
                    case ARCHON:     runArchon(rc);  break;
                    case MINER:      runMiner(rc);   break;
                    case BUILDER:    break;
                    case SOLDIER:    runSoldier(rc); break;
                    case SAGE:       break;
                    case LABORATORY: break;
                    case WATCHTOWER: break;
                }
                runAllLate(rc);
            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    /**
     * Get the horizontally and vertically mirrored location on the map
     */
    static MapLocation getOppositeLocation(MapLocation location, boolean flipHorizontal, boolean flipVertical) {
        return new MapLocation(flipHorizontal ? (mapWidth - location.x - 1) : location.x, flipVertical ? (mapHeight - location.y - 1) : location.y);
    }

    /**
     * Archon tracker
     */
    static class ArchonTracker {
        boolean alive;

        ArchonTracker(boolean _alive) {
            alive = _alive;
        }
    }
    static class MyArchonTracker extends ArchonTracker {
        MapLocation location;

        MyArchonTracker(boolean _alive, MapLocation _location) {
            super(_alive);
            location = _location;
        }
    }
    static class EnemyArchonTracker extends ArchonTracker {
        boolean seen;
        MapLocation myArchonStartingLocation;
        MapLocation guessLocation;
        ArrayDeque<MapLocation> guessLocations;

        EnemyArchonTracker(boolean _alive, boolean _seen, MapLocation _myArchonStartingLocation, MyArchonTracker[] _myArchonTrackers) {
            super(_alive);
            seen = _seen;
            myArchonStartingLocation = _myArchonStartingLocation;

            guessLocations = new ArrayDeque<>();
            for (int i = 0; i <= 1; i++) {
                for (int j = 0; j <= 1; j++) {
                    MapLocation location = getOppositeLocation(myArchonStartingLocation, i == 0, j == 0);
                    boolean isValidGuess = true;
                    for (int k = 0; k < _myArchonTrackers.length; k++) {
                        if (location.distanceSquaredTo(_myArchonTrackers[k].location) <= 2) {
                            isValidGuess = false;
                            break;
                        }
                    }
                    if (isValidGuess) {
                        guessLocations.add(location);
                    }
                }
            }
            goToNextGuessLocation();
        }

        void goToNextGuessLocation() {
            guessLocation = guessLocations.removeFirst();
        }
    }
    static boolean receivedArchonTrackers = false;
    static MyArchonTracker[] myArchonTrackers;
    static EnemyArchonTracker[] enemyArchonTrackers;
    static int encodeMyArchonTracker(MyArchonTracker myArchonTracker) {
        return myArchonTracker.location.x << 8 | myArchonTracker.location.y << 2 | (myArchonTracker.alive ? 1 : 0) << 1;
    }
    static int encodeEnemyArchonTracker(EnemyArchonTracker enemyArchonTracker) {
        return enemyArchonTracker.myArchonStartingLocation.x << 8 | enemyArchonTracker.myArchonStartingLocation.y << 2 | (enemyArchonTracker.alive ? 1 : 0) << 1 | (enemyArchonTracker.seen ? 1 : 0);
    }
    static int encodeEnemyArchonTracker(boolean alive, boolean seen, MapLocation location) {
        return location.x << 8 | location.y << 2 | (alive ? 1 : 0) << 1 | (seen ? 1 : 0);
    }
    static MyArchonTracker decodeMyArchonTracker(int encoded) {
        return new MyArchonTracker(((encoded >> 1) & 0x1) == 1, new MapLocation((encoded >> 8) & 0x3F, (encoded >> 2) & 0x3F));
    }
    static EnemyArchonTracker decodeEnemyArchonTracker(int encoded) {
        return new EnemyArchonTracker(((encoded >> 1) & 0x1) == 1, (encoded & 0x1) == 1, new MapLocation((encoded >> 8) & 0x3F, (encoded >> 2) & 0x3F), myArchonTrackers);
    }
    static MyArchonTracker getNearestMyArchon(MapLocation fromLocation) {
        MyArchonTracker nearest = null;
        for (int i = 0; i < myArchonTrackers.length; i++) {
            if (myArchonTrackers[i].alive && (nearest == null || myArchonTrackers[i].location.distanceSquaredTo(fromLocation) < nearest.location.distanceSquaredTo(fromLocation))) {
                nearest = myArchonTrackers[i];
            }
        }
        return nearest;
    }
    static EnemyArchonTracker getNearestEnemyArchon(MapLocation fromLocation) {
        EnemyArchonTracker nearest = null;
        for (int i = 0; i < enemyArchonTrackers.length; i++) {
            if (enemyArchonTrackers[i].alive && (nearest == null || enemyArchonTrackers[i].guessLocation.distanceSquaredTo(fromLocation) < nearest.guessLocation.distanceSquaredTo(fromLocation))) {
                nearest = enemyArchonTrackers[i];
            }
        }
        return nearest;
    }

    /**
     * Run a single turn for all unit types, before unit-specific functions are called.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runAllEarly(RobotController rc) throws GameActionException {
        // Read my Archons from shared array indicies 0-3 and enemy Archons from indicies 4-7
        // If this is the first turn of the game, wait until next turn before reading so my Archons can broadcast first
        if (!receivedArchonTrackers && !(rc.getType() == RobotType.ARCHON && turnCount == 1)) {
            int numArchons = 0;
            while (numArchons <= 3) {
                int element = rc.readSharedArray(numArchons);
                if (element == 0) {
                    break;
                }
                numArchons++;
            }

            myArchonTrackers = new MyArchonTracker[numArchons];
            for (int i = 0; i < numArchons; i++) {
                myArchonTrackers[i] = decodeMyArchonTracker(rc.readSharedArray(i));
            }

            enemyArchonTrackers = new EnemyArchonTracker[numArchons];
            for (int i = 0; i < numArchons; i++) {
                enemyArchonTrackers[i] = decodeEnemyArchonTracker(rc.readSharedArray(i + 4));
            }

            receivedArchonTrackers = true;
        }
    }

    /**
     * Run a single turn for all unit types, after unit-specific functions are called.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runAllLate(RobotController rc) throws GameActionException {
        MapLocation myLocation = rc.getLocation();

        // If droid
        if (Arrays.asList(droids).contains(rc.getType())) {
            if (receivedArchonTrackers && Clock.getBytecodesLeft() > 2000) {
                // Check for updates to Archons from shared array
                for (int i = 0; i < myArchonTrackers.length; i++) {
                    MyArchonTracker myArchonTracker = decodeMyArchonTracker(rc.readSharedArray(i));
                    if (myArchonTracker != myArchonTrackers[i]) {
                        myArchonTrackers[i] = myArchonTracker;
                    }
                }
                for (int i = 0; i < enemyArchonTrackers.length; i++) {
                    EnemyArchonTracker enemyArchonTracker = decodeEnemyArchonTracker(rc.readSharedArray(i + 4));
                    if (enemyArchonTracker != enemyArchonTrackers[i]) {
                        enemyArchonTrackers[i] = enemyArchonTracker;
                    }
                }

                // If an enemy Archon is seen or destroyed, broadcast it to shared array
                int visionRadiusSquared = rc.getType().visionRadiusSquared;
                for (int i = 0; i < enemyArchonTrackers.length; i++) {
                    MapLocation guessLocation = enemyArchonTrackers[i].guessLocation;
                    // if (rc.canSenseLocation(estimatedLocation)) {
                    // ^ Doesn't work for some reason, bug in Battlecode?
                    if (myLocation.distanceSquaredTo(guessLocation) <= visionRadiusSquared) {
                        RobotInfo robotInfo = rc.senseRobotAtLocation(guessLocation);
                        boolean enemyArchonSeen = !(robotInfo == null || robotInfo.getType() != RobotType.ARCHON || robotInfo.getTeam() == rc.getTeam());
                        if (enemyArchonTrackers[i].seen && enemyArchonTrackers[i].alive) {
                            if (!enemyArchonSeen) {
                                // We've seen it before and now it's gone, so assume it's dead
                                enemyArchonTrackers[i].alive = false;
                                int encodedEnemyArchonTracker = encodeEnemyArchonTracker(enemyArchonTrackers[i]);
                                rc.writeSharedArray(i + 4, encodedEnemyArchonTracker);
                                System.out.println("Broadcasted enemy Archon dead " + guessLocation + " as " + encodedEnemyArchonTracker);
                            }
                        }
                        else if (!enemyArchonTrackers[i].seen) {
                            if (enemyArchonSeen) {
                                // This is the first time we've seen it
                                enemyArchonTrackers[i].seen = true;
                                int encodedEnemyArchonTracker = encodeEnemyArchonTracker(enemyArchonTrackers[i]);
                                rc.writeSharedArray(i + 4, encodedEnemyArchonTracker);
                                System.out.println("Broadcasted enemy Archon seen " + guessLocation + " as " + encodedEnemyArchonTracker);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static boolean archonBroadcastedLocation = false;
    static int archonMinersBuilt = 0;
    static int archonSoldiersBuilt = 0;
    static void runArchon(RobotController rc) throws GameActionException {
        // Broadcast my location in shared array indicies 0-3 and instantiate enemy in indicies 4-7
        if (!archonBroadcastedLocation) {
            // Find first empty array element
            int i = 0;
            while (i <= 3) {
                int element = rc.readSharedArray(i);
                if (element == 0) {
                    break;
                }
                i++;
            }
            if (i == 4) {
                System.out.println("SOMETHING WENT WRONG: Archon did not find empty array element");
            }
            else {
                MapLocation myLocation = rc.getLocation();
                int encodedMyArchonTracker = encodeMyArchonTracker(new MyArchonTracker(true, myLocation));
                rc.writeSharedArray(i, encodedMyArchonTracker);
                System.out.println("Broadcasted my Archon location " + myLocation + " as " + encodedMyArchonTracker);

                int encodedEnemyArchonTracker = encodeEnemyArchonTracker(true, false, myLocation);
                rc.writeSharedArray(i + 4, encodedEnemyArchonTracker);
                System.out.println("Broadcasted enemy Archon as " + encodedEnemyArchonTracker);

                archonBroadcastedLocation = true;
            }
        }

        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (archonMinersBuilt < archonSoldiersBuilt) {
            // Let's try to build a miner.
            rc.setIndicatorString("Trying to build a miner");
            if (rc.canBuildRobot(RobotType.MINER, dir)) {
                rc.buildRobot(RobotType.MINER, dir);
                archonMinersBuilt++;
            }
        } else {
            // Let's try to build a soldier.
            rc.setIndicatorString("Trying to build a soldier");
            if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                rc.buildRobot(RobotType.SOLDIER, dir);
                archonSoldiersBuilt++;
            }
        }
    }

    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runMiner(RobotController rc) throws GameActionException {
        MapLocation myLocation = rc.getLocation();

        // Try to mine gold
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(myLocation.x + dx, myLocation.y + dy);
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
            }
        }

        // Try to mine lead
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(myLocation.x + dx, myLocation.y + dy);
                while (rc.canMineLead(mineLocation)) {
                    rc.mineLead(mineLocation);
                }
            }
        }

        // Move randomly
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }

    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runSoldier(RobotController rc) throws GameActionException {
        MapLocation myLocation = rc.getLocation();

        // Try to attack
        RobotInfo[] actionableEnemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        if (actionableEnemies.length > 0) {
            RobotInfo targetEnemy = actionableEnemies[0];
            
//            RobotInfo targetEnemy = null;
//            for (int i = 0; i < actionableEnemies.length; i++) {
//                if (targetEnemy == null || actionableEnemies[i].getHealth() < targetEnemy.getHealth()) {
//                    targetEnemy = actionableEnemies[i];
//                }
//            }

            MapLocation toAttack = targetEnemy.location;
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
            }
        }

        // If no enemies are nearby, move towards nearest enemy Archon
        // Otherwise, move towards nearest my Archon
        Direction dir = null;
        if (receivedArchonTrackers) {
            RobotInfo[] visibleEnemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
            if (visibleEnemies.length == 0) {
                dir = myLocation.directionTo(getNearestEnemyArchon(myLocation).guessLocation);
            }
            else {
                dir = myLocation.directionTo(getNearestMyArchon(myLocation).location);
            }
        }
        // If the attempted move didn't work, move randomly
        int randAttempts = 0;
        while ((dir == null || !rc.canMove(dir)) && randAttempts <= 10) {
            dir = directions[rng.nextInt(directions.length)];
            randAttempts++;
        }
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}
