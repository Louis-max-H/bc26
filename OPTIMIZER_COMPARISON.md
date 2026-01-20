# Optimizer Comparison Guide

## Quick Decision Tree

```
Do you want to optimize bot parameters?
│
├─ YES → Are you new to optimization?
│        │
│        ├─ YES → Use simple_optimizer.py ✅
│        │        (Easy to understand, clear progress)
│        │
│        └─ NO → Do you have many CPUs available?
│                 │
│                 ├─ YES → Use grasp_parallel.py
│                 │        (Faster, parallel evaluation)
│                 │
│                 └─ NO → Use simple_optimizer.py ✅
│                          (Better for limited resources)
│
└─ NO → Just use compare_bots.py or compare_configs.py
         (Manual comparison of specific configs)
```

## Feature Comparison

| Feature | simple_optimizer.py | grasp_parallel.py | compare_configs.py |
|---------|-------------------|-------------------|-------------------|
| **Purpose** | Parameter optimization | Parameter optimization | One-time comparison |
| **Algorithm** | Coordinate descent | GRASP with memory | N/A |
| **Approach** | Sequential | Parallel batches | Single comparison |
| **Complexity** | ⭐ Simple | ⭐⭐⭐ Complex | ⭐ Simple |
| **Learning curve** | Easy | Steep | Easy |
| **Speed** | Medium | Fast | N/A |
| **CPU usage** | 1 core | All cores | 1 core |
| **Progress tracking** | ✅ Excellent | ✅ Good | ❌ None |
| **Results format** | ✅ JSON | ⚠️ Mixed | ⚠️ Mixed |
| **Resumable** | ✅ Yes | ✅ Yes | ❌ No |
| **Convergence** | ✅ Automatic | ⚠️ Manual | N/A |
| **History** | ✅ Full history | ⚠️ Checkpoints only | ❌ None |
| **Best for** | Learning, clarity | Production, speed | Quick tests |

## Detailed Comparison

### Simple Optimizer (simple_optimizer.py)

**Algorithm:** Coordinate Descent
```
For each iteration:
    For each parameter:
        Test 3 values
        Keep best
```

**Pros:**
- ✅ Very easy to understand
- ✅ Clear progress tracking
- ✅ JSON results (easy to parse)
- ✅ Full history of all tests
- ✅ Automatic convergence
- ✅ Works well with limited resources

**Cons:**
- ❌ Slower (sequential testing)
- ❌ May miss global optimum
- ❌ Not suitable for many parameters (>10)

**Best for:**
- Learning optimization
- Understanding parameter effects
- Limited computational resources
- Small to medium parameter spaces (3-8 parameters)

**Example:**
```bash
python3 src/simple_optimizer.py \
    --template template.json \
    --base-config base.json \
    --maps DefaultSmall,DefaultMedium \
    --max-iterations 20
```

**Time estimate:** 3 params × 3 values × 20 iterations × 2 maps × 5 min/match = ~30 hours

### GRASP Parallel (grasp_parallel.py)

**Algorithm:** GRASP with Adaptive Memory
```
For each iteration:
    Generate batch_size configs (guided by memory)
    Evaluate all in parallel
    Update memory with results
```

**Pros:**
- ✅ Fast (parallel evaluation)
- ✅ Explores parameter space widely
- ✅ Adaptive memory learns from good values
- ✅ Scales well with many CPUs

**Cons:**
- ❌ Complex algorithm
- ❌ Hard to understand what's happening
- ❌ Results harder to parse
- ❌ Requires many CPUs for best performance

**Best for:**
- Production optimization
- Large parameter spaces (>8 parameters)
- When you have many CPUs available
- When speed is critical

**Example:**
```bash
python3 src/grasp_parallel.py \
    --template template.json \
    --base-config base.json \
    --iterations 15 \
    --batch-size 8 \
    --workers 8 \
    --maps DefaultSmall,DefaultMedium
```

**Time estimate:** 15 iterations × 8 configs × 2 maps × 5 min/match ÷ 8 workers = ~15 hours

### Compare Configs (compare_configs.py)

**Purpose:** One-time comparison of two specific configurations

**Pros:**
- ✅ Simple
- ✅ Quick for single comparison
- ✅ Good for A/B testing

**Cons:**
- ❌ No optimization loop
- ❌ Manual parameter selection
- ❌ No progress tracking

**Best for:**
- Comparing two specific configs
- A/B testing
- Validating optimization results

**Example:**
```bash
python3 src/compare_configs.py \
    --paramsTeamA config_a.json \
    --paramsTeamB config_b.json \
    --maps DefaultSmall,DefaultMedium
```

## Performance Comparison

### Scenario 1: Small Parameter Space (3 parameters)

**Simple Optimizer:**
- Evaluations per iteration: 3 params × 3 values = 9
- Total evaluations (10 iterations): 90
- Time (sequential): ~7.5 hours
- Result quality: Good (local optimum)

**GRASP:**
- Evaluations per iteration: 8 (batch size)
- Total evaluations (10 iterations): 80
- Time (8 workers): ~1 hour
- Result quality: Better (explores more)

**Winner:** GRASP (if you have 8 CPUs), Simple (if limited resources)

### Scenario 2: Medium Parameter Space (6 parameters)

**Simple Optimizer:**
- Evaluations per iteration: 6 params × 3 values = 18
- Total evaluations (10 iterations): 180
- Time (sequential): ~15 hours
- Result quality: Good (local optimum)

**GRASP:**
- Evaluations per iteration: 16 (batch size)
- Total evaluations (10 iterations): 160
- Time (16 workers): ~1 hour
- Result quality: Better (explores more)

**Winner:** GRASP (much faster with many CPUs)

### Scenario 3: Learning/Understanding

**Simple Optimizer:**
- Clear what each parameter does
- See direct impact of changes
- Easy to interpret results
- Can analyze history

**GRASP:**
- Hard to see individual parameter effects
- Memory-guided exploration
- Results harder to interpret
- Checkpoint-based tracking

**Winner:** Simple Optimizer (for learning)

## Use Case Recommendations

### Use Simple Optimizer When:

1. **You're learning optimization**
   - Want to understand how parameters affect performance
   - Need clear, interpretable results
   - Want to see step-by-step progress

2. **Limited computational resources**
   - Single machine
   - No cluster access
   - Limited time on shared resources

3. **Small parameter space**
   - 3-8 parameters
   - Parameters are somewhat independent
   - Local optimum is acceptable

4. **Need clear tracking**
   - Want full history of all tests
   - Need to explain results to team
   - Want to analyze parameter importance

### Use GRASP When:

1. **Production optimization**
   - Need best possible results
   - Have time to run many evaluations
   - Can afford computational resources

2. **Many CPUs available**
   - Cluster access
   - Multi-core server
   - Can parallelize effectively

3. **Large parameter space**
   - 8+ parameters
   - Complex interactions between parameters
   - Need to explore widely

4. **Speed is critical**
   - Tight deadlines
   - Need results quickly
   - Can trade complexity for speed

### Use Compare Configs When:

1. **One-time comparison**
   - Testing two specific configurations
   - A/B testing
   - Validating optimization results

2. **Manual tuning**
   - You know what values to test
   - Small number of comparisons
   - Don't need automated optimization

## Example Workflows

### Workflow 1: Learning and Development

```bash
# 1. Start with simple optimizer to learn
python3 src/simple_optimizer.py \
    --template template.json \
    --base-config base.json \
    --maps DefaultSmall \
    --max-iterations 5

# 2. Analyze history to understand parameters
cat BC26/simple_optimizer/history.json

# 3. Refine and optimize more
python3 src/simple_optimizer.py \
    --template template_refined.json \
    --base-config BC26/simple_optimizer/best_config.json \
    --maps DefaultSmall,DefaultMedium \
    --max-iterations 10
```

### Workflow 2: Production Optimization

```bash
# 1. Quick test with simple optimizer
python3 src/simple_optimizer.py \
    --template template.json \
    --base-config base.json \
    --maps DefaultSmall \
    --max-iterations 3

# 2. Use results to inform GRASP
python3 src/grasp_parallel.py \
    --template template.json \
    --base-config BC26/simple_optimizer/best_config.json \
    --iterations 20 \
    --batch-size 16 \
    --workers 16 \
    --maps DefaultSmall,DefaultMedium

# 3. Validate best result
python3 src/compare_configs.py \
    --paramsTeamA BC26/grasp_parallel/best_config.json \
    --paramsTeamB base.json
```

### Workflow 3: Iterative Refinement

```bash
# Round 1: Wide exploration (simple)
python3 src/simple_optimizer.py \
    --template template_wide.json \
    --base-config base.json \
    --maps DefaultSmall \
    --max-iterations 10 \
    --output-dir BC26/round1

# Round 2: Narrow ranges (simple)
# Edit template to narrow ranges around best values
python3 src/simple_optimizer.py \
    --template template_narrow.json \
    --base-config BC26/round1/best_config.json \
    --maps DefaultSmall,DefaultMedium \
    --max-iterations 10 \
    --output-dir BC26/round2

# Round 3: Fine-tune (GRASP)
python3 src/grasp_parallel.py \
    --template template_fine.json \
    --base-config BC26/round2/best_config.json \
    --iterations 20 \
    --batch-size 8 \
    --workers 8 \
    --output-dir BC26/round3
```

## Conversion Guide

### From Simple to GRASP

If you started with simple_optimizer and want to switch to GRASP:

```bash
# Use simple optimizer's best config as GRASP base
cp BC26/simple_optimizer/best_config.json grasp_base.json

# Run GRASP with refined template
python3 src/grasp_parallel.py \
    --template template.json \
    --base-config grasp_base.json \
    --iterations 15 \
    --batch-size 8 \
    --workers 8
```

### From GRASP to Simple

If you want to understand GRASP results better:

```bash
# Use GRASP's best config as simple optimizer base
cp BC26/grasp_parallel/best_config.json simple_base.json

# Run simple optimizer to explore around GRASP result
python3 src/simple_optimizer.py \
    --template template_narrow.json \
    --base-config simple_base.json \
    --max-iterations 5
```

## Summary Table

| Criterion | Simple Optimizer | GRASP | Compare Configs |
|-----------|-----------------|-------|-----------------|
| **Ease of use** | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Speed** | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Result quality** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | N/A |
| **Interpretability** | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ |
| **Resource usage** | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Learning value** | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |

## Recommendation

**For most users, start with `simple_optimizer.py`:**
- Easy to understand
- Clear results
- Good for learning
- Works with limited resources

**Upgrade to `grasp_parallel.py` when:**
- You understand optimization
- You need better results
- You have computational resources
- Speed is important

**Use `compare_configs.py` for:**
- Quick one-off comparisons
- Validating results
- A/B testing

