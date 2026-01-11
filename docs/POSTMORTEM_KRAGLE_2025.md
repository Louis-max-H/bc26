# The Kragle - Battlecode 25 Postmortem

**Team**: Justin Ottesen, Andrew Bank, Matt Voynovich  
**Last Updated**: February 5, 2025

## Key Takeaways

### Development Process
- **Use existing code**: Copy and adapt pathfinding, micro, and other utilities from previous years (especially XSquare's code)
- **Don't optimize too early**: Focus on features first, optimize in weeks 2-3
- **Test everything**: Always test changes on different maps and against other players
- **Review other teams' code**: Learn from what works for others
- **Time management**: Don't spend too much time perfecting things that don't need perfection

### Strategy Insights
- **Prioritize economy**: Get a strong economy as soon as possible - games are snowbally
- **Opening theory matters**: Early game decisions have huge impact
- **Reduce idle time**: Bots doing nothing is a huge waste
- **Don't float resources**: Make sure resources are actually spendable
- **Communication is key**: Share information about battlefronts, goals, etc.

### Code Organization
- **Abstract base classes**: Use Robot base class with subclasses for each robot type
- **Utility classes**: MapData, Communication, Pathfinding, etc.
- **Goal Priority Queue**: Framework for managing multiple goals per robot
- **State machines**: Use finite state machines for robot behavior

### Common Pitfalls
- **Floating resources**: Resources stuck in towers that can't be spent
- **Idle time**: Robots traveling back to refill, waiting around
- **Not learning from others**: Copying successful strategies is a Battlecode staple
- **Over-engineering**: Spending time on perfect solutions when simple works

### Battlecode Fundamentals (Constant Across Years)

#### The Map
- Always a coordinate-grid of size 20x20 to 60x60
- Always symmetrical (reflection over x-axis, y-axis, or rotation)
- Top teams store all known map information
- Identify symmetry and extrapolate known information

#### Bytecode
- Limits around 7500-15000 per robot
- 1 bytecode ≈ 1 assembly-level instruction
- Simple BFS on 20x20 map uses entire bytecode budget
- Understanding bytecode is crucial for optimization
- Use "unrolled" loops for bytecode efficiency

#### Pathfinding
- **Binary passability**: Use BugNav (XSquare's implementation)
- **Variable passability**: Use greedy movement or unrolled Dijkstra's
- Unrolled BFS/Dijkstra for bytecode efficiency

#### Decision-Making Framework
- **Goal Priority Queue**: Each goal has type and MapLocation
- **Modular design**: shouldStartGoal(), executeGoal(), shouldStopGoal()
- Prevents robots from forgetting tasks

#### Micro/Macro
- **Micro**: Fine-control of singular robots (combat optimization)
  - XSquare micro is the gold standard
  - Movement heuristics, attack timing
- **Macro**: Army-level actions
  - Robot production, expansion, resource management
  - Use communication and Goal Priority Framework

### Git Workflow (Recommended)
1. Create branch for new feature: `git checkout -b <branch-name> origin/main`
2. Make incremental commits: `git add .` then `git commit -m "message"`
3. Rebase before merging: `git fetch` then `git rebase origin/main`
4. Push and create Pull Request: `git push -u origin <branch-name>`
5. Use "Rebase and merge" strategy on GitHub

### Testing Practices
- Run several games, closely analyze behavior changes
- Make custom maps to expose problems
- Save old versions and compete against them
- Run scrimmages against other teams
- **DO NOT upload untested bots right before deadlines**

### Resources
- **XSquare's code**: https://github.com/XSquare14 (God of Battlecode)
- **XSquare's guide**: Battlecode Guide
- **Just Woke Up's unrolled BFS generator**: Python script for generating Java code
- **Past repositories**: Check Discord #open-source

### Advice Summary
1. **Spend time preparing**: Read postmortems, look at code, make a plan
2. **Budget your time**: Focus on effort-to-result ratio
3. **Prioritize economy**: Strong economy = snowball advantage
4. **Learn from others**: Watch replays, collaborate, ask questions
5. **Stand on shoulders of giants**: Use every resource available
6. **Test your code**: Run games, analyze behavior, test against old versions
7. **Use Git effectively**: Learn command line git, use branches properly

### Their Journey
- Started: 1200-1500 rating (couldn't submit final bot)
- Sprint 1: 1533 rating, 55 seed
- Sprint 2: 1559 rating, 63 seed  
- US Qualifiers: 1730 rating, 10 seed
- Result: Lost 3-2 to 7 seed, then 3-2 to 2 seed in losers bracket
- Final: Top 12 strength but didn't make finals due to bracket luck

### Key Insights from Their Experience
- **Money towers > Paint towers** (until 4+ SRPs)
- **Don't upgrade towers** - build new ones instead
- **Opening strategy crucial**: Soldier pairs for uncontested ruins, rush if contested
- **Communication reduces idle time**: Share battlefront locations
- **Don't refill paint**: Other teams don't, saves massive idle time
- **Goal management**: Use priority queues, not single goals

### What Worked
- Copying and adapting XSquare's code
- Identifying key game-specific tasks early
- Consistent improvements to resource management
- Opening strategy (soldier pairs)
- Goal Priority Queue framework

### What Didn't Work
- Rewriting pathfinding (wasted time, should have used existing)
- SURVIVE behavior (never impacted matches)
- Messy goal resolution (should have formal framework)
- Not paying attention to other teams' strategies
- Floating resources and idle time (fixed too late)

### Tournament Structure Feedback
- Proposed reseeding when teams drop to losers bracket
- Ensures higher seeds always have better matchups
- Better ensures true top-12 strength teams qualify
