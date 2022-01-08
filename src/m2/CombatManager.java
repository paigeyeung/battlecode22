package m2;

import battlecode.common.*;

strictfp class CombatManager {
    enum COMBAT_DROID_ACTIONS {
        RETREAT,
        HOLD,
        ATTACK
    }

    /** Select an enemy to attack, returns null if no enemy is found */
    static MapLocation getAttackTarget(RobotController rc, int radius) {
        RobotInfo[] actionableEnemies = rc.senseNearbyRobots(radius, rc.getTeam().opponent());
        RobotInfo targetEnemy = null;
        double targetEnemyScore = -1;
        for (int i = 0; i < actionableEnemies.length; i++) {
            RobotInfo thisEnemy = actionableEnemies[i];
            // Increase score by 0-10 based on percent of missing health
            double thisEnemyScore = 10 * (1 - thisEnemy.getHealth() / thisEnemy.getType().getMaxHealth(thisEnemy.level));
            // Increase score by 100 if I can one shot kill
            if (rc.getType().getDamage(rc.getLevel()) >= thisEnemy.getHealth()) {
                thisEnemyScore += 100;
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
            if ((targetEnemy == null || thisEnemyScore > targetEnemyScore) && rc.canAttack(thisEnemy.location)) {
                targetEnemy = thisEnemy;
                targetEnemyScore = thisEnemyScore;
            }
        }
        if (targetEnemy == null) {
            return null;
        }
        return targetEnemy.location;
    }

    /** Try to perform an attack, returns boolean if successful */
    static boolean tryAttack(RobotController rc) throws GameActionException {
        MapLocation attackLocation = getAttackTarget(rc, rc.getType().actionRadiusSquared);
        if (attackLocation != null) {
            rc.attack(attackLocation);
            return true;
        }
        return false;
    }

    /** Calculates combat score for a single robot */
    static double calculateRobotCombatScore(RobotController rc, RobotInfo robot, boolean defensive) throws GameActionException {
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
        score *= 1 / (1 + rc.senseRubble(robot.getLocation()) / 10);
        return score;
    }

    /** Calculates combat score for all locally visible robots of team */
    static double evaluateLocalCombatScore(RobotController rc, Team team, boolean defensive) throws GameActionException {
        RobotInfo[] visibleRobots = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, team);
        double combatScore = 0;
        for (int i = 0; i < visibleRobots.length; i++) {
            combatScore += calculateRobotCombatScore(rc, visibleRobots[i], defensive);
        }
        // Include self in calculation if on same team
        if (team == rc.getTeam()) {
            combatScore += calculateRobotCombatScore(rc, new RobotInfo(rc.getID(), rc.getTeam(), rc.getType(), rc.getMode(), rc.getLevel(), rc.getHealth(), rc.getLocation()), defensive);
        }
        return combatScore;
    }

    /** Decides what a combat droid should do */
    static COMBAT_DROID_ACTIONS getCombatDroidAction(RobotController rc) throws GameActionException {
        double allyCombatScore = evaluateLocalCombatScore(rc, rc.getTeam(), false);
        double enemyCombatScore = evaluateLocalCombatScore(rc, rc.getTeam().opponent(), true);
        COMBAT_DROID_ACTIONS chosenAction = COMBAT_DROID_ACTIONS.ATTACK;
        if (enemyCombatScore > allyCombatScore * 0.5) {
            chosenAction = COMBAT_DROID_ACTIONS.RETREAT;
        }
        return chosenAction;
    }
}
