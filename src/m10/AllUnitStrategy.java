package m10;

import battlecode.common.*;

strictfp class AllUnitStrategy {
    static boolean updatedArchonsLastTurn = false;

    /** Called by RobotPlayer before any other function */
    static void runAllEarly() throws GameActionException {
//        if (GeneralManager.myType == RobotType.ARCHON) {
//            DebugManager.log("BYTECODE: " + Clock.getBytecodeNum() + " at runAllEarly point 1");
//        }

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

//        if (GeneralManager.myType == RobotType.ARCHON) {
//            DebugManager.log("BYTECODE: " + Clock.getBytecodeNum() + " at runAllEarly point 2");
//        }
    }

    /** Called by RobotPlayer after any other function */
    static void runAllLate() throws GameActionException {
//        if (GeneralManager.myType == RobotType.ARCHON) {
//            DebugManager.log("BYTECODE: " + Clock.getBytecodeNum() + " at runAllLate point 1");
//        }

        if (!ArchonTrackerManager.receivedArchonTrackers) {
            return;
        }

        // Update resource locations
        if (Clock.getBytecodesLeft() > 2000 && GeneralManager.turnsAlive % 2 == 0 && GeneralManager.iAmDroid) {
            ResourceLocationsManager.updateResourceLocations();
        }

        // If this is near the start of the game, check if this is the first time we see an enemy near this ally Archon
        // It would be nice for Archons to run this too, but may screw up ArchonResourceManager if shared array is modified in between Archon turns
        if (Clock.getBytecodesLeft() > 2000 && GeneralManager.myType != RobotType.ARCHON) {
            int encodedAllyArchonAdditionalInfo = RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_ADDITIONAL_INFO);
            int nearestAllyArchon = ArchonTrackerManager.getNearestAllyArchon(GeneralManager.myLocation);
            boolean seenEnemy = ((encodedAllyArchonAdditionalInfo >>> nearestAllyArchon) & 0x1) == 1;
            if (!seenEnemy) {
                // There is an enemy
                if (RobotPlayer.rc.senseNearbyRobots(GeneralManager.myType.visionRadiusSquared, GeneralManager.opponentTeam).length > 0) {
                    // It is close enough to ally Archon
                    if (ArchonTrackerManager.getNearestAllyArchonLocation(GeneralManager.myLocation).distanceSquaredTo(GeneralManager.myLocation) <= 150) {
                        encodedAllyArchonAdditionalInfo = encodedAllyArchonAdditionalInfo | (1 << nearestAllyArchon);
                        RobotPlayer.rc.writeSharedArray(CommunicationManager.ALLY_ARCHON_ADDITIONAL_INFO, encodedAllyArchonAdditionalInfo);
                        DebugManager.log("First time seen enemy near ally Archon " + nearestAllyArchon);
                    }
                }
            }
        }

        // Check for updates to Archons from shared array
        if (Clock.getBytecodesLeft() > 2000) {
            for (int i = 0; i < ArchonTrackerManager.allyArchonTrackers.length; i++) {
                boolean toggleBefore = ArchonTrackerManager.allyArchonTrackers[i].toggle;
                ArchonTrackerManager.decodeAndUpdateLocalAllyArchonTracker(i, false);
                boolean toggleAfter = ArchonTrackerManager.allyArchonTrackers[i].toggle;
                if (GeneralManager.myType == RobotType.ARCHON && (ArchonStrategy.mySharedArrayIndex != i) &&
                        ArchonTrackerManager.allyArchonTrackers[i].alive && updatedArchonsLastTurn && toggleBefore == toggleAfter) {
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
        if (Clock.getBytecodesLeft() > 2000 && updatedArchonsLastTurn) {
            // Check for alive and nonmissing enemy Archons
            for (int i = 0; i < ArchonTrackerManager.enemyArchonTrackers.length; i++) {
                if (!ArchonTrackerManager.enemyArchonTrackers[i].alive
                    || ArchonTrackerManager.enemyArchonTrackers[i].missing) {
                    continue;
                }

                MapLocation guessLocation = ArchonTrackerManager.enemyArchonTrackers[i].getGuessLocation();
                // if (rc.canSenseLocation(estimatedLocation)) {
                // ^ Doesn't work for some reason, bug in Battlecode?
                if (guessLocation != null &&
                    GeneralManager.myLocation.distanceSquaredTo(guessLocation) <= GeneralManager.myType.visionRadiusSquared) {
                    RobotInfo robotInfo = RobotPlayer.rc.senseRobotAtLocation(guessLocation);
                    boolean enemyArchonSeen = !(robotInfo == null || robotInfo.getType() != RobotType.ARCHON || robotInfo.getTeam() == GeneralManager.myTeam);
                    if (ArchonTrackerManager.enemyArchonTrackers[i].seen) {
                        if (!enemyArchonSeen) {
//                            // We've seen it before and now it's gone, so assume it's dead
//                            ArchonTrackerManager.setEnemyArchonAlive(i, false);

                            // We've seen it before and now it's gone, so mark it as missing
                            ArchonTrackerManager.setEnemyArchonMissing(i);
                        }
                    } else {
                        if (enemyArchonSeen) {
                            // This is the first time we've seen it
                            ArchonTrackerManager.setEnemyArchonSeen(i, true);
                        } else {
                            // We're here and we don't see it, and no one else has either
                            DebugManager.log("Enemy Archon " + i + " missing at " + guessLocation);
                            ArchonTrackerManager.goToEnemyArchonNextGuessLocation(i);
                        }
                    }
                }
            }
        }
        if (Clock.getBytecodesLeft() > 2000 && updatedArchonsLastTurn) {
//            // Check for alive and missing enemy Archons
//            int numEnemyArchonsMissing = ArchonTrackerManager.numEnemyArchonMissing();
//            if (numEnemyArchonsMissing > 0) {
//                RobotInfo[] robotInfos = RobotPlayer.rc.senseNearbyRobots(visionRadiusSquared, GeneralManager.enemyTeam);
//                for (RobotInfo robotInfo : robotInfos) {
//                    if (robotInfo.getType() == RobotType.ARCHON) {
//                        if (!ArchonTrackerManager.existsEnemyArchonAtLocation(robotInfo.location)) {
//                            ArchonTrackerManager.foundEnemyArchon(robotInfo.location);
//                            numEnemyArchonsMissing--;
//                            if (numEnemyArchonsMissing == 0) {
//                                break;
//                            }
//                        }
//                    }
//                }
//            }

            // Check for alive and unseen enemy Archons
            RobotInfo[] robotInfos = RobotPlayer.rc.senseNearbyRobots(GeneralManager.myType.visionRadiusSquared, GeneralManager.opponentTeam);
            for (RobotInfo robotInfo : robotInfos) {
                if (robotInfo.getType() == RobotType.ARCHON) {
                    if (!ArchonTrackerManager.existsEnemyArchonAtLocation(robotInfo.location)) {
                        ArchonTrackerManager.foundEnemyArchon(robotInfo.location);
                    }
                }
            }
        }

        if(Clock.getBytecodesLeft() > 2000) {
            int buildingInfo = RobotPlayer.rc.readSharedArray(CommunicationManager.BUILDING_INFO);
            if (((buildingInfo >>> 2) & 0x1) == 1 &&
                    (RobotPlayer.rc.getRoundNum() % 4 != (buildingInfo & 0x3) &&
                            (RobotPlayer.rc.getRoundNum()-1) % 4 != (buildingInfo & 0x3))) {
                RobotPlayer.rc.writeSharedArray(CommunicationManager.BUILDING_INFO,
                        ((buildingInfo >>> 3) << 3) | ((0 << 2) | (RobotPlayer.rc.getRoundNum() % 4)));
            }
            else if (((buildingInfo >>> 5) & 0x1) == 1 &&
                    (RobotPlayer.rc.getRoundNum() % 4 != ((buildingInfo >>> 3) & 0x3) &&
                            (RobotPlayer.rc.getRoundNum()-1) % 4 != ((buildingInfo >>> 3) & 0x3))) {
                RobotPlayer.rc.writeSharedArray(CommunicationManager.BUILDING_INFO,
                        ((buildingInfo >>> 6) << 6) | ((0 << 5) | (buildingInfo & 0xF)));
            }
        }

//        if (GeneralManager.myType == RobotType.ARCHON) {
//            DebugManager.log("BYTECODE: " + Clock.getBytecodeNum() + " at runAllLate point 2");
//        }
    }
}
