package m8w1;

import battlecode.common.*;

strictfp class ArchonResourceManager {
    static final int MAX_DISTANCE_TO_NEARBY_ALLY_ARCHON = 25;
    static final double MINER_BUILD_PROPORTION = 0.15;
    static int farthestArchonIndex;
    static AnomalyScheduleEntry[] anomalyScheduleEntries;

    enum ARCHON_ACTIONS {
        BUILD_MINER,
        BUILD_BUILDER,
        BUILD_SOLDIER,
        REPAIR,
        DO_NOTHING,
        MOVE
    }

    static class ArchonModel {
        int index;
        boolean alive;
        boolean onCooldown;
        int nearestEnemyArchon;
        int nearestEnemyArchonDistanceSquared;

        ARCHON_ACTIONS archonAction;

        int droidsBuilt = 0;
        int minersBuilt = 0;
        int buildersBuilt = 0;
        int soldiersBuilt = 0;

        ArchonModel(int _myIndex) {
            index = _myIndex;
            alive = true;

            // Each Archon builds a miner on turn 1, before this runs
            minersBuilt++;
            droidsBuilt++;
        }

        void updateNearestEnemyArchon() {
            nearestEnemyArchon = ArchonTrackerManager.getNearestEnemyArchon(ArchonTrackerManager.allyArchonTrackers[index].location);
            if (nearestEnemyArchon == -1) {
                nearestEnemyArchonDistanceSquared = 10000;
            }
            else {
                if(ArchonTrackerManager.enemyArchonTrackers[nearestEnemyArchon].getGuessLocation() != null)
                    nearestEnemyArchonDistanceSquared =
                        ArchonTrackerManager.allyArchonTrackers[index].location.distanceSquaredTo(ArchonTrackerManager.enemyArchonTrackers[nearestEnemyArchon].getGuessLocation());
            }
        }

        void setActionBuildMiner() {
            archonAction = ARCHON_ACTIONS.BUILD_MINER;
            onCooldown = true;
            minersBuilt++;
            droidsBuilt++;
        }
        void setActionBuildBuilder() {
            archonAction = ARCHON_ACTIONS.BUILD_BUILDER;
            onCooldown = true;
            buildersBuilt++;
            droidsBuilt++;
        }
        void setActionBuildSoldier() {
            archonAction = ARCHON_ACTIONS.BUILD_SOLDIER;
            onCooldown = true;
            soldiersBuilt++;
            droidsBuilt++;
        }
        void setActionMove() {
            archonAction = ARCHON_ACTIONS.MOVE;
            onCooldown = true;
        }
        void setActionDoNothing() {
            archonAction = ARCHON_ACTIONS.DO_NOTHING;
        }
    }

    static ArchonModel[] allyArchonModels;

    /** Initialize, should always be called on turn 1 */
    static void initializeTurn1() {
        anomalyScheduleEntries = RobotPlayer.rc.getAnomalySchedule();
        // Could broadcast amount of visible lead here
    }

    /** Initialize, should always be called on turn 2 */
    static void initializeTurn2() throws GameActionException {
        allyArchonModels = new ArchonModel[ArchonTrackerManager.allyArchonTrackers.length];
        for (int i = 0; i < allyArchonModels.length; i++) {
            allyArchonModels[i] = new ArchonModel(i);
        }

        farthestArchonIndex = findArchonFarthestFromEnemies(true);

        RobotPlayer.rc.writeSharedArray(CommunicationManager.SAVED_ENEMY_COMBAT_SCORE, 0);
    }

    static void setArchonAlive(int index, boolean alive) {
        allyArchonModels[index].alive = alive;
    }

    /** Compute Archon actions */
    static void computeArchonActions() throws GameActionException {
        int turn = RobotPlayer.rc.getRoundNum();
        int totalDroidsBuilt = 0;
        int totalMinersBuilt = 0;
        int totalBuildersBuilt = 0;
        int totalSoldiersBuilt = 0;
        // We include dead ally Archon numbers in this calculation
        for (int i = 0; i < allyArchonModels.length; i++) {
            totalDroidsBuilt += allyArchonModels[i].droidsBuilt;
            totalMinersBuilt += allyArchonModels[i].minersBuilt;
            totalBuildersBuilt += allyArchonModels[i].buildersBuilt;
            totalSoldiersBuilt += allyArchonModels[i].soldiersBuilt;

            allyArchonModels[i].onCooldown = false;
        }

        // Read from shared array ARCHON_RESOURCE_MANAGER_INDEX
        int encodedResourceManager0 = RobotPlayer.rc.readSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX);
        int encodedResourceManager1 = RobotPlayer.rc.readSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX + 1);
        int lead = encodedResourceManager0 >>> 4;
        int gold = encodedResourceManager1 >>> 4;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if (!allyArchonModels[i].alive) {
                continue;
            }
            allyArchonModels[i].onCooldown = ((encodedResourceManager0 >>> i) & 0x1) == 1;
            allyArchonModels[i].setActionDoNothing();
        }

        // Read from shared array ALLY_ARCHON_ADDITIONAL_INFO
        int encodedAllyArchonAdditionalInfo = RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_ADDITIONAL_INFO);
        boolean anySeenEnemy = false;
        boolean seenEnemyArchons[] = new boolean[allyArchonModels.length]; // Not used to modify which Archon produces soldiers at the moment
        for (int i = 0; i < allyArchonModels.length; i++) {
            if (!allyArchonModels[i].alive) {
                continue;
            }
            seenEnemyArchons[i] = ((encodedAllyArchonAdditionalInfo >>> i) & 0x1) == 1;
            if (!anySeenEnemy && seenEnemyArchons[i]) {
                anySeenEnemy = true;
            }
        }

        // Compute move Archon actions

        for (int i = 0; i < allyArchonModels.length; i++) {
            if (!allyArchonModels[i].alive || allyArchonModels[i].onCooldown) {
                continue;
            }
            MapLocation farthestAllyArchonLoc = ArchonTrackerManager.allyArchonTrackers[farthestArchonIndex].location;

//            MapLocation closestAllyArchonLoc = ArchonTrackerManager.allyArchonTrackers[findArchonClosestToEnemies()].location;
//            MapLocation nearestEnemyArchonLoc = ArchonTrackerManager.getNearestEnemyArchonGuessLocation(ArchonTrackerManager.allyArchonTrackers[i].location);

//            if (ArchonTrackerManager.allyArchonTrackers[i].location.distanceSquaredTo(farthestAllyArchonLoc) > MAX_DISTANCE_TO_NEARBY_ALLY_ARCHON) {
//                allyArchonModels[i].setActionMove();
//                DebugManager.log("I'm Archon " + ArchonStrategy.mySharedArrayIndex + " and I want ally Archon " + i + " at " + ArchonTrackerManager.allyArchonTrackers[i].location + " to move");
//            }
        }

        while (true) {
            RobotType chosenBuild = null;
            // If an enemy has not been seen at any ally Archon, build only Miners
            // Unless too many miners already

            if (!anySeenEnemy && (totalMinersBuilt < 8 ||
                    (totalMinersBuilt < 20 && totalMinersBuilt < totalDroidsBuilt * 0.7))) {
                chosenBuild = RobotType.MINER;
            }
            // Maintain 15% proportion of build miners
            else if (totalMinersBuilt < totalDroidsBuilt * MINER_BUILD_PROPORTION) {
                chosenBuild = RobotType.MINER;
            }
            // Otherwise, build soldiers
            else {
                chosenBuild = RobotType.SOLDIER;
            }

            if(turn < 100 &&
                    allyArchonModels[findArchonWithClosestEnemy()].nearestEnemyArchonDistanceSquared < 25 &&
                    totalMinersBuilt >= 2) {
                chosenBuild = RobotType.SOLDIER;
            }

            if (chosenBuild == null) continue;

            if (chosenBuild == RobotType.MINER) {
                if (lead < 50) {
                    break;
                }
                int chosenArchonIndex = findArchonWithFewestMinersBuilt(true);
                if (chosenArchonIndex == -1) {
                    break;
                }

                allyArchonModels[chosenArchonIndex].setActionBuildMiner();
                lead -= 50;
            }
            else if (chosenBuild == RobotType.SOLDIER) {
                if (lead < 75) {
                    break;
                }

                int chosenArchonIndex = findArchonNeedingSoldiers(true);
                        //findArchonWithFewestSoldiersBuilt(true);

                if(turn < 100 &&
                        allyArchonModels[findArchonWithClosestEnemy()].nearestEnemyArchonDistanceSquared < 25)
                    chosenArchonIndex = findArchonWithClosestEnemy();

                if (chosenArchonIndex == -1) {
                    break;
                }

                allyArchonModels[chosenArchonIndex].setActionBuildSoldier();
                lead -= 75;
            }

//            }
//            else if(RobotPlayer.rc.getMode().canMove) {
//                int chosenArchonIndex = findArchonWithFewestSoldiersBuilt(true, true);
//                if (chosenArchonIndex == -1) {
//                    break;
//                }

//                int farthestArchonIndex = findArchonFarthestFromEnemies();
//                if (farthestArchonIndex == -1) {
//                    break;
//                }
//
//                for (int i = 0; i < allyArchonModels.length; i++) {
//                    if (farthestArchonIndex != i)
//                        allyArchonModels[i].setActionMove();
//                }
//            }
        }
    }

    /** Get action for specific Archon */
    static ARCHON_ACTIONS getArchonAction(int index) {
        return allyArchonModels[index].archonAction;
    }

    /** Helper functions to find Archons that meet criteria, returns -1 if no match is found */
    static int findArchonWithFewestDroidsBuilt(boolean ableToBuild) throws GameActionException {
        int fewestIndex = -1;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if ((fewestIndex == -1 || allyArchonModels[i].droidsBuilt < allyArchonModels[fewestIndex].droidsBuilt)
                && (!ableToBuild || (allyArchonModels[i].alive && !allyArchonModels[i].onCooldown
                && !ArchonTrackerManager.isMovingArchon(i)))) {
                fewestIndex = i;
            }
        }
        return fewestIndex;
    }
    static int findArchonWithFewestMinersBuilt(boolean ableToBuild) throws GameActionException {
        int fewestIndex = -1;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if ((fewestIndex == -1 || allyArchonModels[i].minersBuilt < allyArchonModels[fewestIndex].minersBuilt)
                && (!ableToBuild || (allyArchonModels[i].alive && !allyArchonModels[i].onCooldown
                && !ArchonTrackerManager.isMovingArchon(i)))) {
                fewestIndex = i;
            }
        }
        return fewestIndex;
    }
    static int findArchonWithFewestBuildersBuilt(boolean ableToBuild) throws GameActionException {
        int fewestIndex = -1;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if ((fewestIndex == -1 || allyArchonModels[i].buildersBuilt < allyArchonModels[fewestIndex].buildersBuilt)
                    && (!ableToBuild || (allyArchonModels[i].alive && !allyArchonModels[i].onCooldown
                    && !ArchonTrackerManager.isMovingArchon(i)))) {
                fewestIndex = i;
            }
        }
        return fewestIndex;
    }

    static int findArchonWithFewestSoldiersBuilt(boolean ableToBuild) throws GameActionException {
        int fewestIndex = -1;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if ((fewestIndex == -1 || allyArchonModels[i].soldiersBuilt < allyArchonModels[fewestIndex].soldiersBuilt)
                    && (!ableToBuild || (allyArchonModels[i].alive && !allyArchonModels[i].onCooldown &&
                    !ArchonTrackerManager.isMovingArchon(i)))) {
                fewestIndex = i;
            }
        }
        return fewestIndex;
    }

    static int findArchonNeedingSoldiers(boolean ableToBuild) throws GameActionException {
        int maxScore = 0;
        int index = -1;
        for(int i = 0; i <= 1; i++) {
            for(int j = 0; j <= 1; j++) {
                //for index i * 2 + (1 - j)
                int score = ((RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_ENEMY_COMBAT_SCORE+i) >>> (7*j)) & 0x7F);
                if(score > maxScore && allyArchonModels[i * 2 + (1 - j)].alive &&
                        (!ableToBuild || (allyArchonModels[i * 2 + (1 - j)].alive && !allyArchonModels[i * 2 + (1 - j)].onCooldown))) {
                    maxScore = score;
                    index = i * 2 + (1 - j);
                    // 00 1 | 01 0 | 10 3 | 11 2
                }
            }
        }
        if(index != -1) return index;
        return findArchonWithFewestSoldiersBuilt(ableToBuild);
    }

    static int findArchonWithClosestEnemy() {
        int archonIndex = -1;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if(allyArchonModels[i].nearestEnemyArchonDistanceSquared < minDist) {
                minDist = allyArchonModels[i].nearestEnemyArchonDistanceSquared;
                archonIndex = i;
            }
        }
        return archonIndex;
    }

    static int findArchonFarthestFromEnemies(boolean allEnemies) {
        int farthestIndex = -1;
        int maxDist = 0;
        if (allEnemies) {
            for (int i = allyArchonModels.length - 1; i >= 0; i--) {
                if (!allyArchonModels[i].alive) continue;
                    int dist = 0;
                    for (ArchonTrackerManager.EnemyArchonTracker enemyArchonTracker : ArchonTrackerManager.enemyArchonTrackers) {
                        if(enemyArchonTracker.getGuessLocation() != null)
                            dist += ArchonTrackerManager.allyArchonTrackers[i].location.distanceSquaredTo(enemyArchonTracker.getGuessLocation());
                    }
                    if (dist > maxDist) {
                        farthestIndex = i;
                    }
            }
        }
        else {
            for (int i = allyArchonModels.length - 1; i >= 0; i--) {
                if (farthestIndex == -1 ||
                        allyArchonModels[i].nearestEnemyArchonDistanceSquared > allyArchonModels[farthestIndex].nearestEnemyArchonDistanceSquared)
                    farthestIndex = i;
            }
        }
        return farthestIndex;
    }

    static int findArchonClosestToEnemies() {
        int closestIndex = -1;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if(allyArchonModels[i].alive &&
                    (closestIndex == -1 || allyArchonModels[i].nearestEnemyArchonDistanceSquared < allyArchonModels[closestIndex].nearestEnemyArchonDistanceSquared))
                closestIndex = i;
        }
        return closestIndex;
    }
}
