package m6;

import battlecode.common.*;

strictfp class AllUnitStrategy {
    static boolean updatedArchonsLastTurn = false;

    /** Called by RobotPlayer before any other function */
    static void runAllEarly() throws GameActionException {
        if (!ArchonTrackerManager.receivedArchonTrackers) {
            // Read ally Archons from shared array indicies 0-3 and enemy Archons from indicies 4-7
            if (RobotPlayer.rc.getRoundNum() > 1) {
                // If this is the first turn of the game, wait until next turn before reading so ally Archons can broadcast first
                int numArchons = 0;
                while (numArchons <= 3) {
                    int element = RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_TRACKERS_INDEX + numArchons);
                    if (element == 0) {
                        break;
                    }
                    numArchons++;
                }

                ArchonTrackerManager.allyArchonTrackers = new ArchonTrackerManager.AllyArchonTracker[numArchons];
                for (int i = 0; i < numArchons; i++) {
                    ArchonTrackerManager.decodeAndUpdateLocalAllyArchonTracker(i, true);
                }

                ArchonTrackerManager.enemyArchonTrackers = new ArchonTrackerManager.EnemyArchonTracker[numArchons];
                for (int i = 0; i < numArchons; i++) {
                    ArchonTrackerManager.decodeAndUpdateLocalEnemyArchonTracker(i, true);
                }

                // Identify which Archon built me
                // This should only be for droids
                ArchonTrackerManager.myStartingArchonIndex = ArchonTrackerManager.getNearestAllyArchon(GeneralManager.startingLocation);

                ArchonTrackerManager.receivedArchonTrackers = true;
            }
        }
    }

    /** Called by RobotPlayer after any other function */
    static void runAllLate() throws GameActionException {
        if (!ArchonTrackerManager.receivedArchonTrackers) {
            return;
        }

        // If this is near the start of the game, check if this is the first time we see an enemy near this Archon
        // It would be nice for Archons to run this too, but may screw up ArchonResourceManager if shared array is modified in between Archon turns
        if (Clock.getBytecodesLeft() > 1000 && GeneralManager.myType != RobotType.ARCHON) {
            int encodedAllyArchonAdditionalInfo = RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_ADDITIONAL_INFO);
            int nearestAllyArchon = ArchonTrackerManager.getNearestAllyArchon(GeneralManager.myLocation);
            boolean seenEnemy = ((encodedAllyArchonAdditionalInfo >>> nearestAllyArchon) & 0x1) == 1;
            if (!seenEnemy) {
                // There is an enemy
                if (RobotPlayer.rc.senseNearbyRobots(GeneralManager.myType.visionRadiusSquared, RobotPlayer.rc.getTeam().opponent()).length > 0) {
                    encodedAllyArchonAdditionalInfo = encodedAllyArchonAdditionalInfo | (1 << nearestAllyArchon);
                    RobotPlayer.rc.writeSharedArray(CommunicationManager.ALLY_ARCHON_ADDITIONAL_INFO, encodedAllyArchonAdditionalInfo);
                    DebugManager.log("First time seen enemy near Archon " + nearestAllyArchon);
                }
            }
        }

        // Check for updates to Archons from shared array
        if (Clock.getBytecodesLeft() > 1000) {
            for (int i = 0; i < ArchonTrackerManager.allyArchonTrackers.length; i++) {
                boolean toggleBefore = ArchonTrackerManager.allyArchonTrackers[i].toggle;
                ArchonTrackerManager.decodeAndUpdateLocalAllyArchonTracker(i, false);
                boolean toggleAfter = ArchonTrackerManager.allyArchonTrackers[i].toggle;
                if (GeneralManager.myType == RobotType.ARCHON && ArchonStrategy.mySharedArrayIndex != i && ArchonTrackerManager.allyArchonTrackers[i].alive && updatedArchonsLastTurn && toggleBefore == toggleAfter) {
                    // Toggle did not change since last turn, so ally Archon is dead
                    ArchonTrackerManager.setAllyArchonAlive(i, false);
                }
            }
            for (int i = 0; i < ArchonTrackerManager.enemyArchonTrackers.length; i++) {
                ArchonTrackerManager.decodeAndUpdateLocalEnemyArchonTracker(i, false);
            }
            updatedArchonsLastTurn = true;
        }
        else {
            updatedArchonsLastTurn = false;
        }

        // If an enemy Archon is seen or destroyed, broadcast it to shared array
        if (Clock.getBytecodesLeft() > 1000) {
            int visionRadiusSquared = GeneralManager.myType.visionRadiusSquared;

            // Check for alive and nonmissing enemy Archons
            for (int i = 0; i < ArchonTrackerManager.enemyArchonTrackers.length; i++) {
                if (!ArchonTrackerManager.enemyArchonTrackers[i].alive
                    || ArchonTrackerManager.enemyArchonTrackers[i].missing) {
                    continue;
                }

                MapLocation guessLocation = ArchonTrackerManager.enemyArchonTrackers[i].getGuessLocation();
                // if (rc.canSenseLocation(estimatedLocation)) {
                // ^ Doesn't work for some reason, bug in Battlecode?
                if (GeneralManager.myLocation.distanceSquaredTo(guessLocation) <= visionRadiusSquared - 2) {
                    RobotInfo robotInfo = RobotPlayer.rc.senseRobotAtLocation(guessLocation);
                    boolean enemyArchonSeen = !(robotInfo == null || robotInfo.getType() != RobotType.ARCHON || robotInfo.getTeam() == RobotPlayer.rc.getTeam());
                    if (ArchonTrackerManager.enemyArchonTrackers[i].seen) {
                        if (!enemyArchonSeen) {
                            // We've seen it before and now it's gone, so assume it's dead
                            ArchonTrackerManager.setEnemyArchonAlive(i, false);
                        }
                    }
                    else {
                        if (enemyArchonSeen) {
                            // This is the first time we've seen it
                            ArchonTrackerManager.setEnemyArchonSeen(i, true);
                        }
                        else {
                            // We're here and we don't see it, and no one else has either
                            DebugManager.log("Enemy Archon missing at " + guessLocation);
                            ArchonTrackerManager.goToEnemyArchonNextGuessLocation(i);
                        }
                    }
                }
            }

            // Check for alive and missing enemy Archons
            int numEnemyArchonsMissing = ArchonTrackerManager.numEnemyArchonMissing();
            if (numEnemyArchonsMissing > 0) {
                RobotInfo[] robotInfos = RobotPlayer.rc.senseNearbyRobots(visionRadiusSquared, RobotPlayer.rc.getTeam().opponent());
                for (RobotInfo robotInfo : robotInfos) {
                    if (robotInfo.getType() == RobotType.ARCHON) {
                        if (!ArchonTrackerManager.existsEnemyArchonAtLocation(robotInfo.location)) {
                            ArchonTrackerManager.unmissingEnemyArchon(robotInfo.location);
                            numEnemyArchonsMissing--;
                            if (numEnemyArchonsMissing == 0) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
