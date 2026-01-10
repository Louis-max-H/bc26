# Installation
## Quick start
```bash
git clone https://github.com/battlecode/battlecode26-scaffold bc26
cd bc26
./gradlew update
./gradlew build

./gradlew -q tasks listMap
./gradlew run -Pmaps=DefaultMedium -PteamA=examplefuncsplayer -PteamB=examplefuncsplayer -PlanguageA=java -PlanguageB=java
```

You should be using java 21
```bash
sudo pacman -Sy java-21-openjdk
sudo archlinux-java set java-21-openjdk
```

You may now want to use our bots
```bash
git clone https://github.com/FreGeh/battlecode-26 src
./gradlew run -Pmaps=DefaultMedium -PteamA=v01_template -PteamB=v01_template -PlanguageA=java -PlanguageB=java
```

## Utils
**Copy bot**
```python
python copybot.py ref_best {your_name}
```

**Compare bots**
```python
python compare_bots.py ref_best {your_bot}
```


# Intro
Hi ! This is a quick recap for you :blush:
The idea is to be familiar with this code structure during the first week.

I will work on `pathfinding` in the meantime to let you implements your first states.
States ? Yes ! We are using a finit `state machine` this year :D
But why ?

- Because my last year submission with the team was quite messy ^^"
- When doing an action, we may need multiple turn to finish it.
- It will be easier to have multiple state, one per action, than a bunch of `if/else`
- You can code an action that need multiples turns without thinking about the rest or other sides actions :D

# Okay, tell me more !

Let's start by presenting the project structure :
- `bc26`
    - `src` : Our files with our github
    - `matches` : If you want to upload somes replay in the viewer
    - `client` : The executable to have a viewer on your computer, or you can use the one online

In the `src/` folder:
- `src/`
    - `States/` : Your actions (the node of your state machine)
    - `Robots/` : How to ordonates your actions (describe transition of your state machine)
    - `Utils/`  : Some functions that may help you (`PathFinding`, `Sets`, `Tools`)
    `RobotPlayer.java` : The file that will call the correct class in `Robots/`

# But, how do I create my State ?

Okay ! Now that we understand the project structure, let's create a new state :D
Start by copying the `HelloWorld` state in `States/`

```java
package v01_template.States;
import static v01_template.States.Code.*;

public class HelloWorld extends State {
    public HelloWorld(){

        // Initialisation

        this.name = "HelloWorld";
    }

    @Override
    public Result run(){

        // My action

        print("Hello World!");
        return new Result(OK, "My result message ?");
    };
}
```

Wow ... Simple as that ? But ... What is `Result(OK, "msg")` ? It's just to have more explanations for debugs :sweat:
For example, if you run the bot you will have :
```
[server] -------------------- Match Starting --------------------
[server] v01_template vs. v01_template on DefaultMedium
[A: #3@1] Starting at round 141 bytecode 141
[A: #3@1] Done init at bytecode 253
[A: #3@1]    Init    153
[A: #3@1]       Init classic variables
[A: #3@1]       Initializing Explore state
[A: #3@1]    Explore 1355
[A: #3@1]       Normalize coef : 1836
[A: #3@1]       Exploring:
[A: #3@1]        ⬆ 2908224
[A: #3@1]        ↗ 1999404
[A: #3@1]        ➡ 3271752
[A: #3@1]        ↘ 1999404
[A: #3@1]        ⬇ 3271752
[A: #3@1]        ↙ 1999404
[A: #3@1]        ⬅ 3271752
[A: #3@1]        ↖ 1999404
[A: #3@1]        ⏹ 181764
[A: #3@1]       <= OK Turning to EAST
```

Explanations:
```
A    : Team A (debug are only done for the player A with round <= 400)
#3@1 : player id 3, round 1
The digits after `Init` and `explore` are the current number of bytecodes used (max 17500 for baby rats)
<= OK turning to EAST : Return code + message
```

Nice and pretty no ?
But wait for the fun part ! You have more return states :D

Non blocking return code :
- `Ok` : All good !
- `Cant` : task can't be played (cooldown for example)
- `Warn` / `Err` : Same as OK, just for logging

blocking codes :
- `Lock`        : End the turn, go to init, then restart from current state directly
- `End of turn` : End the turn, next turn will restart to init

Let's use them in two examples, first one to combo enemies :
```
public class Attack extends State {
    MapLocation target;
    public Attack(){
        this.name = "Attack";
    }

    @Override
    public Result run(){
        // Should I play this action ?
        if(health == 1){
            return new Result(Warn, "I'm very low guys");
        }

        // Update action if already exist
        if(target != null and target not visible anymore){
            target == null;
        }

        // Update action for new targets
        if(target == null){
            target = nearestEnemies something like that;
        }

        // If no target, return
        if(target == null){
            return new Result(CANT, "No enemies");
        }

        // Play action
        attack(target);
        return new Result(LOCK, "Attacking " + target);
    };
}
```

We use `lock` because we want to stay in the state to attack enemy. Let's make an example where we wait for cheese.

```
public class WaitCheese extends State {
    MapLocation target;
    public WaitCheese(){
        this.name = "WaitCheese";
    }

    @Override
    public Result run(){
        // Should I play this action ?
        if(already have cheese){
            return new Result(OK, "Already cheese");
        }

        // Update action if already exist
        // Mines can't deasapear, nothing to do


        // Update action for new targets
        if(target == null and mine cheese){
            target == mine cheese;
        }

        // If no target, return
        if(target == null){
            return new Result(CANT, "No enemies");
        }

        // Play action
        if(fromage nearby){
            goCollectCheese();
            return new Result(END_OF_TURN, "Going for cheese ! ");
        }

        goToward(target);
        return new Result(END_OF_TURN, "Wait for cheese" + target);
    };
}
```

Here we have `END_OF_TURN`, because we don't want to do anythings else than waiting for cheese.
With this state machine, we would otherwise explore and maybe lost cheese mine from our view :'(

`Init` -> `Attack` -> `WaitCheese` -> `Explore` -> `End of turn`
  ^                                           |
   \_________________________________________/


# Okay, perfect, how can I define the transition between states ?

Hopla, go to `Robots/King.java`:
```java
package v01_template.Robots;
import v01_template.RobotPlayer;
import v01_template.States.*;

public class King extends Robot {
    State explore;
    State helloWorld;

    @Override
    public void init(){
        this.init = new Init();
        this.explore = new Explore();
        this.helloWorld = new HelloWorld();
        this.endTurn = new EndTurn();
    }

    @Override
    public void updateState(Result resultBefore){
        currentState = switch (currentState.name) {
            case "Init" -> explore;
            case "Explore" -> helloWorld;
            case "HelloWorld" -> endTurn;
            case "EndTurn" -> init;
            default -> {
                Robot.err(currentState.name + " don't match any states. Fallback to init");
                yield init;
            }
        };
    }
}
```

The function `updateState` take the result of `currentState.run()`, but you may not need it.
If you have blockings code (`LOCK`, `END_OF_TURN`), it will be directly handed in `Robots/Robot.java` ;)


Et voila !

# And the folder Utils ???

Of course ^^", you have some strange class starting by `Fast*` for basic data structure using `String` based magic.
And then... The `pathfinding` ! Here is a simplified copy/past from `States/Explore.java` for example :

```java
// Calculate heuristic
// For each nearby cells, add their heuristic to the direction that lead to this cell
char scores[] = new char[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
MapLocation myLoc = rc.getLocation();
for(MapLocation loc: rc.getAllLocationsWithinRadiusSquared(myLoc, VISION RADIUS)){
    scores[myLoc.directionTo(loc).ordinal()] += heuristic[LOCATION HASH];
}

// Add the heuristic
PathFinding.addScores(
        Tools.toInt9(scores),
        COEFFICIENT OF THE ADDED SCORES
);
PathFinding.modificatorOrthogonal(); // Add a x2 bonus if move orthogonal

// Turn and move
Direction bestDir = PathFinding.bestDir();
rc.turn(bestDir);
if(PathFinding.move(bestDir).notOk()){
    print("Can't move to best direction.");
}
```

You have also a function to go to a destination, actually very basic ...
```java
class Pathfinding {
    public static Result moveTo(MapLocation loc) throws GameActionException {
        return move(Robot.rc.getLocation().directionTo(loc));
    }

    ...
}
```

I will work on it this week-end ^^"

Why adding scores like that ? It's open for changes, but the main idea is to implements multiples heuristics and then, lets each states use them.

For example, we can imagine the state `AvoidCat` that move backwards cats but also towards `allies` for help ?

# FAQ

- I want my state to do some stuff at each new turn, even when not called because of other state using `LOCK` of `END_OF_TURN` before my state.
    - You add this code to the `init` node :D
- Why `static` everywhere ?
    - Because it cost one bytecode less than non-static
    - It's also easier to access things, for example `Robot.rc`
