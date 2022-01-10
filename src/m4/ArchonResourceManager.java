package m4;

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
        int nearestEnemyArchon;
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
            nearestEnemyArchon = ArchonTrackerManager.getNearestEnemyArchon(ArchonTrackerManager.allyArchonTrackers[index].location);
            if (nearestEnemyArchon == -1) {
                nearestEnemyArchonDistanceSquared = 10000;
            }
            else {
                nearestEnemyArchonDistanceSquared = ArchonTrackerManager.allyArchonTrackers[index].location.distanceSquaredTo(ArchonTrackerManager.enemyArchonTrackers[nearestEnemyArchon].getGuessLocation());
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
        void setActionDoNothing() {
            archonAction = ARCHON_ACTIONS.DO_NOTHING;
        }
    }

    static ArchonModel[] allyArchonModels;

    /** Initialize, should always be called on turn 1 */
    static void initializeTurn1() {
        // Could broadcast amount of visible lead here
    }

    /** Initialize, should always be called on turn 2 */
    static void initializeTurn2() {
        allyArchonModels = new ArchonModel[ArchonTrackerManager.allyArchonTrackers.length];
        for (int i = 0; i < allyArchonModels.length; i++) {
            allyArchonModels[i] = new ArchonModel(i);
        }

        computeArchonRoles();
    }

    static void setArchonDead(int index) {
        allyArchonModels[index].alive = false;
        computeArchonRoles();
    }

    /** Compute Archon roles */
    static void computeArchonRoles() {
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
            DebugManager.log("Archon at location " + ArchonTrackerManager.allyArchonTrackers[i].location + " allyArchonModels[" + i + "].archonRole: " + allyArchonModels[i].archonRole);
        }
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

        // Read from shared array indicies 8-9
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

        // Read from shared array index 10
        int encodedGeneralStrategy0 = RobotPlayer.rc.readSharedArray(CommunicationManager.GENERAL_STRATEGY_INDEX);
        boolean anySeenEnemy = false;
        boolean seenEnemyArchons[] = new boolean[allyArchonModels.length]; // Not used to modify which Archon produces soldiers at the moment
        for (int i = 0; i < allyArchonModels.length; i++) {
            if (!allyArchonModels[i].alive) {
                continue;
            }
            seenEnemyArchons[i] = ((encodedGeneralStrategy0 >>> i) & 0x1) == 1;
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
