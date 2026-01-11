# Strategy Notes

## Current Issues Identified

### Problem: Too Many Rat Traps, Not Enough Cheese
- **Issue**: Putting down too many rat traps
- **Result**: Dying because of insufficient cheese
- **Root Cause**: Spending too much money on rat traps instead of collecting cheese

### Resource Management
- **Need**: Always maintain enough cheese for 200 rounds
- **Rat King Upkeep**: 2 cheese per round per rat king
- **Calculation**: 
  - 1 rat king = 2 cheese/round × 200 rounds = 400 cheese minimum
  - 5 rat kings = 10 cheese/round × 200 rounds = 2000 cheese minimum
  - Plus need buffer for spawning rats, emergencies

## Strategy Adjustments Needed

### 1. Reduce Rat Trap Placement
- **Current**: Placing too many rat traps
- **New Strategy**: Be very conservative with rat traps
- **Cost**: 30 cheese + 15 action cooldown per trap
- **Max**: 25 rat traps per team
- **Decision**: Only place traps in critical defensive positions, not everywhere

### 2. Increase Cheese Collection (Mining)
- **Priority**: Get more cheese rather than spend on traps
- **Action**: Need more miners (baby rats collecting cheese)
- **Ratio**: More miners, fewer guards
- **Focus**: Cheese collection should be primary economic activity

### 3. Convert Rats to Miners
- **Action**: Convert other rats (guards, etc.) to miners
- **Goal**: Maximize cheese collection rate
- **Trade-off**: Less defense, more economy

### 4. Conservative Cheese Management
- **Rule**: Always maintain 200 rounds worth of cheese
- **Calculation per rat king**:
  - 1 rat king: 400 cheese minimum
  - 2 rat kings: 800 cheese minimum
  - 3 rat kings: 1200 cheese minimum
  - 4 rat kings: 1600 cheese minimum
  - 5 rat kings: 2000 cheese minimum
- **Buffer**: Add extra buffer for spawning and emergencies
- **Spending Rules**:
  - Don't spend if it would drop below 200-round reserve
  - Prioritize cheese collection over spending
  - Only spend on essential things (spawning rats, critical traps)

## Implementation Ideas

### Cheese Reserve Check
```java
// Calculate minimum cheese needed for 200 rounds
int numRatKings = countRatKings();
int minCheeseReserve = numRatKings * 2 * 200; // 2 cheese per round per king
int currentCheese = rc.getGlobalCheese();

// Only spend if we have enough reserve
if (currentCheese - cost < minCheeseReserve) {
    // Don't spend, prioritize cheese collection
}
```

### Rat Role Assignment
- **Miners**: Baby rats assigned to collect cheese
- **Guards**: Fewer guards, only for critical defense
- **Ratio**: Maybe 70% miners, 30% guards (adjust based on game state)

### Trap Placement Logic
- Only place traps when:
  1. We have enough cheese reserve (200 rounds)
  2. Trap is in critical defensive position
  3. Enemy is actively threatening
- Don't place traps:
  - Early game (need economy)
  - When cheese is low
  - In non-critical areas

### Spawning Strategy
- Spawn baby rats for mining when:
  - We have cheese reserve
  - Need more cheese collection
- Spawn fewer guards/defensive units
- Focus on economic growth

## Priority Order

1. **Maintain Cheese Reserve** (200 rounds worth)
2. **Collect Cheese** (maximize miners)
3. **Spawn Rats** (for mining, not defense)
4. **Essential Defense** (only when necessary)
5. **Traps** (last priority, very conservative)

## Metrics to Track

- Current cheese vs. minimum reserve
- Number of miners vs. guards
- Cheese collection rate
- Rat trap count
- Rat king count (affects reserve calculation)
