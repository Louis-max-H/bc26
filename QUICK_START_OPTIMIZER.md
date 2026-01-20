# Quick Start: Simple Optimizer

## 5-Minute Setup

### 1. Export Your Current Parameters

```bash
cd /home/lmx/bc26
python3 src/params.py current --export my_base_config.json
```

### 2. Create Template with Bounds

Copy and edit to add min/max bounds:

```bash
cp my_base_config.json my_template.json
# Edit my_template.json
```

**Example template:**
```json
{
  "ATTACK_THRESHOLD": {
    "value": 10,
    "min": 5,
    "max": 20
  },
  "RETREAT_HP": {
    "value": 50,
    "min": 20,
    "max": 80
  }
}
```

### 3. Run Optimizer

```bash
python3 src/simple_optimizer.py \
    --template my_template.json \
    --base-config my_base_config.json \
    --maps DefaultSmall,DefaultMedium \
    --max-iterations 10
```

### 4. Check Results

```bash
# Best configuration found
cat BC26/simple_optimizer/best_config.json

# Current progress
cat BC26/simple_optimizer/progress.json

# Full history
cat BC26/simple_optimizer/history.json
```

## Command Line Options

```bash
python3 src/simple_optimizer.py \
    --template TEMPLATE.json          # Required: parameter bounds
    --base-config BASE.json           # Required: opponent config
    --source current                  # Optional: bot source (default: current)
    --maps Map1,Map2                  # Optional: specific maps
    --output-dir BC26/my_results      # Optional: output directory
    --max-iterations 50               # Optional: max iterations (default: 100)
```

## Understanding Output

### Console Output

```
================================================================================
🚀 Starting Simple Coordinate Descent Optimization
================================================================================
Parameters to optimize: ['PARAM_1', 'PARAM_2', 'PARAM_3']
Max iterations: 10
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
```

### Output Files

**`best_config.json`** - Use this for your bot!
```json
{
  "PARAM_1": {
    "value": 12,
    "min": 5,
    "max": 20
  },
  "PARAM_2": {
    "value": 100,
    "min": 50,
    "max": 200
  }
}
```

**`progress.json`** - Track optimization progress
```json
{
  "iteration": 5,
  "evaluations": 42,
  "best_score": 65.5,
  "best_config": { ... },
  "last_update": "2026-01-20T10:30:00"
}
```

**`history.json`** - Analyze what worked
```json
[
  {
    "evaluation": 1,
    "iteration": 1,
    "parameter": "PARAM_1",
    "value": 8,
    "score": 48.5,
    "timestamp": "2026-01-20T10:00:00"
  },
  {
    "evaluation": 2,
    "iteration": 1,
    "parameter": "PARAM_1",
    "value": 10,
    "score": 50.0,
    "timestamp": "2026-01-20T10:05:00"
  }
]
```

## Common Workflows

### Quick Test (2-3 parameters, 2 maps)

```bash
python3 src/simple_optimizer.py \
    --template template.json \
    --base-config base.json \
    --maps DefaultSmall,DefaultMedium \
    --max-iterations 5
```

**Time:** ~30 minutes (3 params × 3 values × 5 iterations × 2 maps)

### Full Optimization (5+ parameters, all maps)

```bash
python3 src/simple_optimizer.py \
    --template template.json \
    --base-config base.json \
    --max-iterations 20
```

**Time:** Several hours (depends on number of parameters and maps)

### Resume from Best

After first optimization:

```bash
# Use best config as new base
cp BC26/simple_optimizer/best_config.json new_base.json

# Run again with narrower ranges
# Edit template.json to narrow ranges around best values

python3 src/simple_optimizer.py \
    --template template_narrow.json \
    --base-config new_base.json \
    --max-iterations 10
```

## Troubleshooting

### "Template file not found"

```bash
# Check file exists
ls -la my_template.json

# Use absolute path
python3 src/simple_optimizer.py \
    --template /home/lmx/bc26/my_template.json \
    --base-config /home/lmx/bc26/my_base_config.json
```

### "Failed to copy bot"

```bash
# Check source bot exists
ls -la src/current/

# Try different source
python3 src/simple_optimizer.py \
    --template template.json \
    --base-config base.json \
    --source examplefuncsplayer
```

### Optimization is too slow

```bash
# Use fewer maps
python3 src/simple_optimizer.py \
    --template template.json \
    --base-config base.json \
    --maps DefaultSmall \
    --max-iterations 5
```

### Want to see what compare_bots returns

```bash
# Test JSON output
python3 src/compare_bots.py player1 player2 --json --maps DefaultSmall
```

## Tips for Success

### 1. Start Small
- Begin with 2-3 most important parameters
- Use 1-2 maps for quick feedback
- Increase scope once you understand the process

### 2. Choose Good Ranges
```json
// ❌ Too narrow
"ATTACK_THRESHOLD": {"value": 10, "min": 9, "max": 11}

// ✅ Good range
"ATTACK_THRESHOLD": {"value": 10, "min": 5, "max": 20}

// ❌ Too wide
"ATTACK_THRESHOLD": {"value": 10, "min": 1, "max": 1000}
```

### 3. Analyze History

```bash
# See which parameters changed most
cat BC26/simple_optimizer/history.json | grep "parameter"

# See score progression
cat BC26/simple_optimizer/history.json | grep "score"
```

### 4. Iterate

```
Round 1: Wide ranges, few maps, quick test
  ↓
Round 2: Use best config, narrow ranges, more maps
  ↓
Round 3: Fine-tune with all maps
```

## Example: Complete Workflow

```bash
# 1. Export current params
python3 src/params.py current --export base.json

# 2. Create template (manually edit to add bounds)
cp base.json template.json
nano template.json  # Add min/max

# 3. Quick test with 2 maps
python3 src/simple_optimizer.py \
    --template template.json \
    --base-config base.json \
    --maps DefaultSmall,DefaultMedium \
    --max-iterations 5 \
    --output-dir BC26/test1

# 4. Check results
cat BC26/test1/best_config.json

# 5. Full optimization with all maps
cp BC26/test1/best_config.json base_v2.json
python3 src/simple_optimizer.py \
    --template template.json \
    --base-config base_v2.json \
    --max-iterations 20 \
    --output-dir BC26/full_opt

# 6. Use best config in your bot
python3 src/params.py current --import BC26/full_opt/best_config.json
```

## Next Steps

1. Read `SIMPLE_OPTIMIZER.md` for detailed documentation
2. Read `ALGORITHM_EXPLAINED.md` to understand how it works
3. Check `CHANGES_SUMMARY.md` for what was modified
4. Try optimizing your bot parameters!

## Getting Help

If you have issues:
1. Check the console output for error messages
2. Verify your JSON files are valid: `python3 -m json.tool template.json`
3. Test compare_bots separately: `python3 src/compare_bots.py bot1 bot2 --json`
4. Start with the example configs: `example_configs/template_example.json`

