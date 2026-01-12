# Battlecode Development Tips

## Learn from Past Years

### Tutorial from Square
- **Battlecode Guide by XSquare**: https://battlecode.org/assets/files/battlecode-guide-xsquare.pdf
- Very good advice!

### Summary of Post-Mortems
- Copy past code from other years, especially **pathfinding**
- **Macro strategy**: Move toward, Attack, (next turn) Attack, Move backward → Attack 2 times when enemy can only attack once
- Use a **finite state machine**
- Use **heuristic with multifactor** when moving
- **Don't optimize before week 2 or week 3**
- **Always test your changes**, on different maps, against other players
- **Always review others' codes**, of your team or in #open-source of battlecode server
- Maybe use names like `bot_V01` for versioning

## Best Practices

### Code Organization
- Use version naming: `v01`, `v02`, etc.
- Copy and adapt pathfinding from previous years
- Use finite state machines for robot behavior

### Strategy
- Macro patterns: Move → Attack → Attack → Move back (attack twice when enemy can only attack once)
- Use multi-factor heuristics for movement decisions
- Test on different maps
- Test against other players

### Development Timeline
- **Week 1**: Get basic functionality working
- **Week 2-3**: Don't optimize yet, focus on features
- **After Week 3**: Start optimizing

### Testing
- Always test changes
- Test on different maps
- Test against other players
- Review other teams' code

### Learning Resources
- XSquare's guide: https://battlecode.org/assets/files/battlecode-guide-xsquare.pdf
- Battlecode Discord #open-source channel
- Review your team's code
- Review other teams' code
