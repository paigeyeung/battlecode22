package m2;

import battlecode.common.*;

strictfp class AllUnitStrategy {
    static boolean updatedArchonsLastTurn = false;

    /** Called by RobotPlayer before any other function */
    static void runAllEarly(RobotController rc) throws GameActionException {
        if (!ArchonTrackerManager.receivedArchonTrackers) {
            // Read ally Archons from shared array indicies 0-3 and enemy Archons from indicies 4-7
            if (rc.getRoundNum() > 1) {
                // If this is the first turn of the game, wait until next turn before reading so ally Archons can broadcast first
                int numArchons = 0;
                while (numArchons <= 3) {
                    int element = rc.readSharedArray(CommunicationManager.ALLY_ARCHON_TRACKERS_INDEX + numArchons);
                    if (element == 0) {
                        break;
                    }
                    numArchons++;
                }

                ArchonTrackerManager.allyArchonTrackers = new ArchonTrackerManager.AllyArchonTracker[numArchons];
                for (int i = 0; i < numArchons; i++) {
                    ArchonTrackerManager.allyArchonTrackers[i] = ArchonTrackerManager.decodeAllyArchonTracker(rc.readSharedArray(CommunicationManager.ALLY_ARCHON_TRACKERS_INDEX + i));
                }

                ArchonTrackerManager.enemyArchonTrackers = new ArchonTrackerManager.EnemyArchonTracker[numArchons];
                for (int i = 0; i < numArchons; i++) {
                    ArchonTrackerManager.enemyArchonTrackers[i] = ArchonTrackerManager.decodeEnemyArchonTracker(rc.readSharedArray(CommunicationManager.ENEMY_ARCHON_TRACKERS_INDEX + i));
                }

                // Identify which Archon built me
                // This should only be for droids
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
    }

    /** Called by RobotPlayer after any other function */
    static void runAllLate(RobotController rc) throws GameActionException {
        MapLocation myLocation = rc.getLocation();

        // If this is near the start of the game, check if this is the first time we see an enemy near this Archon
        // It would be nice for Archons to run this too, but may screw up ArchonResourceManager if shared array is modified in between Archon turns
        if (ArchonTrackerManager.receivedArchonTrackers && Clock.getBytecodesLeft() > 1000
            && rc.getType() != RobotType.ARCHON) {
            int encodedGeneralStrategy0 = rc.readSharedArray(CommunicationManager.GENERAL_STRATEGY_INDEX);
            int nearestAllyArchonIndex = ArchonTrackerManager.getAllyArchonIndex(ArchonTrackerManager.getNearestAllyArchon(myLocation));
            boolean seenEnemy = ((encodedGeneralStrategy0 >>> nearestAllyArchonIndex) & 0x1) == 1;
            if (!seenEnemy) {
                // There is an enemy
                if (rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent()).length > 0) {
                    encodedGeneralStrategy0 = encodedGeneralStrategy0 | (1 << nearestAllyArchonIndex);
                    rc.writeSharedArray(CommunicationManager.GENERAL_STRATEGY_INDEX, encodedGeneralStrategy0);
                    DebugManager.log(rc, "First time seen enemy near Archon " + nearestAllyArchonIndex);
                }
            }
        }

        // Check for updates to Archons from shared array
        if (ArchonTrackerManager.receivedArchonTrackers && Clock.getBytecodesLeft() > 1000) {
            for (int i = 0; i < ArchonTrackerManager.allyArchonTrackers.length; i++) {
                ArchonTrackerManager.AllyArchonTracker allyArchonTracker = ArchonTrackerManager.decodeAllyArchonTracker(rc.readSharedArray(CommunicationManager.ALLY_ARCHON_TRACKERS_INDEX + i));
                if (!ArchonTrackerManager.allyArchonTrackers[i].isEqualTo(allyArchonTracker)) {
                    // Update ArchonTrackerManager as well
                    if (ArchonTrackerManager.allyArchonTrackers[i].alive != allyArchonTracker.alive && rc.getType() == RobotType.ARCHON) {
                        ArchonResourceManager.allyArchonModels[i].alive = allyArchonTracker.alive;
                    }

                    ArchonTrackerManager.allyArchonTrackers[i].update(allyArchonTracker);
                }
                else if (rc.getType() == RobotType.ARCHON && ArchonTrackerManager.allyArchonTrackers[i].alive && updatedArchonsLastTurn && ArchonTrackerManager.allyArchonTrackers[i].toggle == allyArchonTracker.toggle) {
                    // Toggle did not change since last turn, so ally Archon is dead
                    ArchonTrackerManager.setAllyArchonDead(rc, i);
                }
            }
            for (int i = 0; i < ArchonTrackerManager.enemyArchonTrackers.length; i++) {
                ArchonTrackerManager.EnemyArchonTracker enemyArchonTracker = ArchonTrackerManager.decodeEnemyArchonTracker(rc.readSharedArray(CommunicationManager.ENEMY_ARCHON_TRACKERS_INDEX + i));
                if (!ArchonTrackerManager.enemyArchonTrackers[i].isEqualTo(enemyArchonTracker)) {
                    ArchonTrackerManager.enemyArchonTrackers[i].update(enemyArchonTracker);
                }
            }
            updatedArchonsLastTurn = true;
        }
        else {
            updatedArchonsLastTurn = false;
        }

        // If an enemy Archon is seen or destroyed, broadcast it to shared array
        if (ArchonTrackerManager.receivedArchonTrackers && Clock.getBytecodesLeft() > 1000) {
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
                            rc.writeSharedArray(CommunicationManager.ENEMY_ARCHON_TRACKERS_INDEX + i, encodedEnemyArchonTracker);
                            DebugManager.log(rc, "Broadcasted enemy Archon dead " + guessLocation + " as " + encodedEnemyArchonTracker);
                        }
                    }
                    else if (!ArchonTrackerManager.enemyArchonTrackers[i].seen) {
                        if (enemyArchonSeen) {
                            // This is the first time we've seen it
                            ArchonTrackerManager.enemyArchonTrackers[i].seen = true;
                            int encodedEnemyArchonTracker = ArchonTrackerManager.encodeEnemyArchonTracker(ArchonTrackerManager.enemyArchonTrackers[i]);
                            rc.writeSharedArray(CommunicationManager.ENEMY_ARCHON_TRACKERS_INDEX + i, encodedEnemyArchonTracker);
                            DebugManager.log(rc, "Broadcasted enemy Archon seen " + guessLocation + " as " + encodedEnemyArchonTracker);
                        }
                    }
                }
            }
        }
    }
}
