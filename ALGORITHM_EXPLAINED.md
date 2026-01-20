# Simple Optimizer Algorithm Explained

## Visual Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    SIMPLE OPTIMIZER                          │
│                 (Coordinate Descent)                         │
└─────────────────────────────────────────────────────────────┘

Start with initial config:
  PARAM_1 = 10
  PARAM_2 = 100
  PARAM_3 = 50

┌─────────────────────────────────────────────────────────────┐
│ ITERATION 1                                                  │
└─────────────────────────────────────────────────────────────┘

Step 1: Optimize PARAM_1
  Current: 10
  Test 3 values: [8, 10, 12]
  
  ┌─────────┬────────┬─────────┐
  │ Value   │ Score  │ Result  │
  ├─────────┼────────┼─────────┤
  │ 8       │ 48.5%  │         │
  │ 10      │ 50.0%  │         │
  │ 12      │ 53.2%  │ ⭐ BEST │
  └─────────┴────────┴─────────┘
  
  ➡️ Update: PARAM_1 = 12

Step 2: Optimize PARAM_2
  Current: 100
  Test 3 values: [80, 100, 120]
  
  ┌─────────┬────────┬─────────┐
  │ Value   │ Score  │ Result  │
  ├─────────┼────────┼─────────┤
  │ 80      │ 51.0%  │         │
  │ 100     │ 53.2%  │ ⭐ BEST │
  │ 120     │ 52.5%  │         │
  └─────────┴────────┴─────────┘
  
  ➡️ Keep: PARAM_2 = 100

Step 3: Optimize PARAM_3
  Current: 50
  Test 3 values: [40, 50, 60]
  
  ┌─────────┬────────┬─────────┐
  │ Value   │ Score  │ Result  │
  ├─────────┼────────┼─────────┤
  │ 40      │ 52.0%  │         │
  │ 50      │ 53.2%  │         │
  │ 60      │ 55.8%  │ ⭐ BEST │
  └─────────┴────────┴─────────┘
  
  ➡️ Update: PARAM_3 = 60

End of Iteration 1:
  PARAM_1 = 12 (was 10)
  PARAM_2 = 100 (unchanged)
  PARAM_3 = 60 (was 50)
  Best score: 55.8%

┌─────────────────────────────────────────────────────────────┐
│ ITERATION 2                                                  │
└─────────────────────────────────────────────────────────────┘

Step 1: Optimize PARAM_1
  Current: 12
  Test 3 values: [10, 12, 14]
  ... and so on ...

Continue until:
  - No improvement for 3 iterations
  - OR max iterations reached
```

## Step-by-Step Example

### Initial State
```json
{
  "PARAM_1": {"value": 10, "min": 5, "max": 20},
  "PARAM_2": {"value": 100, "min": 50, "max": 200},
  "PARAM_3": {"value": 50, "min": 20, "max": 100}
}
```

### How Test Values Are Generated

For each parameter, we generate 3 test values:

```
Current value: 10
Range: [5, 20]
Step size: (20 - 5) / 5 = 3

Test values:
  1. Lower:   max(5, 10 - 3) = 7
  2. Current: 10
  3. Higher:  min(20, 10 + 3) = 13

Result: [7, 10, 13]
```

### What Happens in Each Evaluation

```
For value = 7:
  1. Create temporary bot with PARAM_1 = 7
  2. Create opponent bot (base config)
  3. Run compare_bots.py --json
  4. Parse JSON to get win rate
  5. Save result: {"value": 7, "score": 48.5}
  6. Clean up temporary bots
```

## Progress Tracking

### After Each Evaluation

```json
{
  "evaluation": 1,
  "iteration": 1,
  "parameter": "PARAM_1",
  "value": 7,
  "score": 48.5,
  "timestamp": "2026-01-20T10:00:00"
}
```

This is appended to `history.json`.

### After Each Iteration

```json
{
  "iteration": 1,
  "evaluations": 9,
  "best_score": 55.8,
  "best_config": {
    "PARAM_1": {"value": 12, "min": 5, "max": 20},
    "PARAM_2": {"value": 100, "min": 50, "max": 200},
    "PARAM_3": {"value": 60, "min": 20, "max": 100}
  },
  "last_update": "2026-01-20T10:30:00"
}
```

This is saved to `progress.json`.

## Convergence

The optimizer stops when:

1. **No improvement for 3 iterations:**
   ```
   Iteration 5: best = 65.5%
   Iteration 6: best = 65.5% (no improvement)
   Iteration 7: best = 65.5% (no improvement)
   Iteration 8: best = 65.5% (no improvement)
   ➡️ STOP: Converged
   ```

2. **Max iterations reached:**
   ```
   Iteration 50: best = 70.2%
   ➡️ STOP: Max iterations reached
   ```

## Why This Works

### Coordinate Descent Principle

Instead of optimizing all parameters at once (which is hard), we:
1. Fix all parameters except one
2. Find the best value for that parameter
3. Move to the next parameter
4. Repeat

This is called **coordinate descent** and works well when:
- Parameters are somewhat independent
- You have limited computational resources
- You want simple, interpretable results

### Example: Why It's Better Than Random Search

**Random Search (10 evaluations):**
```
Try 10 random configurations
Best might be: 55%
No systematic improvement
```

**Coordinate Descent (10 evaluations, 3 params):**
```
Iteration 1:
  PARAM_1: 3 tests → improve from 50% to 53%
  PARAM_2: 3 tests → improve from 53% to 55%
  PARAM_3: 3 tests → improve from 55% to 58%

Result: 58% (systematic improvement)
```

## Real-World Example

Let's say you're optimizing a combat bot:

```json
{
  "ATTACK_THRESHOLD": {"value": 10, "min": 5, "max": 20},
  "RETREAT_HP": {"value": 50, "min": 20, "max": 80},
  "SCOUT_DISTANCE": {"value": 30, "min": 10, "max": 50}
}
```

### Iteration 1

**Optimize ATTACK_THRESHOLD:**
- Test [8, 10, 12]
- Find that 12 is best (more aggressive = better)
- Win rate: 50% → 53%

**Optimize RETREAT_HP:**
- Test [40, 50, 60]
- Find that 40 is best (retreat earlier = survive longer)
- Win rate: 53% → 56%

**Optimize SCOUT_DISTANCE:**
- Test [24, 30, 36]
- Find that 36 is best (scout farther = better info)
- Win rate: 56% → 60%

### Iteration 2

Now with improved config, test again:
- ATTACK_THRESHOLD: maybe 14 is even better?
- RETREAT_HP: maybe 35 is optimal?
- SCOUT_DISTANCE: maybe 40 is too far?

Continue until no more improvements...

## Tips for Best Results

1. **Start with reasonable initial values**
   - Use your current best config as starting point

2. **Choose appropriate ranges**
   - Too narrow: might miss optimal value
   - Too wide: takes longer to converge

3. **Use fewer maps for faster iteration**
   - Start with 2-3 maps for quick feedback
   - Use all maps for final optimization

4. **Check the history**
   - See which parameters matter most
   - Narrow ranges for important parameters
   - Widen ranges for parameters that don't change

5. **Resume from best**
   - Use `best_config.json` as new `--base-config`
   - Run again with narrower ranges
   - Refine the solution

## Comparison with GRASP

### Simple Optimizer (Coordinate Descent)
```
Iteration 1:
  PARAM_1: [8, 10, 12]
  PARAM_2: [80, 100, 120]
  PARAM_3: [40, 50, 60]

Total: 9 evaluations
Sequential: one at a time
Easy to understand: yes
```

### GRASP
```
Iteration 1:
  Generate 10 random configs (with memory)
  Evaluate all 10 in parallel
  Update memory with results

Total: 10 evaluations
Parallel: all at once
Easy to understand: no
```

**When to use Simple Optimizer:**
- You're learning
- You want to understand what's happening
- You have limited resources
- You want interpretable results

**When to use GRASP:**
- You need maximum speed
- You have many CPUs
- You understand the algorithm
- You want to explore parameter space widely

