package m9;

import battlecode.common.*;

strictfp class ArchonStrategy {
    static int mySharedArrayIndex = -1;
    static boolean mySharedArrayToggle = true;
    static boolean[][] visited = null;

    static int droidsBuilt = 0;
    static int minersBuilt = 0;
    static int buildersBuilt = 0;
    static int soldiersBuilt = 0;

    static MapLocation nearestLeadLocation;
    static MapLocation dest;

    static boolean movedToArchonsDest = false;
    static boolean movedOffRubble;

    /** Build a droid */
    static void archonBuild(RobotType robotType, Direction buildDirection) throws GameActionException {
        RobotPlayer.rc.buildRobot(robotType, buildDirection);
        switch (robotType) {
            case MINER: minersBuilt++; break;
            case BUILDER: buildersBuilt++; break;
            case SOLDIER: soldiersBuilt++; break;
        }
        droidsBuilt++;
    }

    /** Try to build a droid, returns boolean if successful */
    static boolean archonTryBuild(RobotType robotType) throws GameActionException {
        Direction preferredDirection = null;
        if (robotType == RobotType.MINER) {
            if (nearestLeadLocation != null) {
                preferredDirection = GeneralManager.myLocation.directionTo(nearestLeadLocation);
            }
        }
        else if (robotType == RobotType.SOLDIER) {
            preferredDirection = GeneralManager.myLocation.directionTo(ArchonTrackerManager.getNearestEnemyArchonGuessLocation(GeneralManager.myLocation));
        }
        Direction buildDirection = GeneralManager.getBuildDirection(robotType, preferredDirection);
        if (buildDirection != null) {
            archonBuild(robotType, buildDirection);
            return true;
        }
        return false;
    }

    static boolean archonTryMove() throws GameActionException {
        if(movedToArchonsDest) {
            if(!movedOffRubble)
                return archonTryMoveLowerRubble();
            return false;
        }

        MapLocation locFarthestFromEnemies;

//        if(ArchonResourceManager.findArchonFarthestFromEnemies(true) != -1)
//            locFarthestFromEnemies = ArchonTrackerManager.allyArchonTrackers[ArchonResourceManager.findArchonFarthestFromEnemies(true)].location;
//        else locFarthestFromEnemies = ArchonTrackerManager.allyArchonTrackers[ArchonResourceManager.farthestArchonIndex].location;

        locFarthestFromEnemies = ArchonTrackerManager.allyArchonTrackers[ArchonResourceManager.farthestArchonIndex].location;

        if (RobotPlayer.rc.getLocation().distanceSquaredTo(locFarthestFromEnemies) < ArchonResourceManager.MAX_DISTANCE_TO_NEARBY_ALLY_ARCHON) {
            if (RobotPlayer.rc.getMode().canMove && RobotPlayer.rc.canTransform()) {
                RobotPlayer.rc.transform();
                movedToArchonsDest = true;
                return false;
            }
            if(!RobotPlayer.rc.getMode().canMove)
                return false;
        }

        if (!RobotPlayer.rc.getMode().canMove) {
            if (RobotPlayer.rc.canTransform() &&
                    (locFarthestFromEnemies != null &&
                            RobotPlayer.rc.getLocation().distanceSquaredTo(locFarthestFromEnemies) >= ArchonResourceManager.MAX_DISTANCE_TO_NEARBY_ALLY_ARCHON)) {
                RobotPlayer.rc.transform();
                return true;
            }
            return false;
        }

        boolean moved = GeneralManager.tryMove(getNextArchonDir(locFarthestFromEnemies), false);

        if (moved) {
            ArchonTrackerManager.setAllyArchonLocation(mySharedArrayIndex, RobotPlayer.rc.getLocation());
        }

        return moved;
    }

    static boolean archonTryMoveLowerRubble() throws GameActionException {
        if(movedOffRubble) return false;

        MapLocation myLocation = RobotPlayer.rc.getLocation();
        MapLocation lowRubbleDest = null;

        if(dest == null || (!myLocation.equals(dest) &&
                RobotPlayer.rc.canSenseLocation(dest) && RobotPlayer.rc.canSenseRobotAtLocation(dest))) {
            int rubble = RobotPlayer.rc.senseRubble(myLocation), minRubble = RobotPlayer.rc.senseRubble(myLocation);
            for (MapLocation adj : RobotPlayer.rc.getAllLocationsWithinRadiusSquared(myLocation, 4)) {
                if (RobotPlayer.rc.senseRubble(adj) <= minRubble) {
                    if (RobotPlayer.rc.senseRubble(adj) < minRubble ||
                            (lowRubbleDest != null &&
                                    (myLocation.distanceSquaredTo(adj) < myLocation.distanceSquaredTo(lowRubbleDest)))) {
                        minRubble = RobotPlayer.rc.senseRubble(adj);
                        lowRubbleDest = adj;
                    }
                }
            }
            if (lowRubbleDest != null && minRubble < rubble - lowRubbleDest.distanceSquaredTo(myLocation)) {
                dest = lowRubbleDest;
            }
        }

        if(dest == null || myLocation.equals(dest)) {
            if(RobotPlayer.rc.getMode().canMove && RobotPlayer.rc.canTransform()) {
                dest = null;
                movedOffRubble = true;
                RobotPlayer.rc.transform();
                return false;
            }
            return true;
        }

        if (!RobotPlayer.rc.getMode().canMove) {
            if (RobotPlayer.rc.canTransform()) {
                RobotPlayer.rc.transform();
                return true;
            }
            return false;
        }

        boolean moved = GeneralManager.tryMove(getNextArchonDir(dest), false);

//        DebugManager.log("moved to (" + dest.x + "," + dest.y + ")? " + moved + getNextArchonDir(dest));

        if (moved) {
            ArchonTrackerManager.setAllyArchonLocation(mySharedArrayIndex, RobotPlayer.rc.getLocation());
        }

        return moved;
    }

    static Direction getNextArchonDir(MapLocation dest) throws GameActionException {
        MapLocation myLoc = RobotPlayer.rc.getLocation();

        if(myLoc.equals(dest) || dest == null) return null;

        Direction movementDir = null;
//        int minDist = myLoc.distanceSquaredTo(dest);
        int f = Integer.MAX_VALUE;

        for(Direction dir : GeneralManager.DIRECTIONS) {
            if(RobotPlayer.rc.canMove(dir)) {
                MapLocation adj = RobotPlayer.rc.adjacentLocation(dir);
                int newDist = adj.distanceSquaredTo(dest);
                int newRubble = RobotPlayer.rc.senseRubble(adj);
                int newF = newDist + newRubble;
                if (visited[adj.x][adj.y]) newF += 100;

                if (newF < f) {
                    f = newF;
                    movementDir = dir;
                    visited[adj.x][adj.y] = true;
                } else if (newF == f) {
                    if ((int) (Math.random() * 2) == 0) {
                        f = newF;
                        movementDir = dir;
                        visited[adj.x][adj.y] = true;
                    }
                }
            }
        }
        return movementDir;
    }

    static boolean archonTryRepair() throws GameActionException {
        RobotInfo[] actionableAllies = RobotPlayer.rc.senseNearbyRobots(GeneralManager.myType.actionRadiusSquared, RobotPlayer.rc.getTeam());
        for (int i = 0; i < actionableAllies.length; i++) {
            RobotInfo allyRobot = actionableAllies[i];
            if (allyRobot.getHealth() < allyRobot.getType().getMaxHealth(allyRobot.getLevel()) &&
                    !(allyRobot.getType().canAttack() && allyRobot.getHealth() >=
                            CombatManager.HEALTH_PERCENTAGE_THRESHOLD_FOR_DISINTEGRATING * allyRobot.getType().getMaxHealth(allyRobot.getLevel()))) {
                if (RobotPlayer.rc.canRepair(allyRobot.location)) {
                    RobotPlayer.rc.repair(allyRobot.location);
                    return true;
                }
            }
        }
        return false;
    }

    /** Called by RobotPlayer */
    static void runArchon() throws GameActionException {
//        DebugManager.log("BYTECODE: " + Clock.getBytecodeNum() + " at runArchon point 1");

        if (visited == null) {
            visited = new boolean[GeneralManager.mapWidth + 1][GeneralManager.mapHeight + 1];
        }

        // First turn initializations
        if (GeneralManager.turnsAlive == 1) {
            // Initialize Archon tracker manager
            // Find first empty array element
            mySharedArrayIndex = 0;
            while (mySharedArrayIndex <= 3) {
                int element = RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_TRACKERS_INDEX + mySharedArrayIndex);
                if (element == 0) {
                    break;
                }
                mySharedArrayIndex++;
            }
            if (mySharedArrayIndex == 4) {
                DebugManager.log("SOMETHING WENT WRONG: Archon did not find empty array element");
            }
            else {
                ArchonTrackerManager.updateGlobalAllyArchonTrackerFirstTime(mySharedArrayIndex, true, GeneralManager.myLocation, mySharedArrayToggle);
                ArchonTrackerManager.updateGlobalEnemyArchonTrackerFirstTime(mySharedArrayIndex, true, GeneralManager.myLocation, false, 0, false);
            }

            dest = null;
            movedOffRubble = false;

            // Initialize resource manager
            ArchonResourceManager.initializeTurn1();

            // Get nearest lead location
            nearestLeadLocation = GeneralManager.getNearestLeadLocation();
            if (nearestLeadLocation == null) {
                DebugManager.log("SOMETHING WENT WRONG: Nearest lead not found!");
            }

            // Build a miner
            archonTryBuild(RobotType.MINER);

            // Finish turn
            return;
        }

        // Turn 2 initializations
        if (GeneralManager.turnsAlive == 2) {
            // Update invalid enemy Archon guess locations
            int encoded = RobotPlayer.rc.readSharedArray(CommunicationManager.ENEMY_ARCHON_ADDITIONAL_INFO);
            if (encoded == 0) {
                for (int i = 0; i < ArchonTrackerManager.enemyArchonTrackers.length; i++) {
                    for (int j = 0; j < ArchonTrackerManager.enemyArchonTrackers[i].guessLocations.size(); j++) {
                        boolean valid = true;
                        for (int k = 0; k < ArchonTrackerManager.allyArchonTrackers.length; k++) {
                            if (ArchonTrackerManager.allyArchonTrackers[k].location.distanceSquaredTo(ArchonTrackerManager.enemyArchonTrackers[i].guessLocations.get(j)) <= 5) {
                                valid = false;
                                DebugManager.log("Enemy Archon " + i + " guess location " + ArchonTrackerManager.enemyArchonTrackers[i].guessLocations.get(j) + " is invalid because it's too close to ally Archon " + k + " location " + ArchonTrackerManager.allyArchonTrackers[k].location);
                                break;
                            }
                        }
                        if (!valid) {
                            encoded = encoded | (1 << (i * 4 + j));
                        }
                    }
                }
                RobotPlayer.rc.writeSharedArray(CommunicationManager.ENEMY_ARCHON_ADDITIONAL_INFO, encoded);
            }

            // Initialize resource manager
            ArchonResourceManager.initializeTurn2();
        }

//        DebugManager.log("BYTECODE: " + Clock.getBytecodeNum() + " at runArchon point 2");

        // Toggle bit in shared array ALLY_ARCHON_TRACKERS_INDEX to show alive
        mySharedArrayToggle = !mySharedArrayToggle;
        ArchonTrackerManager.setAllyArchonToggle(mySharedArrayIndex, mySharedArrayToggle);
        ArchonTrackerManager.updateGlobalAllyArchonTracker(mySharedArrayIndex);

//        DebugManager.log("BYTECODE: " + Clock.getBytecodeNum() + " at runArchon point 3");

        // If first alive Archon, write to shared array ARCHON_RESOURCE_MANAGER_INDEX
        // Reset the array indices except cooldowns last turn, then write lead and gold
        if (mySharedArrayIndex == ArchonTrackerManager.getFirstAliveAllyArchon()) {
            int encodedResourceManager0 = RobotPlayer.rc.readSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX);
            encodedResourceManager0 = encodedResourceManager0 & 0xF;
            int lead = RobotPlayer.rc.getTeamLeadAmount(RobotPlayer.rc.getTeam());
            if (lead > 0xFFF) {
                lead = 0xFFF;
            }
            encodedResourceManager0 = encodedResourceManager0 | lead << 4;

            int encodedResourceManager1 = 0;
            int gold = RobotPlayer.rc.getTeamGoldAmount(RobotPlayer.rc.getTeam());
            if (gold > 0xFFF) {
                gold = 0xFFF;
            }
            encodedResourceManager1 = encodedResourceManager1 | gold << 4;

            RobotPlayer.rc.writeSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX, encodedResourceManager0);
            RobotPlayer.rc.writeSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX + 1, encodedResourceManager1);
        }

//        DebugManager.log("BYTECODE: " + Clock.getBytecodeNum() + " at runArchon point 4");

//        if(RobotPlayer.rc.getRoundNum() > 200)

        if (RobotPlayer.rc.getMode() == RobotMode.PORTABLE) {
            // If we're portable, don't try to do anything else
            archonTryMove();
        }
        else {
            // Get and perform action from ArchonResourceManager
            ArchonResourceManager.computeArchonActions();
            ArchonResourceManager.ARCHON_ACTIONS action = ArchonResourceManager.getArchonAction(mySharedArrayIndex);
            if (action == ArchonResourceManager.ARCHON_ACTIONS.BUILD_MINER) {
                archonTryBuild(RobotType.MINER);
            }
            else if (action == ArchonResourceManager.ARCHON_ACTIONS.BUILD_BUILDER) {
                archonTryBuild(RobotType.BUILDER);
            }
            else if (action == ArchonResourceManager.ARCHON_ACTIONS.BUILD_SOLDIER) {
                archonTryBuild(RobotType.SOLDIER);
            }
            else if (action == ArchonResourceManager.ARCHON_ACTIONS.MOVE) {
                ArchonTrackerManager.setMoving(mySharedArrayIndex, archonTryMove());
            }
            else {
                archonTryRepair();
            }
        }

        if(RobotPlayer.rc.getRoundNum() > 2) {
            if (!movedToArchonsDest && ArchonTrackerManager.numArchonsMoving() < RobotPlayer.rc.getArchonCount() - 1)
                ArchonTrackerManager.setMoving(mySharedArrayIndex, archonTryMove());
            else if (!movedOffRubble && ArchonTrackerManager.numArchonsMoving() < RobotPlayer.rc.getArchonCount() - 1) {
                ArchonTrackerManager.setMoving(mySharedArrayIndex, archonTryMoveLowerRubble());
            }
        }

//        MapLocation mapCenter = GeneralManager.getMapCenter();
//        if(RobotPlayer.rc.getLocation().distanceSquaredTo(mapCenter) > 25)

//        DebugManager.log("BYTECODE: " + Clock.getBytecodeNum() + " at runArchon point 5");

        // Write to shared array ARCHON_RESOURCE_MANAGER_INDEX
        int encodedResourceManager0 = RobotPlayer.rc.readSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX);
        int encodedResourceManager1 = RobotPlayer.rc.readSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX + 1);

        double score = CombatManager.evaluateLocalCombatScore(RobotPlayer.rc.getTeam().opponent(), true)
                - CombatManager.evaluateLocalCombatScore(RobotPlayer.rc.getTeam(), false) + 500;

        if(score > 1270) score = 1270;
        if(score < 0) score = 0;

        int encoded = RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_ENEMY_COMBAT_SCORE + (int)(mySharedArrayIndex / 2));

        if((encoded >>> (7 * (1-mySharedArrayIndex % 2)) & 0x7F) != Math.round(score/10)) {
            int newEncodedScore;
            if (mySharedArrayIndex % 2 == 0) newEncodedScore = (int)Math.round(score/10) << 7 | (encoded & 0x7F);
            else newEncodedScore = ((encoded >>> 7) & 0x7F) | ((int)Math.round(score/10) & 0x7F);

            RobotPlayer.rc.writeSharedArray(CommunicationManager.ALLY_ARCHON_ENEMY_COMBAT_SCORE + (int)(mySharedArrayIndex / 2),
                    newEncodedScore);
        }

        for(AnomalyScheduleEntry e : ArchonResourceManager.anomalyScheduleEntries) {
            if(e.anomalyType.equals(AnomalyType.VORTEX) && e.roundNumber == RobotPlayer.rc.getRoundNum()) {
                movedOffRubble = false;
                visited = new boolean[GeneralManager.mapWidth + 1][GeneralManager.mapHeight + 1];
                break;
            }
        }

//        DebugManager.log("AT ARCHON " + mySharedArrayIndex + ": Enemy combat score is " + enemyCombatScore);
//        DebugManager.log("Archon 0: " + ((RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_ENEMY_COMBAT_SCORE) >>> 7) & 0x7F) +
//                "\nArchon 1: " + ((RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_ENEMY_COMBAT_SCORE)) & 0x7F) +
//                "\nArchon 2: " + ((RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_ENEMY_COMBAT_SCORE+1) >>> 7) & 0x7F) +
//                "\nArchon 3: " + ((RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_ENEMY_COMBAT_SCORE+1)) & 0x7F));

        boolean onCooldown = RobotPlayer.rc.getActionCooldownTurns() > 10 || RobotPlayer.rc.getMode() == RobotMode.PORTABLE;
        encodedResourceManager1 = encodedResourceManager1 | ((onCooldown ? 1 : 0) << mySharedArrayIndex);
        // If last alive Archon, copy cooldowns last turn to this turn
        if (mySharedArrayIndex == ArchonTrackerManager.getLastAliveAllyArchon()) {
            encodedResourceManager0 = encodedResourceManager0 & 0xFFF0;
            encodedResourceManager0 = encodedResourceManager0 | (encodedResourceManager1 & 0xF);
            RobotPlayer.rc.writeSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX, encodedResourceManager0);
        }
        RobotPlayer.rc.writeSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX + 1, encodedResourceManager1);

//        DebugManager.log("BYTECODE: " + Clock.getBytecodeNum() + " at runArchon point 6");
    }
}