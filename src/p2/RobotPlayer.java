package p2;

import battlecode.common.*;

import java.util.PriorityQueue;
import java.util.Random;
import java.util.ArrayList;
import java.util.Hashtable;

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


    static final int DROID_VISION_RADIUS = 20;
    static final int BUILDING_ACTION_RADIUS = 20;

    static MapLocation target;

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
                    case MINER:      runMiner(rc);   break;
                    case SOLDIER:    runSoldier(rc); break;
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
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runArchon(RobotController rc) throws GameActionException {
        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rng.nextBoolean()) {
            // Let's try to build a miner.
//            rc.setIndicatorString("Trying to build a miner");
            if (rc.canBuildRobot(RobotType.MINER, dir)) {
                rc.buildRobot(RobotType.MINER, dir);
            }
        } else {
            // Let's try to build a soldier.
//            rc.setIndicatorString("Trying to build a soldier");
            if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                rc.buildRobot(RobotType.SOLDIER, dir);
            }
        }
    }

    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runMiner(RobotController rc) throws GameActionException {
        // Try to mine on squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
                while (rc.canMineLead(mineLocation)) {
                    rc.mineLead(mineLocation);
                }
            }
        }

//        rc.senseNearbyLocationsWithLead()


        MapLocation currentLoc = rc.getLocation();
        int radius = rc.getType().visionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(16, opponent);
//        ArrayList<MapLocation> enemyLocs = new ArrayList<>();

        MapLocation enemyLoc = null;
        //test with one enemy for now

        for(RobotInfo enemy : enemies) {
            if(enemy.type.canAttack()) {
//                enemyLocs.add(enemy.location);
                enemyLoc = enemy.location;
                break;
            }
        }

//        double[] h = new double[directions.length];
        double maxH = 0;
        Direction movementDir = null;

        // If enemy attack units in vision range, move away; else, move randomly
        if(enemyLoc != null) {
            for(int i = 0; i < directions.length; i++) {
                if(!rc.canMove(directions[i])) continue;
                double h = calcHEnemy(rc,rc.adjacentLocation(directions[i]),enemyLoc);
                if(h > maxH) {
                    maxH = h;
                    movementDir = directions[i];
                }
            }
            if(movementDir != null)
                rc.move(movementDir);
        }
        else {
//            movementDir = directions[rng.nextInt(directions.length)];
//            if (rc.canMove(movementDir)) {
//                rc.move(movementDir);
////            System.out.println("I moved!");
//            }
            for(int i = 0; i < directions.length; i++) {
                if(!rc.canMove(directions[i])) continue;
                double h = calcH(rc,rc.adjacentLocation(directions[i]));
                if(h > maxH) {
                    maxH = h;
                    movementDir = directions[i];
                }
            }
            if(movementDir != null)
                rc.move(movementDir);
            else {
                movementDir = directions[rng.nextInt(directions.length)];
                if (rc.canMove(movementDir)) {
                    rc.move(movementDir);
    //            System.out.println("I moved!");
                }
            }
        }
    }

    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runSoldier(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
            }
        }

        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
//            System.out.println("I moved!");
        }
    }

    static MapLocation findTarget(RobotController rc) {
        return null;
    }

    static double calcH(RobotController rc, MapLocation loc) throws GameActionException {
        MapLocation currentLoc = rc.getLocation();
        return (10*rc.senseGold(loc)+rc.senseLead(loc))/(rc.senseRubble(loc)+sqDistBetween(currentLoc,loc));
    }

    static double calcHGoal(RobotController rc, MapLocation loc, MapLocation goal) throws GameActionException {
        MapLocation currentLoc = rc.getLocation();
        return (10*rc.senseGold(loc)+rc.senseLead(loc))/(rc.senseRubble(loc)+sqDistBetween(currentLoc,loc)) *
                sqDistBetween(currentLoc,goal)/sqDistBetween(loc,goal);
    }

    static double calcHEnemy(RobotController rc, MapLocation loc, MapLocation enemyLoc) throws GameActionException {
        MapLocation currentLoc = rc.getLocation();
        return (10*rc.senseGold(loc)+rc.senseLead(loc) + 10)/(rc.senseRubble(loc)+sqDistBetween(currentLoc,loc)) +
                sqDistBetween(loc,enemyLoc)/sqDistBetween(currentLoc,enemyLoc);
    }

    static int sqDistBetween(MapLocation loc1, MapLocation loc2) {
        return (loc1.x - loc2.x)*(loc1.x - loc2.x) + (loc1.y - loc2.y)*(loc1.y - loc2.y);
    }

    static ArrayList bfs(MapLocation s, MapLocation g, RobotController rc) {
        PriorityQueue<MapLocation> open = new PriorityQueue<>();
        ArrayList<MapLocation> closed = new ArrayList<>();
        Hashtable<Integer, Boolean> visited = new Hashtable<>();

        open.add(s); // robot's current location
        int x = s.x, y = s.y;

        while(!open.isEmpty()) {
            MapLocation u = open.remove();
            closed.add(u);

            if(u.equals(g)) {
                return closed;
            }
            else {
                for(int i = -1; i <= 1; i++) {
                    for(int j = -1; j <= 1; j++) {
                        if(i != 0 || j != 0) {
                            if(x + 1 < rc.getMapWidth()) {

                            }
                        }
                    }
                }
            }
        }

        return null;
    }
}

