package bytecodetest;

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

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);

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
                    case MINER:      break;
                    case SOLDIER:    break;
                    case LABORATORY: // Examplefuncsplayer doesn't use any of these robot types below.
                    case WATCHTOWER: // You might want to give them a try!
                    case BUILDER:
                    case SAGE:       break;
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
     * Results
     * Nothing / the tracker - 2 bytecode
     * sout - 2 bytecode
     * Storing rc.get result in variable instead of calling rc.get every time - costs slightly less
     * Initializing/incrementing static variable instead of local variable - costs significantly more
     * Storing rc in static variable instead of passing rc as argument - costs slightly less
     * Initializing local byte instead of int - costs the same - 2 bytecode
     * For loop 100 times using byte/short/long instead of int - costs significantly more
     * While loop 4 times using byte/short instead of int - costs significantly more
     * Every iteration of empty for loop - 5 bytecode
     * Create instance of static class - costs very little
     * Initialize int/class array - 1 bytecode per element
     * ((a >>> 2) & 0x1) == 1 - 11 bytecode
     * (a2 & (1 << 2)) != 0 - 8 bytecode
     */

    static void runArchon(RobotController rc) throws GameActionException {
        int mapWidth = 60, mapHeight = 60;
        MapLocation myLocation = rc.getLocation();

        startBytecodeTracker();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(myLocation.x + dx, myLocation.y + dy);
                rc.canMineLead(mineLocation);
            }
        }
        endBytecodeTracker();

        startBytecodeTracker();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(myLocation.x + dx, myLocation.y + dy);
                rc.canMineLead(mineLocation);
                rc.senseLead(mineLocation);
            }
        }
        endBytecodeTracker();

        System.out.println("Total bytecode used: " + Clock.getBytecodeNum());
        rc.resign();
    }

    static int testVar = 1;
    static RobotController rcStatic;
    static void testFunc1(RobotController rc) {
        rc.getType();
    }
    static void testFunc2() {
        rcStatic.getType();
    }
    static class TestClass {
        boolean alive;
        TestClass(boolean _alive) {
            alive = _alive;
        }
    }
    static class TestClass2 {
        boolean alive;
    }

    static int startingBytecode;
    static void startBytecodeTracker() {
        startingBytecode = Clock.getBytecodeNum();
    }
    static void endBytecodeTracker() {
        int endingBytecode = Clock.getBytecodeNum();
        int usedBytecode = endingBytecode - startingBytecode;
        usedBytecode -= 2;
        System.out.println("Bytecode used: " + usedBytecode);
    }
}
