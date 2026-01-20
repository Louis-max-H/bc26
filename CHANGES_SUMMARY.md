# Summary of Changes

## What Was Created

### 1. Modified `compare_bots.py` 
**Location:** `/home/lmx/bc26/src/compare_bots.py`

**Changes:**
- Added `--json` flag to output results as JSON instead of rich tables
- Added `to_json()` method to `State` class that returns structured data
- JSON output includes:
  - Win rates for both players
  - Individual match results
  - Maps won/lost/drawn
  - Win conditions breakdown
  - All data in easy-to-parse format

**Usage:**
```bash
# Old way (table output)
python3 src/compare_bots.py player1 player2

# New way (JSON output, easy to parse)
python3 src/compare_bots.py player1 player2 --json
```

### 2. New `simple_optimizer.py`
**Location:** `/home/lmx/bc26/src/simple_optimizer.py`

**What it does:**
- Simple coordinate descent optimization
- Tests one parameter at a time
- For each parameter, tests 3 values and keeps the best
- Tracks progress and saves results in JSON format

**Algorithm:**
```
While not converged:
    For each parameter:
        1. Generate 3 test values (lower, current, higher)
        2. Compare these 3 bots against base opponent
        3. Keep the best configuration
        4. Move to next parameter
```

**Features:**
- ✅ Clear progress tracking with iteration numbers
- ✅ JSON output for all results (easy to parse and reuse)
- ✅ Saves 3 files:
  - `best_config.json` - Best configuration found
  - `progress.json` - Current progress and best score
  - `history.json` - Complete history of all evaluations
- ✅ Automatic convergence detection
- ✅ Resumes from best configuration

**Usage:**
```bash
python3 src/simple_optimizer.py \
    --template config/template.json \
    --base-config config/base_config.json \
    --maps DefaultSmall,DefaultMedium \
    --max-iterations 50
```

### 3. Documentation
**Location:** `/home/lmx/bc26/SIMPLE_OPTIMIZER.md`

Complete documentation including:
- How the algorithm works
- Usage examples
- Configuration file format
- Output file descriptions
- Comparison with other optimizers
- Tips and best practices

### 4. Example Configurations
**Location:** `/home/lmx/bc26/example_configs/`

- `template_example.json` - Example template with parameter bounds
- `base_example.json` - Example base configuration

### 5. Updated README
**Location:** `/home/lmx/bc26/README.md`

Added section on Simple Coordinate Descent optimizer with usage examples.

## Key Improvements

### 1. Easier to Understand
- **Before:** Complex GRASP algorithm with parallel evaluations, adaptive memory
- **After:** Simple coordinate descent - one parameter at a time

### 2. Better Progress Tracking
- **Before:** Results hard to parse from compare_bots output
- **After:** 
  - Clear iteration numbers
  - Evaluation counter
  - Best score tracking
  - Full history in JSON

### 3. Easy to Parse Results
- **Before:** Rich table output, hard to parse programmatically
- **After:** Clean JSON output with all data structured

### 4. Resumable
- Save progress after each evaluation
- Can resume by using `best_config.json` as new `--base-config`

## Example Output Structure

### progress.json
```json
{
  "iteration": 5,
  "evaluations": 42,
  "best_score": 65.5,
  "best_config": { ... },
  "last_update": "2026-01-20T10:30:00"
}
```

### history.json
```json
[
  {
    "evaluation": 1,
    "iteration": 1,
    "parameter": "PARAM_NAME",
    "value": 10,
    "score": 52.3,
    "timestamp": "2026-01-20T10:00:00"
  }
]
```

### compare_bots JSON output
```json
{
  "player1": "bot1",
  "player2": "bot2",
  "total_matches": 20,
  "player1_stats": {
    "wins": 12,
    "losses": 8,
    "win_rate": 60.0,
    "win_maps": 5,
    "draw_maps": 2,
    "lose_maps": 3
  },
  "matches": [...]
}
```

## Quick Start

1. **Prepare your configuration files:**
   ```bash
   # Export current params as base config
   python3 src/params.py current --export base_config.json
   
   # Create template with bounds (edit manually)
   cp base_config.json template.json
   # Edit template.json to set min/max bounds
   ```

2. **Run optimization:**
   ```bash
   python3 src/simple_optimizer.py \
       --template template.json \
       --base-config base_config.json \
       --maps DefaultSmall,DefaultMedium \
       --max-iterations 20
   ```

3. **Check results:**
   ```bash
   # Best configuration
   cat BC26/simple_optimizer/best_config.json
   
   # Progress
   cat BC26/simple_optimizer/progress.json
   
   # Full history
   cat BC26/simple_optimizer/history.json
   ```

## Comparison: Simple vs GRASP

| Feature | simple_optimizer.py | grasp_parallel.py |
|---------|-------------------|-------------------|
| Algorithm | Coordinate descent | GRASP with memory |
| Complexity | ⭐ Simple | ⭐⭐⭐ Complex |
| Parallelization | Sequential | Parallel batches |
| Progress tracking | ✅ Clear & detailed | ✅ Advanced |
| Results format | ✅ JSON (easy) | ⚠️ Hard to parse |
| Easy to understand | ✅ Yes | ❌ No |
| Good for beginners | ✅ Yes | ❌ No |
| Speed | Slower (sequential) | Faster (parallel) |

## Next Steps

1. Try the simple optimizer with your actual bot parameters
2. Check the history to see which parameters matter most
3. Use the best configuration as your new base
4. Iterate to improve further

