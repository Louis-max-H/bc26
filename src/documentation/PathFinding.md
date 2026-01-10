# Introduction
## Déplacement, mouvement
Cette année nous avons deux composantes à prendre en compte : Les déplacements et l'orientation.
Nous allons donc avoir un score pour :
- Si je me tourne dans cette direction, es-ce que je peux voir telle cellule que je n'ai pas vu depuis longtemps
- Si je me déplace sur cette cellule, es-ce que je suis à la portée de telle action ou autre

Les deux doivent pouvoir être pris en compte :
- Impossible d'attaquer une autre unité si je ne peux pas la voir
- Ça ne sert à rien de reculer pour être sur de la voir si je suis ensuite hors de portée pour l'attaquer

## Automate
Les meilleurs équipes de battlecode sont aussi celles qui réussisses à "regrouper" les actions.
Par example, admettons que je doive fuir un chat, un algorithme classique consistera à bouger dans la direction opposée, mais il serait peux être plus judicieux de bouger vers des alliées pour l'attaquer en groupe.

Mais cette action de déplacement vers les alliés peux aussi être utilisé pour les éviter et ainsi explorer la ou personne ne se trouve.

Ainsi, plusieurs heuristiques peuvent se cummuller et s'utiliser dans différents états. Pour ce faire, nous allons les implenter dans la class PathFinding qui pourra être utilisé par tous.


# Orientation de la vision
## Example
Nous allons utiliser une classe `VisionUtils.java` pour gérer la vision et `PathFinding.java` pour se déplace.
Pour ce faire, voici quelques fonction utiles :
```java
// Add a score to a location. When looking to this location, the unit will have a bonus of score.
VisionUtils.setScore(MapLocation loc, char score)

// Return the sum of all score the unit can view from loc, with orientation dir for unit of type rc.getType()
VisionUtils.getScoreInView(MapLocation loc, Direction dir, rc.getType())
```

## Aspect technique

Pratique non ? Mais attention a certains aspect :
- Le score est un char (2**16 = 65536) mais ne doit pas dépasser 700. (En effet, le roi peux voir 9*9 cellules, et on obtient un maximum de 2**16/(9*9) = 801 pour ne pas owerflow)
- On utilise des chars car `"\u0000\u0000... x3600 ...\u0000".toCharArray()` permet d'allouer un tableau remplis de 0 en une dizaine de bytecodes contre `3600` si l'on fait un tableau d'entier
- Les cases on par défaut un score de 700 si elle ne sont pas explorée, (il faut surement baisser cette limite, je vous explique comment dans la prochaine partie)
- Le score sur une cellule est mis à 0 en début de tour si l'unité peux voir cette case 
- On utilise un système de score qui encourage à voir les cases qui n'ont pas déjà été vue depuis un petit bout de temps, ce score augmente de 1 tout les 5 tours. (Donc pour 2000 tours, score au max de 400)
- On peux se dire que l'on considère qu'une bonne action vaut 100 points et qu'une action à faire à tout pris 300 ? (On retombe sur la valeur par défaut de 700)

# Déplacement
## Example
### Déplacement dans la meilleur direction
Les scores pour les déplacements sont des entiers, donc possiblement négatif.\
Ces scores sont enregistré dans la classe `Explore.java` et remis à zéro en début de tour.\
On peux imaginer qu'un état rajoute un score même si il ne bouge pas, et ensuite, quand un état cherche à faire un déplacement, il bénéficiera du score de l'état précédent.

Voici un extrait de l'état `Explore.java` :

```java
// To explore, the score for a direction is the score of the view in this direction 
int[] scores = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
MapLocation myLoc = rc.getLocation();
for(Direction dir : Direction.values()){
    if(dir != Direction.CENTER){
        scores[dir.ordinal()] = VisionUtils.getScoreInView(myLoc.add(dir), dir, rc.getType());
    }
}

// We add the score without normalization.
PathFinding.addScoresWithoutNormalization(
        scores,
        10 // Coefficient
);

// Take the best direction
Direction bestDir = PathFinding.bestDir();
rc.turn(bestDir);
if(PathFinding.move(bestDir).notOk()){
    print("Can't move to best direction.");
}
```

### Déplacement vers une cible
Et si je veux me déplacer vers un endroit précis ?
```java
// TODOS: Use Bugnav to get best direction to loc
public static Direction directionTo(MapLocation loc){
    return Robot.rc.getLocation().directionTo(loc);
}

public static Result moveTo(MapLocation loc) throws GameActionException {
    return move(directionTo(loc));
}

public static Result move(Direction dir) throws GameActionException {
    RobotController rc = Robot.rc;
    if(rc.canMove(dir)){
        Robot.lastLocation = rc.getLocation();
        Robot.lastDirection = dir;

        rc.move(dir);
        return new Result(OK, "Moved to " + dir.toString());
    } else {
        return new Result(CANT, "Can't move to " + dir.toString());
    }
}

// Move to best direction
public static Result moveBestDir() throws GameActionException {
    return move(bestDir());
}
```

### Déplacement malin

Bon ... Il faut que je fasse un algo (bugnav) pour pouvoir déterminer la meilleur direction vers une distination...\
Mais il y a un problème non ?\
Pourquoi faire plein de score si finalement, on ne les utilises pas ?\

Si jamais vous voulez vous déplacer dans une direction de manière maline, vous pouvez faire :
```java
PathFinding.modificatorOrientation(
    PathFinding.directionTo(target)
);
Pathfinding.moveBestDir();
```

La fonction, modificator orientation s'occupe de diviser drastiquement les scores des cases qui ne sont pas dans la direction souhaitée, mais ce qui permet tout de même de les prendre en compte !
```java
public static void modificatorOrientation(Direction dir){
    // Boost score toward direction. Set to 0 if not toward.
    scores[dir.rotateLeft().ordinal()] /= 2;
    scores[dir.ordinal()] *= 1;
    scores[dir.rotateRight().ordinal()] /= 2;

    Direction opposite = dir.opposite();
    scores[opposite.rotateLeft().rotateLeft().ordinal()] /= 100;
    scores[opposite.rotateLeft().ordinal()] /= 50;
    scores[opposite.ordinal()] /= 150;
    scores[opposite.rotateRight().ordinal()] /= 50;
    scores[opposite.rotateRight().rotateRight().ordinal()] /= 100;
    scores[Direction.CENTER.ordinal()] = 0;
}
```


## Aspect technique
Pour `addScoresWithoutNormalization()` ou `addScoresWithoutNormalization()` ???\
Pour pouvoir comparer des choses sur une même base ! Imaginez un malus de -100 sur une case car nous serions à la portée de l'adversaire. On cherche ensuit à comparer cette valeurs à une heuristique qui pour chaque case, nous indique la distance vers une autre case.\
Dans notre cas, l'heuristique nous donne un score de 150 sur toutes les cases, on aurait un score positif au final, donc on se déplace quand même. Alors qu'un score de 15 sur toutes les cases pour cette heuristique nous aurait un score final négatif, on ne se déplace pas.

Les scores peuvent donc être normalisé sur `100_000`, le score maximum valant 100_000 en utilisant la normalization.\
Si vous ne voulez pas normaliser vos scores (par example malus en fonction du nombre de dégat), pensez bien à faire en sorte qu'un score de `1` soit en faite un score de `100_000`

Vous pouvez utiliser un coefficient pour que les heuristique, même normalisée, ait un score plus important que d'autres.

Nous avons un overflow, si nous utilisons plus de `100` scores, chacuns avec une valeurs de `100_000` (noramlisé), et une coefficient de `200`. Donc normalement, on est large.

# Pourquoi cette stratégie va nous faire monter le classement ?
Allez, c'est le moment de se jetter des fleurs (expression française, je ne sait pas si elle sera idiomatique en anglais)
- Car on a réussit à découpler heuristique vision / déplacement
- Car on utilise Jinja pour avoir un `VisionUtils` super rapide
- Car on va faire des déplacements multi-heuristique
- Car tout les paramètres de coefficients pourrons être optimisés en lançant des milliers de matchs sur des serveurs de tests, je vais surement faire ça prochainement

