package m8w1;

import battlecode.common.*;

strictfp class CombatManager {
    enum COMBAT_DROID_ACTIONS {
        RETREAT,
        HOLD,
        ATTACK
    }

    static boolean retreating = false;
    static final double HEALTH_PERCENTAGE_THRESHOLD_FOR_DISINTEGRATING = 0.3;

    static boolean lastAttackTargetIsOneShotKill = false;
    static boolean lastAttackTargetIsArchon = false;

    /** Select an enemy to attack, returns null if no enemy is found */
    static MapLocation getAttackTarget(int radius) {
        RobotInfo[] actionableEnemies = RobotPlayer.rc.senseNearbyRobots(radius, RobotPlayer.rc.getTeam().opponent());
        RobotInfo targetEnemy = null;
        double targetEnemyScore = -1;
        boolean targetIsOneShotKill = false;
        boolean targetIsArchon = false;
        for (int i = 0; i < actionableEnemies.length; i++) {
            RobotInfo thisEnemy = actionableEnemies[i];
            // Increase score by 0-10 based on percent of missing health
            double thisEnemyScore = 10 * (1 - thisEnemy.getHealth() / thisEnemy.getType().getMaxHealth(thisEnemy.level));
            // Increase score by 100 if I can one shot kill
            boolean thisTargetIsOneShotKill = false;
            if (GeneralManager.myType.getDamage(RobotPlayer.rc.getLevel()) >= thisEnemy.getHealth()) {
                thisEnemyScore += 100;
                thisTargetIsOneShotKill = true;
            }
            // Increase score by 0-50 based on target type
            switch (thisEnemy.getType()) {
                case SAGE: thisEnemyScore += 50; break;
                case SOLDIER: thisEnemyScore += 35; break;
                case WATCHTOWER: thisEnemyScore += 30; break;
                case MINER: thisEnemyScore += 25; break;
                case ARCHON: thisEnemyScore += 20; break;
                case LABORATORY: thisEnemyScore += 10; break;
                case BUILDER: thisEnemyScore += 0; break;
            }
            if ((targetEnemy == null || thisEnemyScore > targetEnemyScore) && RobotPlayer.rc.canAttack(thisEnemy.location)) {
                targetEnemy = thisEnemy;
                targetEnemyScore = thisEnemyScore;
                targetIsOneShotKill = thisTargetIsOneShotKill;
                targetIsArchon = thisEnemy.getType() == RobotType.ARCHON;
            }
        }
        if (targetEnemy == null) {
            return null;
        }
        lastAttackTargetIsOneShotKill = targetIsOneShotKill;
        lastAttackTargetIsArchon = targetIsArchon;
        return targetEnemy.location;
    }

    /** Try to perform an attack, returns boolean if successful */
    static boolean tryAttack() throws GameActionException {
        MapLocation attackLocation = getAttackTarget(GeneralManager.myType.actionRadiusSquared);
        if (attackLocation != null && RobotPlayer.rc.canAttack(attackLocation)) {
            RobotPlayer.rc.attack(attackLocation);
            if (lastAttackTargetIsOneShotKill && lastAttackTargetIsArchon) {
                ArchonTrackerManager.setEnemyArchonAlive(attackLocation, false);
            }
            return true;
        }
        return false;
    }

    /** Calculates combat score for a single robot */
    static double calculateRobotCombatScore(RobotInfo robot, boolean defensive) throws GameActionException {
        double score = 0;
        // Increase score by 0-100 based on target type
        if (defensive) {
            switch (robot.getType()) {
                case SAGE: score += 100; break;
                case SOLDIER: score += 10; break;
                case WATCHTOWER: score += 30; break;
                case ARCHON: score += 2; break;
                case BUILDER: score += 1; break;
                case LABORATORY: score += 0; break;
                case MINER: score -= 1; break;
            }
        }
        else {
            switch (robot.getType()) {
                case SAGE: score += 100; break;
                case SOLDIER: score += 10; break;
            }
        }
        // Decrease score based on percent of missing health
        score *= robot.getHealth() / robot.getType().getMaxHealth(robot.level);
        // Decrease score based on rubble
        score *= 1 / (1 + RobotPlayer.rc.senseRubble(robot.getLocation()) / 10);
        return score;
    }

    /** Calculates combat score for all locally visible robots of team */
    static double evaluateLocalCombatScore(Team team, boolean defensive) throws GameActionException {
        RobotInfo[] visibleRobots = RobotPlayer.rc.senseNearbyRobots(GeneralManager.myType.visionRadiusSquared,
                team);
        double combatScore = 0;
        for (int i = 0; i < visibleRobots.length; i++) {
            combatScore += calculateRobotCombatScore(visibleRobots[i], defensive);
        }
        // Include self in calculation if on same team
        if (team == RobotPlayer.rc.getTeam()) {
            combatScore += calculateRobotCombatScore(new RobotInfo(RobotPlayer.rc.getID(), RobotPlayer.rc.getTeam(), GeneralManager.myType, RobotPlayer.rc.getMode(), RobotPlayer.rc.getLevel(), RobotPlayer.rc.getHealth(), RobotPlayer.rc.getLocation()), defensive);
        }
        return combatScore;
    }

    /** Decides what a combat droid should do */
    static COMBAT_DROID_ACTIONS getCombatDroidAction() throws GameActionException {
        int savedEnemyCombatScore = RobotPlayer.rc.readSharedArray(CommunicationManager.SAVED_ENEMY_COMBAT_SCORE);
        double allyCombatScore = evaluateLocalCombatScore(RobotPlayer.rc.getTeam(), false);
        double enemyCombatScore = evaluateLocalCombatScore(RobotPlayer.rc.getTeam().opponent(), true);
        int distToNearestAllyArchon = RobotPlayer.rc.getLocation().distanceSquaredTo(ArchonTrackerManager.getNearestAllyArchonLocation(RobotPlayer.rc.getLocation()));

        COMBAT_DROID_ACTIONS chosenAction = COMBAT_DROID_ACTIONS.ATTACK;

        if(savedEnemyCombatScore <= allyCombatScore * (0.7 + 1 / (RobotPlayer.rc.getRoundNum() / 10 + 2))) {
            RobotPlayer.rc.writeSharedArray(CommunicationManager.SAVED_ENEMY_COMBAT_SCORE, 0);
            retreating = false;
            chosenAction = CombatManager.COMBAT_DROID_ACTIONS.ATTACK;
        }

//        DebugManager.log(allyCombatScore + "       " + (allyCombatScore < 10 + 10 * RobotPlayer.rc.getRoundNum() / 200));
        if ((enemyCombatScore > allyCombatScore * 0.9 || savedEnemyCombatScore > allyCombatScore * 0.9) &&
                !retreating) {
            chosenAction = CombatManager.COMBAT_DROID_ACTIONS.RETREAT;
            if (enemyCombatScore < allyCombatScore || distToNearestAllyArchon <= 16) {
                chosenAction = CombatManager.COMBAT_DROID_ACTIONS.HOLD;
            }
            else {
                retreating = true;
                if(enemyCombatScore > savedEnemyCombatScore)
                    RobotPlayer.rc.writeSharedArray(CommunicationManager.SAVED_ENEMY_COMBAT_SCORE, (int)enemyCombatScore);
            }
        }

        if(retreating) {
            if (distToNearestAllyArchon > 25)
                chosenAction = CombatManager.COMBAT_DROID_ACTIONS.RETREAT;
            else {
                if(RobotPlayer.rc.getHealth() < HEALTH_PERCENTAGE_THRESHOLD_FOR_DISINTEGRATING
                    * GeneralManager.myType.getMaxHealth(RobotPlayer.rc.getLevel())) {
                    if(distToNearestAllyArchon <= 4) {
                        DebugManager.log("I'm disintegrating");
                        RobotPlayer.rc.disintegrate();
                    }
                    else
                        chosenAction = CombatManager.COMBAT_DROID_ACTIONS.RETREAT;
                }
                else retreating = false;
            }
        }


//        else if(allyCombatScore < 30 + 10 * RobotPlayer.rc.getRoundNum() / 100 &&
//            distToNearestAllyArchon <= 25 &&
//            distToNearestAllyArchon >= allyCombatScore / 10) {
//                chosenAction = CombatManager.COMBAT_DROID_ACTIONS.HOLD;
//        }

        return chosenAction;
    }
}
