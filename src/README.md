
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

# Start to code !
You may check .html or .md in documentation/ folder, it's the same tutorial for the template code proposed :D

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
