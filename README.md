# Battlecode 2026 Scaffold - Java
[Official Resources](https://play.battlecode.org/bc26/resources)

Post mortem on my website : [https://louis-max.gitlab.io/blog/battlecode/](https://louis-max.gitlab.io/blog/battlecode/)

# Installation
## Quick start
```bash
git clone https://github.com/Louis-max-H/bc26
cd bc26
./gradlew update
./gradlew build

./gradlew -q tasks listMap
python3 src/jinja.py src/current --prod --params current.json
./gradlew run -Pmaps=pipes -PteamB=current -PteamA=current -PlanguageA=java -PlanguageB=java | tee tmp 
```

You should be using java 21
```bash
sudo pacman -Sy java-21-openjdk
sudo archlinux-java set java-21-openjdk
```
