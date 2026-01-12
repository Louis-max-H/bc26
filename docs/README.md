
![image](https://hackmd.io/_uploads/Sk0YkfqE-e.png )

# Battlecode 2026
[Official Resources](https://play.battlecode.org/bc26/resources)
[This Document](https://hackmd.io/@_-O2yxm8TCuwksuVWcQA5w/BJ5colq4-l/edit)

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
./gradlew run -Pmaps=DefaultMedium -PteamA=v00_demo -PteamB=v00_demo -PlanguageA=java -PlanguageB=java
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


# Notes
## Match, phases, win
- Match: best of 3 games
- Game length: 2000 rounds

**Phases**
- Start in **Cooperation**
- Switch to **Backstab** immediately when any happens:
    - a rat damages an enemy rat
    - a rat triggers an enemy rat trap
    - a baby rat ratnaps an enemy baby rat

**Game end**
- If a team has 0 rat kings: that team auto loses (unless both hit 0 same round, then use points)
- If all cats die in **Cooperation**: game ends that round, winner by points
- If all cats die in **Backstab**: game continues until a team has 0 rat kings or round 2000

## Scoring (points)
Let:
- $D$ = your share of total cat damage ($\in [0,1]$)
- $K$ = your share of living rat kings at end ($\in [0,1]$)
- $C$ = your share of cheese delivered by baby rats to rat kings ($\in [0,1]$)

Points:
- Cooperation: $P = \text{round}(0.5D + 0.3K + 0.2C)$
- Backstab: $P = \text{round}(0.3D + 0.5K + 0.2C)$

Tiebreakers:
1. total global cheese at end
2. total rats alive at end (baby + kings)
3. random

## Notation (constants)
- Cheese: $c$
    - baby raw cheese $c_R$
    - team global cheese $c_G$
- Cooldowns: $\mathrm{cd}_m$ move, $\mathrm{cd}_t$ turn, $\mathrm{cd}_a$ action

## Turns and cooldowns
- Each robot runs once per round, call `Clock.yield()` to end your turn
- At the start of every round:
    - $\mathrm{cd}_m,\mathrm{cd}_t,\mathrm{cd}_a \leftarrow \max(0, \mathrm{cd}-10)$
- On your turn you may:
    - move if $\mathrm{cd}_m < 10$
    - turn if $\mathrm{cd}_t < 10$
    - use an action if $\mathrm{cd}_a < 10$
- Actions add cooldown (value depends on the action)

## Map
- Size: $30\times 30$ to $60\times 60$, both sides even
- Coordinates: bottom left $(0,0)$, +x east, +y north
- Symmetric
- Walls: impassable, share of map $\le 20\%$
- Dirt: impassable, share of map $\le 50\%$

## Resources
### Cheese
- Start: $c_G = 2500$
- Cheese mines: even count, mines apart by $\ge 5$ units
- Spawns near each mine in a $9\times 9$ area (symmetrical across map)
- Per round spawn chance at a mine (since last spawn):
    - $p(r) = 1 - 0.99^r$
- 5 cheese appear on spawn
- Not on walls, can be on dirt
- Must be reachable by cats (for spawn locations)

**Raw vs global**
- Baby picks up: becomes raw $c_R$
- Baby delivers to a rat king: becomes global $c_G$
- Rat king pickup: goes straight to $c_G$
- Spending cheese uses $c_R$ first, then $c_G$
- If a baby dies: its $c_R$ drops on its tile
- Carry slowdown: move and action cooldown scale with $0.01\cdot c_R$

### Dirt
- Team global stash
- Rat digging adds to stash
- Cat digging deletes dirt permanently (not added to any stash)

## Roboters

### Baby Rat (player)
- size $=1\times 1$
- hp $=100$
- $\mathrm{cd}_m = 10$
- $\mathrm{cd}_t = 10$
- vision: cone $90^\circ$ with range $R=\sqrt{20}$

**Bite (any rat can bite)**
- Target: adjacent enemy rat, enemy rat king, or cat
- Must be within your vision
- Damage:
    - base $=10$
    - if spend $X$ cheese: additional dmg $=\lceil \log X \rceil$

**Ratnap (baby only)**
- Can carry 1 baby rat (ally or enemy) from an adjacent visible tile if:
    - target is facing away /
    - target has less hp /
    - target is ally
- Carried rat:
    - can sense + squeak
    - cannot move, turn, or act
    - immune to attacks while carried (except cat pounce kills both)

**Drop**
- Any time: drop to adjacent passable, unoccupied tile
- Auto drop after 10 rounds: tries the tile in front
    - if blocked: swap (carrier becomes carried)

**Throw**
- Requires at least 1 empty landing space in front
- Flight:
    - speed: 2 tiles per turn
    - max: 4 turns (stops early if hits wall, dirt, or another rat)
- Damage on hit:
    - $t$ = turns spent in air
    - $5(4-t)$ to the thrown rat
    - $5(4-t)$ to any rat it collides with
- Landing stun:
    - add $10$ to $\mathrm{cd}_m,\mathrm{cd}_t,\mathrm{cd}_a$
    - instead add $30$ if it drops after 4 turns, or if it hit a target (need clarification)
- While flying:
    - cannot be attacked
    - does not trigger traps
    - cannot pick up cheese
    - can only sense + communicate
- If a thrown baby hits a cat: baby dies, cat sleeps 2 turns

**Traps (any rat can place)**
- Rat trap:
    - cost $=5c$
    - add $\mathrm{cd}_a {+}= 5$
    - max active per team = 25
    - trigger: enemy rat steps within radius $R=\sqrt{2}$
    - effect: 50 damage, add $\mathrm{cd}_m {+}= 20$
    - visible to allies, hidden to enemies + cats
    - if triggered during Cooperation: starts Backstab
- Cat trap (Cooperation only):
    - cost $=10c$
    - add $\mathrm{cd}_a {+}= 10$
    - max active per team = 10
    - trigger: cat steps within $R=\sqrt{2}$
    - effect: 100 damage, add $\mathrm{cd}_m {+}= 20$
    - cannot place after Backstab (existing ones stay)

**Remove trap (any rat)**
- Adjacent + visible
- no refund, no cooldown added

**Dirt (any rat)**
- Dig: adjacent + visible
    - cost $=10c$
    - add $\mathrm{cd}_a {+}= 25$
    - adds 1 dirt to team stash
- Place: adjacent + visible
    - cost $=10c$
    - add $\mathrm{cd}_a {+}= 25$
    - tile must not contain robot, wall, dirt, cheese, or mine

### Rat King (player)
- size = $3\times 3$
- start per team: 1
- hp = 500
- $\mathrm{cd}_m = 40$ (4 rounds between each move)
- $\mathrm{cd}_t = 10$
- vision: cone $360^\circ$ range $R=\sqrt{25}=5$

**Upkeep**
- end of each round: consumes $3c$
- if not enough cheese for any king: that king loses 10 hp and consumes none
- consume order: older kings first

**Upgrade to Rat King**
- Condition: 7+ allied rats in a baby rat’s surrounding $3\times 3$
- Cost: $50c$
- Space rules: surrounding $3\times 3$ has no walls or dirt and does not intersect any king or cat
- On creation:
    - triggers any traps whose trigger radius touches that $3\times 3$
    - new king hp $=\min(500,$ sum of hp in that $3\times 3)$
    - destroys all baby rats (ally or enemy) in that $3\times 3$
    - their raw cheese is added to the new team’s $c_G$
    - keeps the center baby rat’s current $\mathrm{cd}_m$ and $\mathrm{cd}_a$
- Limit: max 5 kings per team

**Spawn baby rat**
- Location: any empty tile adjacent to the king footprint
- Facing: same as king
- Cost:
    - $10 + 10\left\lfloor \frac{N}{4} \right\rfloor$
    - $N$ = baby rats alive on your team
- add $\mathrm{cd}_a {+}= 10$

### Cat (NPC)
- size = $2\times 2$
- hp = 10000
- $\mathrm{cd}_m = 10$
- acts last each round
- spawns even number of cats at map center in beginning
- vision: cone $180^\circ$ range $R=\sqrt{30}$
- senses like rats except it cannot sense cat traps
- cannot tell teams apart
- hears squeaks and learns the source location
- knows all walls and uses BFS around walls

**Attacks**
- Pounce:
    - jump up to 3 tiles, can jump over obstacles
    - landing tile cannot contain wall, dirt, or a rat king
    - kills all rats it lands on
    - sets movement cooldown to 20 for that jump
- Scratch:
    - 50 damage to a rat in its vision cone
    - add $\mathrm{cd}_a {+}= 15$
- Feeding:
    - if a thrown rat enters the cat’s occupied space: cat kills it
    - cat sleeps 2 turns (no move, no act)

**Movement and dirt**
- Walk: 1 tile forward (needs space, no dirt, no rat king)
- Turn: up to $90^\circ$ once per turn (before or after moving)
- Dig: remove adjacent dirt, add $\mathrm{cd}_a {+}= 30$, dirt is deleted

## Communication
### Shared array
- 64 ints, each $0$ to $1023$
- any robot: `readSharedArray()`
- only rat kings: `writeSharedArray()`
- probably smart to store there:
    - cheese mine locations
    - cat location
    - king location
    - assignment/waypoint locations
    - enemy location
    - dropped cheese
    - Walls

### Squeak
- `squeak(int messageContent)` at most once per turn
- message fields (in order):
    - messageContent, robotID, location, round
- received by:
    - allied rats within radius $R=4$
    - cats within the same radius
- persists 5 rounds

## Engine limits (practical)
- Bytecode per turn:
    - rats: 17500
    - rat kings: 20000
- No shared statics (separate JVM per robot)
- If you hit the bytecode cap: execution resumes next turn where it stopped
- Heap limit guideline: 8 MB per robot (tournament risk if exceeded)



# Todos
- Cooperative or not ?
    - Sometimes, maybe maps are not connected with other team
    - Otherwise, very likely to be not cooperative.
- Dig
    - Randomly
    - Toward center
    - Pathfinding
- Communication
    - Send loc of wall
    - Loc of units
    - Loc of interest point
- Throw units
    - Above walls
    - Make damage
- Build to
    - Trap other units (Maybe not usefull ?)
    - Block cat
