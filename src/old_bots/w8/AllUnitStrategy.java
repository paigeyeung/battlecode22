package w8;

import battlecode.common.*;

import java.util.Arrays;

strictfp class AllUnitStrategy {
    /** Called by RobotPlayer before any other function */
    static void runAllEarly(RobotController rc) throws GameActionException {
        // Read ally Archons from shared array indicies 0-3 and enemy Archons from indicies 4-7
        // If this is the first turn of the game, wait until next turn before reading so ally Archons can broadcast first
        if (!ArchonTrackerManager.receivedArchonTrackers && !(rc.getType() == RobotType.ARCHON && GeneralManager.turnCount == 1)) {
            int numArchons = 0;
            while (numArchons <= 3) {
                int element = rc.readSharedArray(numArchons);
                if (element == 0) {
                    break;
                }
                numArchons++;
            }

            ArchonTrackerManager.allyArchonTrackers = new ArchonTrackerManager.AllyArchonTracker[numArchons];
            for (int i = 0; i < numArchons; i++) {
                ArchonTrackerManager.allyArchonTrackers[i] = ArchonTrackerManager.decodeAllyArchonTracker(rc.readSharedArray(i));
            }

            ArchonTrackerManager.enemyArchonTrackers = new ArchonTrackerManager.EnemyArchonTracker[numArchons];
            for (int i = 0; i < numArchons; i++) {
                ArchonTrackerManager.enemyArchonTrackers[i] = ArchonTrackerManager.decodeEnemyArchonTracker(rc.readSharedArray(i + 4));
            }

            // Identify which Archon built me
            ArchonTrackerManager.AllyArchonTracker myStartingArchon = ArchonTrackerManager.getNearestAllyArchon(GeneralManager.startingLocation);
            for (int i = 0; i < ArchonTrackerManager.allyArchonTrackers.length; i++) {
                if (myStartingArchon == ArchonTrackerManager.allyArchonTrackers[i]) {
                    ArchonTrackerManager.myStartingArchonIndex = i;
                    break;
                }
            }

            ArchonTrackerManager.receivedArchonTrackers = true;
        }
    }

    /** Called by RobotPlayer after any other function */
    static void runAllLate(RobotController rc) throws GameActionException {
        MapLocation myLocation = rc.getLocation();

        // If droid
        if (Arrays.asList(GeneralManager.DROIDS).contains(rc.getType())) {
            if (ArchonTrackerManager.receivedArchonTrackers && Clock.getBytecodesLeft() > 2000) {
                // Check for updates to Archons from shared array
                for (int i = 0; i < ArchonTrackerManager.allyArchonTrackers.length; i++) {
                    ArchonTrackerManager.AllyArchonTracker allyArchonTracker = ArchonTrackerManager.decodeAllyArchonTracker(rc.readSharedArray(i));
                    if (allyArchonTracker != ArchonTrackerManager.allyArchonTrackers[i]) {
                        ArchonTrackerManager.allyArchonTrackers[i] = allyArchonTracker;
                    }
                }
                for (int i = 0; i < ArchonTrackerManager.enemyArchonTrackers.length; i++) {
                    ArchonTrackerManager.EnemyArchonTracker enemyArchonTracker = ArchonTrackerManager.decodeEnemyArchonTracker(rc.readSharedArray(i + 4));
                    if (enemyArchonTracker != ArchonTrackerManager.enemyArchonTrackers[i]) {
                        ArchonTrackerManager.enemyArchonTrackers[i] = enemyArchonTracker;
                    }
                }

                // If an enemy Archon is seen or destroyed, broadcast it to shared array
                int visionRadiusSquared = rc.getType().visionRadiusSquared;
                for (int i = 0; i < ArchonTrackerManager.enemyArchonTrackers.length; i++) {
                    MapLocation guessLocation = ArchonTrackerManager.enemyArchonTrackers[i].guessLocation;
                    // if (rc.canSenseLocation(estimatedLocation)) {
                    // ^ Doesn't work for some reason, bug in Battlecode?
                    if (myLocation.distanceSquaredTo(guessLocation) <= visionRadiusSquared) {
                        RobotInfo robotInfo = rc.senseRobotAtLocation(guessLocation);
                        boolean enemyArchonSeen = !(robotInfo == null || robotInfo.getType() != RobotType.ARCHON || robotInfo.getTeam() == rc.getTeam());
                        if (ArchonTrackerManager.enemyArchonTrackers[i].seen && ArchonTrackerManager.enemyArchonTrackers[i].alive) {
                            if (!enemyArchonSeen) {
                                // We've seen it before and now it's gone, so assume it's dead
                                ArchonTrackerManager.enemyArchonTrackers[i].alive = false;
                                int encodedEnemyArchonTracker = ArchonTrackerManager.encodeEnemyArchonTracker(ArchonTrackerManager.enemyArchonTrackers[i]);
                                rc.writeSharedArray(i + 4, encodedEnemyArchonTracker);
                                System.out.println("Broadcasted enemy Archon dead " + guessLocation + " as " + encodedEnemyArchonTracker);
                            }
                        }
                        else if (!ArchonTrackerManager.enemyArchonTrackers[i].seen) {
                            if (enemyArchonSeen) {
                                // This is the first time we've seen it
                                ArchonTrackerManager.enemyArchonTrackers[i].seen = true;
                                int encodedEnemyArchonTracker = ArchonTrackerManager.encodeEnemyArchonTracker(ArchonTrackerManager.enemyArchonTrackers[i]);
                                rc.writeSharedArray(i + 4, encodedEnemyArchonTracker);
                                System.out.println("Broadcasted enemy Archon seen " + guessLocation + " as " + encodedEnemyArchonTracker);
                            }
                        }
                    }
                }
            }
        }
    }
}
