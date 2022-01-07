package p4;

import battlecode.common.*;

import java.util.ArrayDeque;

strictfp class ArchonTrackerManager {
    static class ArchonTracker {
        boolean alive;

        ArchonTracker(boolean _alive) {
            alive = _alive;
        }
    }

    static class MyArchonTracker extends ArchonTracker {
        MapLocation location;

        MyArchonTracker(boolean _alive, MapLocation _location) {
            super(_alive);
            location = _location;
        }
    }

    static class EnemyArchonTracker extends ArchonTracker {
        boolean seen;
        MapLocation myArchonStartingLocation;
        MapLocation guessLocation;
        ArrayDeque<MapLocation> guessLocations;

        EnemyArchonTracker(boolean _alive, boolean _seen, MapLocation _myArchonStartingLocation, MyArchonTracker[] _myArchonTrackers) {
            super(_alive);
            seen = _seen;
            myArchonStartingLocation = _myArchonStartingLocation;

            guessLocations = new ArrayDeque<>();
            for (int i = 0; i <= 1; i++) {
                for (int j = 0; j <= 1; j++) {
                    MapLocation location = GeneralManager.getOppositeLocation(myArchonStartingLocation, i == 0, j == 0);
                    boolean isValidGuess = true;
                    for (int k = 0; k < _myArchonTrackers.length; k++) {
                        if (location.distanceSquaredTo(_myArchonTrackers[k].location) <= 2) {
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

        MapLocation getLocation() {
            return guessLocation;
        }
    }

    static boolean receivedArchonTrackers = false;
    static MyArchonTracker[] myArchonTrackers;
    static EnemyArchonTracker[] enemyArchonTrackers;
    static int myStartingArchonIndex = -1;

    static int encodeMyArchonTracker(MyArchonTracker myArchonTracker) {
        return myArchonTracker.location.x << 8 | myArchonTracker.location.y << 2 | (myArchonTracker.alive ? 1 : 0) << 1;
    }
    static int encodeEnemyArchonTracker(EnemyArchonTracker enemyArchonTracker) {
        return enemyArchonTracker.myArchonStartingLocation.x << 8 | enemyArchonTracker.myArchonStartingLocation.y << 2 | (enemyArchonTracker.alive ? 1 : 0) << 1 | (enemyArchonTracker.seen ? 1 : 0);
    }
    static int encodeEnemyArchonTracker(boolean alive, boolean seen, MapLocation location) {
        return location.x << 8 | location.y << 2 | (alive ? 1 : 0) << 1 | (seen ? 1 : 0);
    }
    static MyArchonTracker decodeMyArchonTracker(int encoded) {
        return new MyArchonTracker(((encoded >> 1) & 0x1) == 1, new MapLocation((encoded >> 8) & 0x3F, (encoded >> 2) & 0x3F));
    }
    static EnemyArchonTracker decodeEnemyArchonTracker(int encoded) {
        return new EnemyArchonTracker(((encoded >> 1) & 0x1) == 1, (encoded & 0x1) == 1, new MapLocation((encoded >> 8) & 0x3F, (encoded >> 2) & 0x3F), myArchonTrackers);
    }

    static MyArchonTracker getNearestMyArchon(MapLocation fromLocation) {
        MyArchonTracker nearest = null;
        for (int i = 0; i < myArchonTrackers.length; i++) {
            if (myArchonTrackers[i].alive && (nearest == null || myArchonTrackers[i].location.distanceSquaredTo(fromLocation) < nearest.location.distanceSquaredTo(fromLocation))) {
                nearest = myArchonTrackers[i];
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
