# Battlecode 2026 API Reference

## All Classes and Interfaces

### Classes

| Class | Description |
|-------|-------------|
| `Clock` | Clock is a singleton that allows contestants to introspect the state of their running code. |
| `Direction` | This enumeration represents a direction from one MapLocation to another. |
| `GameActionException` | An exception caused by a robot's interaction with the game world. |
| `GameActionExceptionType` | Enumerates the possible errors in GameWorld interactions that cause a GameActionException to be thrown. |
| `GameConstants` | GameConstants defines constants that affect gameplay. |
| `MapInfo` | |
| `MapLocation` | This class is an immutable representation of two-dimensional coordinates in the battlecode world. |
| `Message` | |
| `RobotController` | A RobotController allows contestants to make their robot sense and interact with the game world. |
| `RobotInfo` | RobotInfo stores basic information that was 'sensed' of another Robot. |
| `Team` | This enum represents the team of a robot. |
| `TrapType` | Enumerates possible traps that can be built. |
| `UnitType` | |

### Interfaces

| Interface | Description |
|-----------|-------------|
| `RobotController` | A RobotController allows contestants to make their robot sense and interact with the game world. |

### Enum Classes

| Enum | Description |
|------|-------------|
| `Direction` | This enumeration represents a direction from one MapLocation to another. |
| `GameActionExceptionType` | Enumerates the possible errors in GameWorld interactions that cause a GameActionException to be thrown. |
| `Team` | This enum represents the team of a robot. |
| `TrapType` | Enumerates possible traps that can be built. |
| `UnitType` | |

### Exception Classes

| Exception | Description |
|-----------|-------------|
| `GameActionException` | An exception caused by a robot's interaction with the game world. |

## Class Hierarchy

### Package: `battlecode.common`

```
java.lang.Object
├── battlecode.common.Clock
├── battlecode.common.GameConstants
├── battlecode.common.MapInfo
├── battlecode.common.MapLocation (implements java.lang.Comparable<T>, java.io.Serializable)
├── battlecode.common.Message
└── battlecode.common.RobotInfo

java.lang.Throwable (implements java.io.Serializable)
└── java.lang.Exception
    └── battlecode.common.GameActionException
```

## Interface Hierarchy

```
battlecode.common.RobotController
```

## Enum Class Hierarchy

```
java.lang.Object
└── java.lang.Enum<E> (implements java.lang.Comparable<T>, java.lang.constant.Constable, java.io.Serializable)
    ├── battlecode.common.Direction
    ├── battlecode.common.GameActionExceptionType
    ├── battlecode.common.Team
    ├── battlecode.common.TrapType
    └── battlecode.common.UnitType
```

## Key Classes for Bot Development

### `RobotController`
- Main interface for interacting with the game world
- Methods for sensing, moving, attacking, building, etc.
- Access via `rc` parameter in `RobotPlayer.run(RobotController rc)`

### `MapLocation`
- Immutable 2D coordinates (x, y)
- Methods: `distanceSquaredTo()`, `directionTo()`, `add()`, etc.
- Used for navigation and positioning

### `Direction`
- Enum: NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST, CENTER
- Used for movement and facing direction

### `Clock`
- Singleton for introspection
- `Clock.getRoundNum()` - current round number
- `Clock.getBytecodeNum()` - bytecodes used this turn
- `Clock.yield()` - end turn

### `RobotInfo`
- Information about sensed robots
- Contains: location, health, team, type, etc.

### `GameConstants`
- Static constants for gameplay
- Movement cooldowns, vision ranges, etc.

### `UnitType`
- Enum: BABY_RAT, RAT_KING

### `TrapType`
- Enum: RAT_TRAP, CAT_TRAP

### `Team`
- Enum: A, B

### `GameActionException`
- Thrown when actions fail (can't move, insufficient resources, etc.)
- Must be handled defensively

## Common Usage Patterns

### Getting RobotController
```java
public class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        // rc is your main interface to the game
    }
}
```

### Getting Current Location
```java
MapLocation myLoc = rc.getLocation();
```

### Sensing Nearby Robots
```java
RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
```

### Moving
```java
if (rc.canMove(Direction.NORTH)) {
    rc.move(Direction.NORTH);
}
```

### Checking Cooldowns
```java
int moveCooldown = rc.getMovementCooldownTurns();
int turnCooldown = rc.getTurningCooldownTurns();
int actionCooldown = rc.getActionCooldownTurns();
```

### Ending Turn
```java
Clock.yield(); // End turn and wait for next round
```
