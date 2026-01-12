# Messaging System Usage Examples

## Overview
The messaging system is now integrated into your bot. Here's how to use it:

## Sending Messages (Baby Rats)

### Report Enemy King Location
```java
import v02_myVersion.Utils.Communication;
import static v02_myVersion.Utils.Communication.*;

// In your baby rat state:
RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().getVisionRadiusSquared(), rc.getTeam().opponent());
for (RobotInfo enemy : enemies) {
    if (enemy.getType() == UnitType.RAT_KING) {
        Communication.sendSqueak(rc, TYPE_ENEMY_KING, enemy.getLocation());
        break; // Only squeak once per turn
    }
}
```

### Report Cheese Mine Location
```java
// When you discover a cheese mine:
MapInfo[] nearbyMap = rc.senseNearbyMapInfos();
for (MapInfo info : nearbyMap) {
    if (info.hasCheeseMine()) {
        Communication.sendSqueak(rc, TYPE_MINE, info.getMapLocation());
        break;
    }
}
```

### Report Cat Location
```java
RobotInfo[] nearby = rc.senseNearbyRobots();
for (RobotInfo robot : nearby) {
    if (robot.getType() == UnitType.CAT) {
        Communication.sendSqueak(rc, TYPE_CAT, robot.getLocation());
        break;
    }
}
```

## Reading from Shared Array (All Robots)

### Read Enemy King Location
```java
MapLocation enemyKing = Communication.readEnemyKingLocation(rc);
if (enemyKing != null) {
    // Enemy king location is known and recent (within last 100 rounds)
    int distance = rc.getLocation().distanceSquaredTo(enemyKing);
    // Use this information for strategy
}
```

### Read Cat Location
```java
MapLocation catLoc = Communication.readCatLocation(rc);
if (catLoc != null) {
    // Cat location is known and recent (within last 50 rounds)
    // Avoid this area or prepare for attack
}
```

### Read Cheese Mine Locations
```java
MapLocation[] mines = Communication.readMineLocations(rc);
for (MapLocation mine : mines) {
    // Navigate to known cheese mines
    if (rc.canSenseLocation(mine)) {
        // Mine is nearby, collect cheese
    }
}
```

## Integration Points

### Automatic Processing
- **Rat Kings**: Automatically call `Communication.ingestSqueaks(rc)` at the start of each turn
- **All Robots**: Shared array state is automatically updated each turn
- **Error Handling**: Single try/catch at root level in `Robot.run()`

### Manual Usage
You can also manually call communication methods in your states:

```java
// In any state, you can read from shared array:
MapLocation enemyKing = Communication.readEnemyKingLocation(rc);

// Baby rats can send squeaks:
if (rc.getType() == UnitType.BABY_RAT) {
    Communication.sendSqueak(rc, TYPE_ENEMY_KING, someLocation);
}
```

## Message Types Available

- `TYPE_EMPTY` - Empty message
- `TYPE_ENEMY_KING` - Enemy rat king location
- `TYPE_MINE` - Cheese mine location
- `TYPE_CAT` - Cat location
- `TYPE_ENEMY_RAT` - Enemy rat location (for future use)
- `TYPE_CHEESE` - Cheese location (for future use)

## Shared Array Layout

- **Indices 0-2**: Enemy King (X, Y, Round)
- **Indices 3-62**: Cheese Mines (pairs of X, Y for up to 30 mines)
- **Indices 61-63**: Cat (Round, Y, X)

## Important Notes

1. **Only Rat Kings can write to shared array** - This is enforced automatically
2. **Only Baby Rats can squeak** - This is enforced automatically
3. **One squeak per turn per robot** - Tracked automatically
4. **Change detection** - Only writes to shared array if value changed (bytecode optimization)
5. **No try/catch in states** - Single catch at root level to avoid 500 bytecode penalty

## Example: Complete State Using Communication

```java
public class CollectCheese extends State {
    @Override
    public Result run() throws GameActionException {
        // Read known cheese mine locations
        MapLocation[] mines = Communication.readMineLocations(rc);
        
        // Find nearest mine
        MapLocation nearestMine = null;
        int minDist = Integer.MAX_VALUE;
        for (MapLocation mine : mines) {
            int dist = rc.getLocation().distanceSquaredTo(mine);
            if (dist < minDist) {
                minDist = dist;
                nearestMine = mine;
            }
        }
        
        // If no known mines, explore and report any found
        if (nearestMine == null) {
            MapInfo[] nearby = rc.senseNearbyMapInfos();
            for (MapInfo info : nearby) {
                if (info.hasCheeseMine()) {
                    Communication.sendSqueak(rc, TYPE_MINE, info.getMapLocation());
                }
            }
        }
        
        // Check for enemy king and report
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().getVisionRadiusSquared(), rc.getTeam().opponent());
        for (RobotInfo enemy : enemies) {
            if (enemy.getType() == UnitType.RAT_KING) {
                Communication.sendSqueak(rc, TYPE_ENEMY_KING, enemy.getLocation());
                break;
            }
        }
        
        // Avoid cat if known
        MapLocation catLoc = Communication.readCatLocation(rc);
        if (catLoc != null) {
            int catDist = rc.getLocation().distanceSquaredTo(catLoc);
            if (catDist < 20) {
                // Move away from cat
                return new Result(OK, "Avoiding cat");
            }
        }
        
        return new Result(OK, "");
    }
}
```
