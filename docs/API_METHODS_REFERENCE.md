# Battlecode 2026 API Methods Reference

Complete index of all methods, constants, and enums in the Battlecode API.

## RobotController Interface Methods

### Movement & Positioning
- `adjacentLocation(Direction)` - Returns the location adjacent to current location in the given direction
- `canMove(Direction)` - Checks whether this robot can move one step in the target direction
- `canMoveForward()` - Checks whether this robot can move one step in the direction it is facing
- `move(Direction)` - Moves one step in the specified direction
- `moveForward()` - Moves one step in the direction the robot is facing
- `getLocation()` - Returns this robot's designated center location
- `getAllPartLocations()` - Returns all the locations that a robot occupies
- `onTheMap(MapLocation)` - Checks whether a MapLocation is on the map
- `isLocationOccupied(MapLocation)` - Checks whether a robot is at a given location

### Turning & Direction
- `canTurn()` - Checks whether this robot can turn
- `canTurn(Direction)` - Checks whether this robot can turn to the specified direction
- `turn(Direction)` - Turns to the specified direction
- `getDirection()` - Returns this robot's current direction
- `isMovementReady()` - Tests whether the robot can move
- `isTurningReady()` - Tests whether the robot can turn

### Cooldowns
- `getMovementCooldownTurns()` - Returns the number of movement cooldown turns remaining
- `getTurningCooldownTurns()` - Returns the number of turning cooldown turns remaining
- `getActionCooldownTurns()` - Returns the number of action cooldown turns remaining
- `isActionReady()` - Tests whether the robot can act

### Sensing
- `canSenseLocation(MapLocation)` - Checks whether the given location is within the robot's vision range
- `canSenseRobot(int)` - Tests whether the given robot exists and if it is within this robot's vision range
- `canSenseRobotAtLocation(MapLocation)` - Checks whether a robot is at a given location
- `senseNearbyRobots()` - Returns all robots within vision radius
- `senseNearbyRobots(int)` - Returns all robots that can be sensed within a certain distance
- `senseNearbyRobots(int, Team)` - Returns all robots of a given team within a certain distance
- `senseNearbyRobots(MapLocation, int, Team)` - Returns all robots of a given team within a certain radius of a specified location
- `senseRobot(int)` - Senses information about a particular robot given its ID
- `senseRobotAtLocation(MapLocation)` - Senses the robot at the given location, or null if there is no robot there
- `senseMapInfo(MapLocation)` - Senses the map info at a location
- `senseNearbyMapInfos()` - Return map info for all senseable locations
- `senseNearbyMapInfos(int)` - Return map info for all senseable locations within a radius squared
- `senseNearbyMapInfos(MapLocation)` - Return map info for all senseable locations within vision radius of a center location
- `senseNearbyMapInfos(MapLocation, int)` - Return map info for all senseable locations within a radius squared of a center location
- `sensePassability(MapLocation)` - Returns whether that location is passable
- `getAllLocationsWithinRadiusSquared(MapLocation, int)` - Returns a list of all locations within the given vision cone of a location

### Attack
- `canAttack(MapLocation)` - Tests whether this robot can attack (aka bite) the given location
- `canAttack(MapLocation, int)` - Tests whether this robot can attack (bite) the given location with the given amount of cheese
- `attack(MapLocation)` - Performs a rat attack (aka bite) action, defaulting to a bite with no cheese for rats
- `attack(MapLocation, int)` - Performs the specific attack for this robot type, consuming the specified amount of cheese for increasing bite strength

### Cheese Management
- `canPickUpCheese(MapLocation)` - Tests whether this robot can pick up cheese at the given location
- `pickUpCheese(MapLocation)` - picks up cheese from the given location
- `canTransferCheese(MapLocation, int)` - Tests whether you can transfer cheese to a given rat king
- `transferCheese(MapLocation, int)` - Transfers cheese to a given rat king
- `getAllCheese()` - Returns the amount of cheese the robot has access to
- `getGlobalCheese()` - Returns the amount of global cheese available
- `getRawCheese()` - Returns the amount of cheese the robot is currently holding
- `getCurrentRatCost()` - Returns the current cheese cost for an allied rat king to spawn a rat

### Ratnapping / Carrying
- `canCarryRat(MapLocation)` - Tests whether the robot can grab (carry) a robot at the specified location
- `carryRat(MapLocation)` - Causes this robot to pick up (grab) a robot at the specified location
- `canDropRat(Direction)` - Tests whether this robot can safely drop a carried robot in the specified direction
- `dropRat(Direction)` - Safely drops robot in the specified direction
- `canThrowRat()` - Tests whether the robot can throw a carried robot
- `throwRat()` - Throws robot in the robot's facing direction
- `getCarrying()` - Returns robot that this robot is carrying or null if this robot is not carrying another robot
- `isBeingCarried()` - Returns whether robot is being carried
- `isBeingThrown()` - Returns whether robot is being thrown

### Traps
- `canPlaceRatTrap(MapLocation)` - Tests whether this robot can place a rat trap at the given location
- `placeRatTrap(MapLocation)` - Places a rat trap at the given location
- `canPlaceCatTrap(MapLocation)` - Tests whether this robot can place a cat trap at the given location
- `placeCatTrap(MapLocation)` - Places a cat trap at the given location
- `canRemoveRatTrap(MapLocation)` - Tests whether this robot can remove a rat trap at the given location
- `removeRatTrap(MapLocation)` - Removes the rat trap at the given location
- `canRemoveCatTrap(MapLocation)` - Tests whether this robot can remove a cat trap at the given location
- `removeCatTrap(MapLocation)` - Removes the cat trap at the given location

### Dirt
- `canPlaceDirt(MapLocation)` - Tests whether this robot can place dirt at the given location
- `placeDirt(MapLocation)` - Places dirt at the given location
- `canRemoveDirt(MapLocation)` - Tests whether this robot can remove dirt from the given location
- `removeDirt(MapLocation)` - Removes dirt from the given location
- `getDirt()` - Returns the amount of dirt that this robot's team has

### Rat King Operations
- `canBecomeRatKing()` - Checks if a rat can become a rat king
- `becomeRatKing()` - Upgrades this rat into a rat king if possible
- `canBuildRat(MapLocation)` - Checks if a rat king can spawn a baby rat at the given location
- `buildRat(MapLocation)` - Spawns a baby rat at the given location

### Communication
- `squeak(int)` - Sends a message (contained in an int, so 4 bytes) to all locations within squeaking range
- `readSqueaks(int)` - Reads all squeaks sent to this unit within the past 5 rounds if roundNum = -1, or only squeaks sent from the specified round otherwise
- `readSharedArray(int)` - Reads a value from the shared array at the given index
- `writeSharedArray(int, int)` - Writes a value to the shared array at the given index (rat kings only)

### Game State
- `getRoundNum()` - Returns the current round number, where round 1 is the first round of the match
- `getTeam()` - Returns this robot's Team
- `getType()` - Returns what UnitType this robot is
- `getID()` - Returns the ID of this robot
- `getHealth()` - Returns this robot's current health
- `isCooperation()` - Returns the game state- true if in cooperation mode, false if in backstabbing mode
- `getMapWidth()` - Returns the width of the game map
- `getMapHeight()` - Returns the height of the game map

### Debugging
- `setIndicatorDot(MapLocation, int, int, int)` - Draw a dot on the game map for debugging purposes
- `setIndicatorLine(MapLocation, MapLocation, int, int, int)` - Draw a line on the game map for debugging purposes
- `setIndicatorString(String)` - Sets the indicator string for this robot for debugging purposes
- `setTimelineMarker(String, int, int, int)` - Adds a marker to the timeline at the current round for debugging purposes

### Other
- `disintegrate()` - Destroys the robot
- `resign()` - Causes your team to lose the game

## Clock Class Methods

- `getBytecodeNum()` - Returns the number of bytecodes the current robot has executed since the beginning of the current round
- `getBytecodesLeft()` - Returns the number of bytecodes this robot has left in this round
- `getRoundNum()` - Returns the current round number
- `getTimeElapsed()` - Returns the total amount of time that this team's robots have collectively spent executing since the beginning of the match
- `getTimeLeft()` - Returns the total amount of execution time left this team has before they timeout
- `yield()` - Ends the processing of this robot during the current round

## MapLocation Class Methods

### Construction & Basic Info
- `MapLocation(int, int)` - Creates a new MapLocation representing the location with the given coordinates
- `x` - The x-coordinate
- `y` - The y-coordinate

### Distance & Direction
- `distanceSquaredTo(MapLocation)` - Computes the squared distance from this location to the specified location
- `bottomLeftDistanceSquaredTo(MapLocation)` - Computes the squared distance from the multi-part robot at this location to the specified location
- `directionTo(MapLocation)` - Returns the closest approximate Direction from this MapLocation to location
- `isWithinDistanceSquared(MapLocation, int)` - Determines whether this location is within a specified distance from target location
- `isWithinDistanceSquared(MapLocation, int, Direction, double)` - Determines whether this location is within a specified distance and cone degree from target location
- `isWithinDistanceSquared(MapLocation, int, Direction, double, boolean)` - Determines whether this location is within a specified distance and cone degree from target location

### Movement
- `add(Direction)` - Returns a new MapLocation object representing a location one unit in distance from this one in the given direction
- `subtract(Direction)` - Returns a new MapLocation object representing a location one unit in distance from this one in the opposite direction of the given direction
- `translate(int, int)` - Returns a new MapLocation object translated from this location by a fixed amount
- `isAdjacentTo(MapLocation)` - Determines whether this location is adjacent to a given location

### Comparison
- `equals(Object)` - Two MapLocations are regarded as equal iff their coordinates are the same
- `compareTo(MapLocation)` - A comparison function for MapLocations
- `hashCode()` - Hash code
- `toString()` - String representation

## Direction Enum Methods

### Constants
- `NORTH` - Direction that represents pointing north (up on screen)
- `NORTHEAST` - Direction that represents pointing northeast
- `EAST` - Direction that represents pointing east (right on screen)
- `SOUTHEAST` - Direction that represents pointing southeast
- `SOUTH` - Direction that represents pointing south (down on screen)
- `SOUTHWEST` - Direction that represents pointing southwest
- `WEST` - Direction that represents pointing west (left on screen)
- `NORTHWEST` - Direction that represents pointing northwest
- `CENTER` - Direction that represents pointing nowhere

### Methods
- `allDirections()` - Returns a list of all directions
- `cardinalDirections()` - Returns a list of all cardinal directions
- `opposite()` - Computes the direction opposite this one
- `rotateLeft()` - Computes the direction 45 degrees to the left (counter-clockwise) of this one
- `rotateRight()` - Computes the direction 45 degrees to the right (clockwise) of this one
- `fromDelta(int, int)` - Converts dx and dy to a Direction
- `getDeltaX()` - Returns the delta X of the direction
- `getDeltaY()` - Returns the delta Y of the direction
- `getDirectionOrderNum()` - Returns the order number
- `dx` - Change in x
- `dy` - Change in y
- `DIRECTION_ORDER` - Static variable

## RobotInfo Class

### Fields
- `ID` - The unique ID of the robot
- `team` - The Team that the robot is on
- `type` - The type of the robot
- `health` - The health of the robot
- `location` - The current location of the robot
- `direction` - The current location of the robot
- `cheeseAmount` - The current cheese this robot holds
- `carryingRobot` - The current robot being carried by this robot, or null if not carrying any robots
- `chirality` - Robot chirality (used by cat only)

### Methods
- `getID()` - Returns the ID of this robot
- `getTeam()` - Returns the team that this robot is on
- `getType()` - Returns this robot's type
- `getHealth()` - Returns the health of this robot
- `getLocation()` - Returns the location of this robot
- `getDirection()` - Returns the direction of this robot
- `getRawCheeseAmount()` - Returns the cheese amount of this robot
- `getCarryingRobot()` - Returns the robot this robot is carrying, or null if not carrying a robot
- `getChirality()` - Returns the chirality of this robot
- `equals(Object)` - Equality check
- `hashCode()` - Hash code
- `toString()` - String representation

## MapInfo Class

### Methods
- `getMapLocation()` - Returns the location of this square
- `isPassable()` - Returns if this square is passable
- `isWall()` - Returns if this square is a wall
- `isDirt()` - Returns if this square is a dirt
- `getCheeseAmount()` - Returns the amount of cheese on this square
- `hasCheeseMine()` - Returns if this square has a cheese mine
- `getTrap()` - Returns the trap on this square, or TrapType.NONE if there is no trap
- `toString()` - String representation

## Message Class

### Methods
- `getBytes()` - Returns message content
- `getSenderID()` - Returns sender ID
- `getSource()` - Returns source location
- `getRound()` - Returns round number
- `copy()` - Creates a copy
- `toString()` - String representation

## UnitType Enum

### Constants
- `BABY_RAT` - Baby rat unit type
- `RAT_KING` - Rat king unit type
- `CAT` - Cat unit type

### Methods
- `getHealth()` - Returns health
- `getSize()` - Returns size
- `getMovementCooldown()` - Returns movement cooldown
- `getActionCooldown()` - Returns action cooldown
- `getBytecodeLimit()` - Returns bytecode limit
- `getVisionRadiusSquared()` - Returns vision radius squared
- `getVisionAngle()` - Returns vision angle
- `getAllTypeLocations(MapLocation)` - Returns all locations for this unit type
- `isBabyRatType()` - Checks if baby rat
- `isRatKingType()` - Checks if rat king
- `isCatType()` - Checks if cat
- `isRobotType()` - Checks if robot (rat)
- `isThrowableType()` - Checks if throwable
- `isThrowingType()` - Checks if can throw
- `usesBottomLeftLocationForDistance()` - Checks distance calculation method

### Fields
- `health` - Health value
- `size` - Size value
- `movementCooldown` - Movement cooldown
- `actionCooldown` - Action cooldown
- `bytecodeLimit` - Bytecode limit
- `visionConeRadiusSquared` - Vision radius squared
- `visionConeAngle` - Vision angle

## TrapType Enum

### Constants
- `NONE` - No trap
- `RAT_TRAP` - Traps enemy rats
- `CAT_TRAP` - Traps the cat

### Fields
- `buildCost` - Crumbs cost of each trap
- `damage` - The damage done if trap triggered
- `stunTime` - How many turn stun lasts after entering
- `triggerRadiusSquared` - The radius within which the trap is triggered
- `maxCount` - Maximum number of this trap type that a team can have active at once
- `trapLimit` - How many traps of this type can be on the map at the same time
- `actionCooldown` - Action cooldown
- `spawnCheeseAmount` - Amount of cheese that spawns with the rat trap

## Team Enum

### Constants
- `A` - Team A
- `B` - Team B
- `NEUTRAL` - Neutral robots

### Methods
- `isPlayer()` - Returns whether a robot of this team is a player-controlled entity (team A or team B)
- `opponent()` - Determines the team that is the opponent of this team

## GameActionExceptionType Enum

### Constants
- `CANT_DO_THAT` - Indicates when a robot tries to perform an action it can't
- `CANT_MOVE_THERE` - Indicates when a robot tries to move into a non-empty location
- `CANT_SENSE_THAT` - Indicates when a robot tries to sense a robot that no longer exists or is no longer in this robot's vision range
- `NO_ROBOT_THERE` - Indicates when a robot tries to perform an action on another robot, but there is no suitable robot there
- `NOT_ENOUGH_RESOURCE` - Indicates when a robot tries to perform an action for which it does not have enough resources
- `OUT_OF_RANGE` - Indicates when a robot tries to perform an action on a location that is outside its range
- `IS_NOT_READY` - Indicates when a robot tries to execute an action, but is not currently idle
- `ROUND_OUT_OF_RANGE` - Indicates when round number is out of range
- `INTERNAL_ERROR` - Internal error in the GameWorld engine

## GameConstants

### Map Constants
- `MAP_MIN_WIDTH` - The minimum possible map width
- `MAP_MAX_WIDTH` - The maximum possible map width
- `MAP_MIN_HEIGHT` - The minimum possible map height
- `MAP_MAX_HEIGHT` - The maximum possible map height
- `MAX_WALL_PERCENTAGE` - The maximum percentage of the map that can be walls
- `MAX_DIRT_PERCENTAGE` - The maximum percentage of the map that can be dirt

### Cheese Constants
- `INITIAL_TEAM_CHEESE` - The amount of cheese each team starts with
- `CHEESE_SPAWN_AMOUNT` - How much cheese each mine spawns at once
- `CHEESE_MINE_SPAWN_PROBABILITY` - Probability parameter for cheese spawn at a mine
- `CHEESE_PICK_UP_RADIUS_SQUARED` - The maximum distance for picking up cheese on the map
- `CHEESE_TRANSFER_RADIUS_SQUARED` - The maximum distance for transferring cheese to an allied rat king
- `CHEESE_TRANSFER_COOLDOWN` - The amount added to the action cooldown counter after dropping/transferring cheese
- `CHEESE_COOLDOWN_PENALTY` - The fractional slowdown a rat's movement and actions (not turning!) incur per unit of cheese the rat is currently carrying
- `SQ_CHEESE_SPAWN_RADIUS` - Cheese will spawn within a [-radius, radius] square of the cheese mine
- `MIN_CHEESE_MINE_SPACING_SQUARED` - The minimum distance between cheese mines on the map

### Cooldown Constants
- `COOLDOWN_LIMIT` - If the amount of cooldown is at least this value, a robot cannot act
- `COOLDOWNS_PER_TURN` - The number of cooldown turns reduced per turn
- `TURNING_COOLDOWN` - The amount added to the turning cooldown counter when turning
- `MOVE_STRAFE_COOLDOWN` - The default cooldown applied when moving in one of the 7 non-forward directions (forward is 10 ticks)

### Rat King Constants
- `NUMBER_INITIAL_RAT_KINGS` - The number of rat kings a player starts with
- `MAX_NUMBER_OF_RAT_KINGS` - The maximum number of rat kings that a team can have
- `RAT_KING_CHEESE_CONSUMPTION` - The amount of cheese the rat king consumes each round
- `RAT_KING_HEALTH_LOSS` - The amount of health the rat king loses by not eating cheese
- `RAT_KING_UPGRADE_CHEESE_COST` - The cheese cost for upgrading a rat into a rat king

### Building Constants
- `BUILD_ROBOT_BASE_COST` - The base cheese cost for spawning a rat
- `BUILD_ROBOT_COST_INCREASE` - The amount by which the cost to spawn a rat increases by for every NUM_ROBOTS_FOR_COST_INCREASE allied rats
- `NUM_ROBOTS_FOR_COST_INCREASE` - The number of allied rats needed to increase the base cost of a rat by BUILD_ROBOT_COST_INCREASE
- `BUILD_ROBOT_COOLDOWN` - The amount added to the action cooldown counter after a king builds a robot
- `BUILD_ROBOT_RADIUS_SQUARED` - The maximum distance from a rat king for building robots
- `BUILD_DISTANCE_SQUARED` - The maximum distance from a robot for building traps or dirt

### Attack Constants
- `RAT_BITE_DAMAGE` - The damage a robot takes after being bitten by a rat

### Cat Constants
- `CAT_SCRATCH_DAMAGE` - The damage a robot takes after being scratched by a cat
- `CAT_POUNCE_MAX_DISTANCE_SQUARED` - The distance squared a cat can pounce to
- `CAT_POUNCE_ADJACENT_DAMAGE_PERCENT` - Percent damage a rat takes when a cat pounces to an adjacent location
- `CAT_SLEEP_TIME` - Amount of rounds a cat sleeps for when fed
- `CAT_DIG_ADDITIONAL_COOLDOWN` - Additional cooldown for cat digging

### Trap Constants
- `DIG_DIRT_CHEESE_COST` - The cheese cost to dig up a tile of dirt
- `DIG_COOLDOWN` - The amount added to the action cooldown counter after digging out a tile of dirt
- `PLACE_DIRT_CHEESE_COST` - The cheese cost to place a tile of dirt

### Ratnapping Constants
- `CARRY_COOLDOWN_MULTIPLIER` - The multiplier to the cooldowns when carrying another robot
- `MAX_CARRY_DURATION` - The maximum number of turns of robots a rat can carry another rat
- `MAX_CARRY_TOWER_HEIGHT` - The maximum number of robots a rat can carry
- `HEALTH_GRAB_THRESHOLD` - The minimum gap between an enemy robot's health and our own before we can grab it from all angles

### Throwing Constants
- `THROW_DURATION` - The total number turns a rat can travel for while thrown (rats are stunned while thrown)
- `THROW_DAMAGE` - The base damage a thrown rat takes upon hitting the ground
- `THROW_DAMAGE_PER_TILE` - The damage a thrown rat takes per tile it impacts early
- `HIT_GROUND_COOLDOWN` - The stun cooldown after hitting the ground after being thrown
- `HIT_TARGET_COOLDOWN` - The stun cooldown after hitting the target after being thrown

### Communication Constants
- `SHARED_ARRAY_SIZE` - The size of the shared array
- `COMM_ARRAY_MAX_VALUE` - The maximum value of an integer in the shared array and persistent array
- `SQUEAK_RADIUS_SQUARED` - The maximum squared radius a robot can squeak to
- `MESSAGE_ROUND_DURATION` - The maximum number of rounds a message will exist for
- `MAX_MESSAGES_SENT_ROBOT` - The maximum number of messages a robot can send per turn

### Game Constants
- `GAME_MAX_NUMBER_OF_ROUNDS` - The maximum number of rounds in a game
- `GAME_DEFAULT_SEED` - The default game seed
- `SPEC_VERSION` - The current spec version the server compiles with

### Execution Constants
- `EXCEPTION_BYTECODE_PENALTY` - The bytecode penalty that is imposed each time an exception is thrown
- `MAX_TEAM_EXECUTION_TIME` - The maximum execution time that can be spent on a team in one match

### Debugging Constants
- `INDICATOR_STRING_MAX_LENGTH` - The maximum length of indicator strings that a player can associate with a robot
- `TIMELINE_LABEL_MAX_LENGTH` - The maximum length of a label to add to the timeline
