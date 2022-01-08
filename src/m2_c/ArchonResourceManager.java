package m2_c;

import battlecode.common.*;

strictfp class ArchonResourceManager {
    enum ARCHON_ROLES {
        OFFENSIVE,
        NONOFFENSIVE,
        DO_NOTHING
    }

    enum ARCHON_ACTIONS {
        BUILD_MINER,
        BUILD_BUILDER,
        BUILD_SOLDIER,
        REPAIR,
        DO_NOTHING
    }

    static class ArchonModel {
        int index;
        boolean alive;
        boolean onCooldown;
        int nearestEnemyArchonIndex;
        int nearestEnemyArchonDistanceSquared;

        ARCHON_ROLES archonRole;
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
            ArchonTrackerManager.EnemyArchonTracker nearestEnemyArchon = ArchonTrackerManager.getNearestEnemyArchon(ArchonTrackerManager.allyArchonTrackers[index].location);
            nearestEnemyArchonIndex = ArchonTrackerManager.getEnemyArchonIndex(nearestEnemyArchon);
            nearestEnemyArchonDistanceSquared = ArchonTrackerManager.allyArchonTrackers[index].location.distanceSquaredTo(nearestEnemyArchon.guessLocation);
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
        void setActionDoNothing() {
            archonAction = ARCHON_ACTIONS.DO_NOTHING;
        }
    }

    static ArchonModel[] allyArchonModels;

    /** Initialize, should always be called on turn 1 */
    static void initializeTurn1(RobotController rc) {
        // Could broadcast amount of visible lead here
    }

    /** Initialize, should always be called on turn 2 */
    static void initializeTurn2(RobotController rc) {
        allyArchonModels = new ArchonModel[ArchonTrackerManager.allyArchonTrackers.length];
        for (int i = 0; i < allyArchonModels.length; i++) {
            allyArchonModels[i] = new ArchonModel(i);
        }

        computeArchonRoles(rc);
    }

    static void setArchonDead(RobotController rc, int index) {
        allyArchonModels[index].alive = false;
        computeArchonRoles(rc);
    }

    /** Compute Archon roles */
    static void computeArchonRoles(RobotController rc) {
        for (int i = 0; i < allyArchonModels.length; i++) {
            allyArchonModels[i].updateNearestEnemyArchon();
        }

        int closestToEnemyDistanceSquared = 10000;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if (!allyArchonModels[i].alive) {
                continue;
            }
            if (allyArchonModels[i].nearestEnemyArchonDistanceSquared < closestToEnemyDistanceSquared) {
                closestToEnemyDistanceSquared = allyArchonModels[i].nearestEnemyArchonDistanceSquared;
            }
        }

        for (int i = 0; i < allyArchonModels.length; i++) {
            if (!allyArchonModels[i].alive) {
                continue;
            }
            if (allyArchonModels[i].nearestEnemyArchonDistanceSquared - closestToEnemyDistanceSquared <= 10) {
                allyArchonModels[i].archonRole = ARCHON_ROLES.OFFENSIVE;
            }
            else {
                allyArchonModels[i].archonRole = ARCHON_ROLES.NONOFFENSIVE;
            }
            DebugManager.log(rc, "Archon at location " + ArchonTrackerManager.allyArchonTrackers[i].location + " allyArchonModels[" + i + "].archonRole: " + allyArchonModels[i].archonRole);
        }
    }

    /** Compute Archon actions */
    static void computeArchonActions(RobotController rc) throws GameActionException {
        int turn = rc.getRoundNum();
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

        // Read from shared array indicies 8-9
        int encodedIndex8 = rc.readSharedArray(8);
        int encodedIndex9 = rc.readSharedArray(9);
        int lead = encodedIndex8 >>> 4;
        int gold = encodedIndex9 >>> 4;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if (!allyArchonModels[i].alive) {
                continue;
            }
            allyArchonModels[i].onCooldown = ((encodedIndex8 >>> i) & 0x1) == 1;
            allyArchonModels[i].setActionDoNothing();
        }

        // Read from shared array index 10
        int encodedIndex10 = rc.readSharedArray(10);
        boolean anySeenEnemy = false;
        boolean seenEnemyArchons[] = new boolean[allyArchonModels.length]; // Not used to modify which Archon produces soldiers at the moment
        for (int i = 0; i < allyArchonModels.length; i++) {
            if (!allyArchonModels[i].alive) {
                continue;
            }
            seenEnemyArchons[i] = ((encodedIndex10 >>> i) & 0x1) == 1;
            if (!anySeenEnemy && seenEnemyArchons[i]) {
                anySeenEnemy = true;
            }
        }

        while (true) {
            RobotType chosenBuild = null;
            // If an enemy has not been seen at any ally Archon, build only Miners
            // Unless too many miners already
            if (!anySeenEnemy && (totalMinersBuilt < 40 || (totalMinersBuilt < 20 && totalMinersBuilt < totalDroidsBuilt * 0.8))) {
                chosenBuild = RobotType.MINER;
            }
            // Maintain 10% proportion of build miners
            else if (totalMinersBuilt < totalDroidsBuilt * 0.1) {
                chosenBuild = RobotType.MINER;
            }
            // Otherwise, build soldiers
            else {
                chosenBuild = RobotType.SOLDIER;
            }

            if (chosenBuild == null) {
                continue;
            }
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
                int chosenArchonIndex = findArchonWithFewestSoldiersBuilt(true, true);
                if (chosenArchonIndex == -1) {
                    break;
                }
                allyArchonModels[chosenArchonIndex].setActionBuildSoldier();
                lead -= 75;
            }
        }
    }

    /** Get action for specific Archon */
    static ARCHON_ACTIONS getArchonAction(int index) {
        return allyArchonModels[index].archonAction;
    }

    /** Helper functions to find Archons that meet criteria, returns -1 if no match is found */
    static int findArchonWithFewestDroidsBuilt(boolean ableToBuild) {
        int fewestIndex = -1;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if ((fewestIndex == -1 || allyArchonModels[i].droidsBuilt < allyArchonModels[fewestIndex].droidsBuilt)
                && (!ableToBuild || (allyArchonModels[i].alive && !allyArchonModels[i].onCooldown))) {
                fewestIndex = i;
            }
        }
        return fewestIndex;
    }
    static int findArchonWithFewestMinersBuilt(boolean ableToBuild) {
        int fewestIndex = -1;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if ((fewestIndex == -1 || allyArchonModels[i].minersBuilt < allyArchonModels[fewestIndex].minersBuilt)
                && (!ableToBuild || (allyArchonModels[i].alive && !allyArchonModels[i].onCooldown))) {
                fewestIndex = i;
            }
        }
        return fewestIndex;
    }
    static int findArchonWithFewestBuildersBuilt(boolean ableToBuild) {
        int fewestIndex = -1;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if ((fewestIndex == -1 || allyArchonModels[i].buildersBuilt < allyArchonModels[fewestIndex].buildersBuilt)
                && (!ableToBuild || (allyArchonModels[i].alive && !allyArchonModels[i].onCooldown))) {
                fewestIndex = i;
            }
        }
        return fewestIndex;
    }
    static int findArchonWithFewestSoldiersBuilt(boolean ableToBuild, boolean offensiveArchon) {
        int fewestIndex = -1;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if ((fewestIndex == -1 || allyArchonModels[i].soldiersBuilt < allyArchonModels[fewestIndex].soldiersBuilt)
                && (!ableToBuild || (allyArchonModels[i].alive && !allyArchonModels[i].onCooldown))
                && (!offensiveArchon || allyArchonModels[i].archonRole == ARCHON_ROLES.OFFENSIVE)) {
                fewestIndex = i;
            }
        }
        return fewestIndex;
    }
}
