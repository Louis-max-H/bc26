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

## GRASP Optimization

Optimize bot parameters using GRASP (Greedy Randomized Adaptive Search Procedure).

**Local execution:**
```bash
python3 src/grasp_parallel.py --template example_config.json --base-config example_config.json --iterations 5 --batch-size 2 --maps DefaultSmall,DefaultMedium
```

Template : Domaine que l'on veut tester
Base config : Meilleur configuration

**On SLURM cluster:**
```bash
# Quick test (2 iterations)
sbatch run_grasp_quick.sbatch

# Full optimization (5 iterations)
sbatch run_grasp.sbatch

# Check status
squeue -u $USER

# View logs in real-time
tail -f logs/grasp_JOBID.out
```

**Features:**
- 🔄 Adaptive memory: learns from good parameter values
- ⚡ Parallel evaluation: uses all available CPUs
- 📊 Progress bars: shows performance metrics (eval/s, best score, etc.)
- 💾 Automatic checkpoints: saves progress every N iterations
- 🎯 Auto-detection: automatically detects number of CPUs

**Results:** Best configuration saved in `BC26/grasp_results/best_config.json`

**See `GRASP_README.md` for detailed documentation.**

## Useful Gradlew Commands

- `./gradlew build` - Compiles your player
- `./gradlew run` - Runs a game with the settings in gradle.properties
- `./gradlew update` - Update configurations for the latest version (run often)
- `./gradlew zipForSubmit` - Create a submittable zip file
- `./gradlew tasks` - See what else you can do!

## Documentation

- `README.md` - This file (quick start guide)
- `PARAMS_README.md` - Parameter system documentation
- `GRASP_README.md` - GRASP optimization guide (cluster usage, tuning)
- `docs/` - Official Battlecode documentation

# Start to code!
You may check `.html` or `.md` files in the `docs/` folder for tutorials on the template code.
