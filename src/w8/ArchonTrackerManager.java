package w8;

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

        AllyArchonTracker(boolean _alive, MapLocation _location) {
            super(_alive);
            location = _location;
        }
    }

    static class EnemyArchonTracker extends ArchonTracker {
        boolean seen;
        MapLocation correspondingAllyArchonStartingLocation;
        MapLocation guessLocation;
        ArrayDeque<MapLocation> guessLocations;

        EnemyArchonTracker(boolean _alive, boolean _seen, MapLocation _correspondingAllyArchonStartingLocation, AllyArchonTracker[] _allyArchonTrackers) {
            super(_alive);
            seen = _seen;
            correspondingAllyArchonStartingLocation = _correspondingAllyArchonStartingLocation;

            guessLocations = new ArrayDeque<>();
            for (int i = 0; i <= 1; i++) {
                for (int j = 0; j <= 1; j++) {
                    MapLocation location = GeneralManager.getOppositeLocation(correspondingAllyArchonStartingLocation, i == 0, j == 0);
                    boolean isValidGuess = true;
                    for (int k = 0; k < _allyArchonTrackers.length; k++) {
                        if (location.distanceSquaredTo(_allyArchonTrackers[k].location) <= 2) {
                            isValidGuess = false;
                            break;
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
        }
    }

    static boolean receivedArchonTrackers = false;
    static AllyArchonTracker[] allyArchonTrackers;
    static EnemyArchonTracker[] enemyArchonTrackers;
    static int myStartingArchonIndex = -1; // This should only be for droids

    static int encodeAllyArchonTracker(AllyArchonTracker allyArchonTracker) {
        return allyArchonTracker.location.x << 8 | allyArchonTracker.location.y << 2 | (allyArchonTracker.alive ? 1 : 0) << 1;
    }
    static int encodeEnemyArchonTracker(EnemyArchonTracker enemyArchonTracker) {
        return enemyArchonTracker.correspondingAllyArchonStartingLocation.x << 8 | enemyArchonTracker.correspondingAllyArchonStartingLocation.y << 2 | (enemyArchonTracker.alive ? 1 : 0) << 1 | (enemyArchonTracker.seen ? 1 : 0);
    }
    static int encodeEnemyArchonTracker(boolean alive, boolean seen, MapLocation location) {
        return location.x << 8 | location.y << 2 | (alive ? 1 : 0) << 1 | (seen ? 1 : 0);
    }
    static AllyArchonTracker decodeAllyArchonTracker(int encoded) {
        return new AllyArchonTracker(((encoded >> 1) & 0x1) == 1, new MapLocation((encoded >> 8) & 0x3F, (encoded >> 2) & 0x3F));
    }
    static EnemyArchonTracker decodeEnemyArchonTracker(int encoded) {
        return new EnemyArchonTracker(((encoded >> 1) & 0x1) == 1, (encoded & 0x1) == 1, new MapLocation((encoded >> 8) & 0x3F, (encoded >> 2) & 0x3F), allyArchonTrackers);
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
}
