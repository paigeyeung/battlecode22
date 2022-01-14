package w1;

import battlecode.common.*;
import java.util.Random;

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

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!
//            System.out.println("Age: " + turnCount + "; Location: " + rc.getLocation());

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the RobotType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()) {
                    case ARCHON:     runArchon(rc);  break;
                    case MINER:      runMiner(rc);   break;
                    case BUILDER:    break;
                    case SOLDIER:    runSoldier(rc); break;
                    case SAGE:       break;
                    case LABORATORY: break;
                    case WATCHTOWER: break;
                }
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
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static int archonMinersBuilt = 0;
    static int archonSoldiersBuilt = 0;
    static void runArchon(RobotController rc) throws GameActionException {
        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rng.nextBoolean()) {
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

        // If no enemies are nearby, move towards enemy Archon
        // Otherwise, move towards my Archon
        // HARDCODED VALUES OF ENEMY ARCHON LOCATION
        Direction dir = null;
        RobotInfo[] visibleEnemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
        MapLocation archonALocation = new MapLocation(5, 5);
        MapLocation archonBLocation = new MapLocation(26, 26);
        if (visibleEnemies.length == 0) {
            MapLocation enemyArchonLocation = (rc.getTeam() == Team.A) ? archonBLocation : archonALocation;
            dir = myLocation.directionTo(enemyArchonLocation);
        }
        else {
            MapLocation myArchonLocation = (rc.getTeam() == Team.A) ? archonALocation : archonBLocation;
            dir = myLocation.directionTo(myArchonLocation);
        }
        int randAttempts = 0;
        while (!rc.canMove(dir) && randAttempts <= 10) {
            dir = directions[rng.nextInt(directions.length)];
            randAttempts++;
        }
        boolean canMove = randAttempts < 10;
        if (canMove) {
            rc.move(dir);
        }
    }
}
