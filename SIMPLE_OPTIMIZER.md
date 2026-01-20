# Simple Coordinate Descent Optimizer

A simplified optimization approach for Battlecode bot parameters that uses coordinate descent: optimizing one parameter at a time.

## How It Works

The optimizer follows this simple loop:

```
While not converged (max iterations):
    For each parameter:
        1. Generate 3 test values (lower, current, higher)
        2. Compare these 3 bots against the base opponent
        3. Keep the best configuration
        4. Move to next parameter
```

## Features

✅ **Simple & Easy to Understand**: One parameter at a time, clear progress
✅ **Progress Tracking**: Saves progress after each evaluation
✅ **JSON Results**: Easy to parse and reuse results
✅ **Automatic Convergence**: Stops when no improvement is found
✅ **Full History**: Tracks all evaluations and scores

## Usage

### Basic Usage

```bash
python3 src/simple_optimizer.py --template config/template.json --base-config config/base_config.json
```

### With Custom Maps

```bash
python3 src/simple_optimizer.py \
    --template config/template.json \
    --base-config config/base_config.json \
    --maps DefaultSmall,DefaultMedium
```

### Full Options

```bash
python3 src/simple_optimizer.py \
    --template config/template.json \
    --base-config config/base_config.json \
    --source current \
    --maps DefaultSmall,DefaultMedium \
    --output-dir BC26/my_optimization \
    --max-iterations 50
```

## Configuration Files

### Template Configuration

The template file defines parameters with their bounds:

```json
{
  "PARAM_NAME_1": {
    "value": 10,
    "min": 5,
    "max": 20
  },
  "PARAM_NAME_2": {
    "value": 100,
    "min": 50,
    "max": 200
  }
}
```

### Base Configuration

The base configuration is your opponent (typically your current best bot):

```json
{
  "PARAM_NAME_1": {
    "value": 10,
    "min": 5,
    "max": 20
  },
  "PARAM_NAME_2": {
    "value": 100,
    "min": 50,
    "max": 200
  }
}
```

## Output Files

The optimizer saves three files in the output directory:

1. **`best_config.json`**: The best configuration found so far
2. **`progress.json`**: Current optimization progress and best score
3. **`history.json`**: Complete history of all evaluations

### Progress File Example

```json
{
  "iteration": 5,
  "evaluations": 42,
  "best_score": 65.5,
  "best_config": { ... },
  "last_update": "2026-01-20T10:30:00"
}
```

### History File Example

```json
[
  {
    "evaluation": 1,
    "iteration": 1,
    "parameter": "PARAM_NAME_1",
    "value": 8,
    "score": 52.3,
    "timestamp": "2026-01-20T10:00:00"
  },
  {
    "evaluation": 2,
    "iteration": 1,
    "parameter": "PARAM_NAME_1",
    "value": 10,
    "score": 55.7,
    "timestamp": "2026-01-20T10:05:00"
  }
]
```

## Example Output

```
================================================================================
🚀 Starting Simple Coordinate Descent Optimization
================================================================================
Parameters to optimize: ['PARAM_1', 'PARAM_2', 'PARAM_3']
Max iterations: 100
Source: current
Maps: DefaultSmall,DefaultMedium
Output directory: BC26/simple_optimizer
================================================================================

📊 Evaluating initial configuration...
Initial score: 50.00%

================================================================================
🔄 Iteration 1
================================================================================
Current best score: 50.00%
Total evaluations: 0
================================================================================

📊 Optimizing parameter: PARAM_1
   Current value: 10
   Range: [5, 20]
   Testing values: [8, 10, 12]

   [1/3] Testing PARAM_1 = 8...
         Score: 48.50%

   [2/3] Testing PARAM_1 = 10...
         Score: 50.00%

   [3/3] Testing PARAM_1 = 12...
         Score: 53.20%
   🌟 New best score: 53.20%

   ✅ Best value for PARAM_1: 12 (score: 53.20%)
   ➡️  Updated PARAM_1: 10 → 12

...
```

## Comparison with Other Optimizers

| Feature | simple_optimizer.py | grasp_parallel.py |
|---------|-------------------|-------------------|
| Approach | Coordinate descent | GRASP with memory |
| Complexity | Simple | Complex |
| Parallelization | Sequential | Parallel batches |
| Progress tracking | ✅ Clear | ✅ Advanced |
| Results format | ✅ JSON | ⚠️ Hard to parse |
| Easy to understand | ✅ Yes | ❌ No |

## Tips

1. **Start with fewer maps** for faster iteration during development
2. **Use `--max-iterations 10`** for quick tests
3. **Check `history.json`** to see all parameter values tested
4. **Resume optimization** by using the `best_config.json` as your new `--base-config`

## Improvements Made to compare_bots.py

The `compare_bots.py` script now supports JSON output:

```bash
# Old way (table output, hard to parse)
python3 src/compare_bots.py player1 player2

# New way (JSON output, easy to parse)
python3 src/compare_bots.py player1 player2 --json
```

JSON output includes:
- Win rates for both players
- Individual match results
- Maps won/lost/drawn
- Win conditions breakdown
- All data in structured, easy-to-parse format

