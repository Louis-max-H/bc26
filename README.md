# Battlecode 2026 Scaffold - Java
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

## Configure java


## Utils
**Copy bot**
```python
python copybot.py ref_best {your_name}
```

**Compare bots**
```python
python compare_bots.py ref_best {your_bot}
```

## Params
```bash
python3 src/params.py current --export params.json     # Save Params.java into params.json
python3 src/params.py current --import params.json     # Load params.json into Params.java
python3 src/jinja.py src/current --prod --params params.json     # Use params for configuration of jinja
```

## Compare bots
```bash
python3 src/compare_configs.py --paramsTeamA config_a.json --paramsTeamB config_b.json --maps DefaultSmall,DefaultMedium
```

This will:
- Generate random IDs for both teams
- Copy the current bot to temporary folders
- Import parameters for each bot
- Generate files with Jinja
- Compare both bots
- Save results to `BC26/grasp/results/results.json`
- Save bot configs to `BC26/grasp/bots/tmpXXXX.json`

Options:
- `--source FOLDER`: Source folder to copy (default: current)
- `--no-cleanup`: Keep temporary folders after comparison
- `--output-dir DIR`: Output directory for results (default: BC26/grasp)
- `--maps MAP1,MAP2`: Comma-separated list of maps to test (if not specified, uses all maps from maps/ folder)

## Parameter Optimization

### Simple Coordinate Descent (Recommended for beginners)

Optimize parameters one at a time - simple and easy to understand:

```bash
python3 simple_optimizer.py \
    --template configs/template_config.json \
    --base-config configs/base_config.json \
    --source current \
    --output-dir logs/my_optimization \
    --max-iterations 50 \
    --threads 3
```

**How it works:**
1. For each parameter:
   - Test 3 values (lower, current, higher)
   - Compare these 3 bots
   - Keep the best one
2. Repeat until convergence
