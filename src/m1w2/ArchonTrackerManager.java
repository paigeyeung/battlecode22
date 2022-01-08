package m1w2;

import battlecode.common.*;

import java.util.ArrayDeque;

strictfp class ArchonTrackerManager {
    static class ArchonTracker {
        boolean alive;

        ArchonTracker(boolean _alive) {
            alive = _alive;
        }
    }

    static class AllyArchonTracker extends ArchonTracker {
        MapLocation location;
        boolean toggle;

        AllyArchonTracker(boolean _alive, MapLocation _location, boolean _toggle) {
            super(_alive);
            location = _location;
            toggle = _toggle;
        }

        boolean isEqualTo(AllyArchonTracker other) {
            return alive == other.alive && location.equals(other.location) && toggle == other.toggle;
        }

        void update(AllyArchonTracker other) {
            alive = other.alive;
            location = other.location;
            toggle = other.toggle;
        }
    }

    static class EnemyArchonTracker extends ArchonTracker {
        MapLocation correspondingAllyArchonStartingLocation;
        boolean seen;
        MapLocation guessLocation;
        ArrayDeque<MapLocation> guessLocations;

        EnemyArchonTracker(boolean _alive, MapLocation _correspondingAllyArchonStartingLocation, boolean _seen, AllyArchonTracker[] _allyArchonTrackers) {
            super(_alive);
            correspondingAllyArchonStartingLocation = _correspondingAllyArchonStartingLocation;
            seen = _seen;

            guessLocations = new ArrayDeque<>();
            for (int i = 0; i <= 1; i++) {
                for (int j = 0; j <= 1; j++) {
                    MapLocation location = GeneralManager.getOppositeLocation(correspondingAllyArchonStartingLocation, i == 0, j == 0);
                    boolean isValidGuess = true;
                    if (guessLocations.contains(location)) {
                        isValidGuess = false;
                    }
                    else {
                        for (int k = 0; k < _allyArchonTrackers.length; k++) {
                            if (location.distanceSquaredTo(_allyArchonTrackers[k].location) <= 2) {
                                isValidGuess = false;
                                break;
                            }
                        }
                    }
                    if (isValidGuess) {
                        guessLocations.add(location);
                    }
                }
            }
            goToNextGuessLocation();
        }

        void goToNextGuessLocation() {
            guessLocation = guessLocations.removeFirst();
            if (guessLocation == null) {
                guessLocation = new MapLocation(0, 0);
                DebugManager.log(null, "ArchonTrackerManager: Ran out of guess locations!");
            }
        }

        boolean isEqualTo(EnemyArchonTracker other) {
            return alive == other.alive && guessLocation.equals(other.guessLocation) && seen == other.seen;
        }

        void update(EnemyArchonTracker other) {
            alive = other.alive;
            guessLocation = other.guessLocation;
            seen = other.seen;
        }
    }

    static boolean receivedArchonTrackers = false;
    static AllyArchonTracker[] allyArchonTrackers;
    static EnemyArchonTracker[] enemyArchonTrackers;
    static int myStartingArchonIndex = -1; // This should only be for droids

    static int encodeAllyArchonTracker(AllyArchonTracker allyArchonTracker) {
        return allyArchonTracker.location.x << 8 | allyArchonTracker.location.y << 2 | (allyArchonTracker.alive ? 1 : 0) << 1 | (allyArchonTracker.toggle ? 1 : 0);
    }
    static int encodeEnemyArchonTracker(EnemyArchonTracker enemyArchonTracker) {
        return enemyArchonTracker.correspondingAllyArchonStartingLocation.x << 8 | enemyArchonTracker.correspondingAllyArchonStartingLocation.y << 2 | (enemyArchonTracker.alive ? 1 : 0) << 1 | (enemyArchonTracker.seen ? 1 : 0);
    }
    static int encodeEnemyArchonTracker(boolean alive, boolean seen, MapLocation location) {
        return location.x << 8 | location.y << 2 | (alive ? 1 : 0) << 1 | (seen ? 1 : 0);
    }
    static AllyArchonTracker decodeAllyArchonTracker(int encoded) {
        return new AllyArchonTracker(((encoded >>> 1) & 0x1) == 1, new MapLocation((encoded >>> 8) & 0x3F, (encoded >>> 2) & 0x3F), (encoded & 0x1) == 1);
    }
    static EnemyArchonTracker decodeEnemyArchonTracker(int encoded) {
        return new EnemyArchonTracker(((encoded >>> 1) & 0x1) == 1, new MapLocation((encoded >>> 8) & 0x3F, (encoded >>> 2) & 0x3F), (encoded & 0x1) == 1, allyArchonTrackers);
    }

    static void setAllyArchonDead(RobotController rc, int index) throws GameActionException {
        allyArchonTrackers[index].alive = false;
        int encodedAllyArchonTracker = encodeAllyArchonTracker(allyArchonTrackers[index]);
        rc.writeSharedArray(index, encodedAllyArchonTracker);
        DebugManager.log(rc, "Broadcasted ally Archon dead location " + allyArchonTrackers[index].location + " as " + encodedAllyArchonTracker);

        if (rc.getType() == RobotType.ARCHON) {
            ArchonResourceManager.setArchonDead(rc, index);
        }
    }

    static AllyArchonTracker getNearestAllyArchon(MapLocation fromLocation) {
        AllyArchonTracker nearest = null;
        for (int i = 0; i < allyArchonTrackers.length; i++) {
            if (allyArchonTrackers[i].alive && (nearest == null || allyArchonTrackers[i].location.distanceSquaredTo(fromLocation) < nearest.location.distanceSquaredTo(fromLocation))) {
                nearest = allyArchonTrackers[i];
            }
        }
        return nearest;
    }
    static EnemyArchonTracker getNearestEnemyArchon(MapLocation fromLocation) {
        EnemyArchonTracker nearest = null;
        for (int i = 0; i < enemyArchonTrackers.length; i++) {
            if (enemyArchonTrackers[i].alive && (nearest == null || enemyArchonTrackers[i].guessLocation.distanceSquaredTo(fromLocation) < nearest.guessLocation.distanceSquaredTo(fromLocation))) {
                nearest = enemyArchonTrackers[i];
            }
        }
        return nearest;
    }

    static int getAllyArchonIndex(AllyArchonTracker allyArchonTracker) {
        for (int i = 0; i < allyArchonTrackers.length; i++) {
            if (allyArchonTracker.equals(allyArchonTrackers[i])) {
                return i;
            }
        }
        return -1;
    }
    static int getEnemyArchonIndex(EnemyArchonTracker enemyArchonTracker) {
        for (int i = 0; i < enemyArchonTrackers.length; i++) {
            if (enemyArchonTracker.equals(enemyArchonTrackers[i])) {
                return i;
            }
        }
        return -1;
    }

    static int getFirstAliveAllyArchon() {
        for (int i = 0; i < allyArchonTrackers.length; i++) {
            if (allyArchonTrackers[i].alive) {
                return i;
            }
        }
        return -1;
    }
    static int getLastAliveAllyArchon() {
        int last = -1;
        for (int i = 0; i < allyArchonTrackers.length; i++) {
            if (allyArchonTrackers[i].alive) {
                last = i;
            }
        }
        return last;
    }
}
