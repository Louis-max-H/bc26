# Guide GRASP pour Battlecode

## 📚 Vue d'ensemble

Ce système implémente l'algorithme **GRASP (Greedy Randomized Adaptive Search Procedure)** pour optimiser les paramètres de votre bot Battlecode.

### Deux versions disponibles :

1. **`grasp.py`** : Version séquentielle classique avec recherche locale
2. **`grasp_parallel.py`** : Version parallélisée optimisée pour clusters (RECOMMANDÉ)

## 🚀 Utilisation sur Cluster Linux

### Prérequis

```bash
# Installer les dépendances
pip install numpy tqdm
```

### 🎯 Lancement rapide sur cluster SLURM

```bash
# Test rapide (2 itérations, 1 map)
sbatch run_grasp_quick.sbatch

# Optimisation complète (5 itérations, plusieurs maps)
sbatch run_grasp.sbatch

# Vérifier le statut
squeue -u $USER

# Voir les logs en temps réel
tail -f logs/grasp_JOBID.out
```

### ⚙️ Configuration du fichier SLURM

Éditez `run_grasp.sbatch` pour ajuster :

```bash
#SBATCH --cpus-per-task=16    # Nombre de CPUs (auto-détecté par le script)
#SBATCH --mem=32G              # Mémoire RAM
#SBATCH --time=48:00:00        # Temps max (HH:MM:SS)
#SBATCH --partition=compute    # Partition du cluster

# Dans le script :
ITERATIONS=5                   # Nombre d'itérations GRASP
BATCH_SIZE=2                   # Configs évaluées par itération
MAPS="DefaultSmall,DefaultMedium"  # Maps à tester
```

### 📊 Sortie avec barres de progression

Avec `tqdm` installé, vous verrez :

```
GRASP Progress: 40%|████      | 20/50 [2:15:30<3:23:15, 0.15iter/s, best=68.5%, avg=65.2%, workers=16, eval/s=3.55]
  Eval iter 20: 100%|██████████| 8/8 [00:45<00:00, last_score=67.3%]
```

**Légende :**
- `best` : Meilleur score trouvé
- `avg` : Score moyen de l'itération
- `workers` : Nombre de workers actifs
- `eval/s` : Évaluations par seconde (performance)

## 💻 Utilisation en local

### Version parallélisée (recommandée)

```bash
python3 src/grasp_parallel.py \
  --template example_config.json \
  --base-config example_config.json \
  --iterations 5 \
  --batch-size 2 \
  --maps DefaultSmall,DefaultMedium \
  --output-dir BC26/grasp_results
```

### Version séquentielle

```bash
python3 src/grasp.py \
  --template example_config.json \
  --base-config example_config.json \
  --iterations 5 \
  --output-dir BC26/grasp_results
```

## 📖 Options détaillées

### Options communes

| Option | Description | Défaut |
|--------|-------------|--------|
| `--template` | Fichier JSON template des paramètres | Obligatoire |
| `--base-config` | Config de base (adversaire de référence) | Obligatoire |
| `--iterations` | Nombre d'itérations GRASP | 5 |
| `--source` | Dossier source du bot | `current` |
| `--maps` | Maps à tester (séparées par virgules) | Toutes |
| `--output-dir` | Dossier de sortie | `BC26/grasp` |
| `--save-every` | Fréquence de sauvegarde | 2 |
| `--alpha-start` | Alpha initial (randomisation) | 0.5 |
| `--alpha-end` | Alpha final (randomisation) | 0.1 |

### Options spécifiques à `grasp_parallel.py`

| Option | Description | Défaut |
|--------|-------------|--------|
| `--workers` | Nombre de workers (0=auto) | 0 (auto) |
| `--batch-size` | Configs par itération | 2 |

## 🧠 Fonctionnement de l'algorithme

### Phase 1 : Construction gloutonne randomisée

- Génère des configurations guidées par une **mémoire adaptative**
- Paramètre **α** (alpha) contrôle l'exploration vs exploitation :
  - α=1 : Totalement aléatoire (exploration)
  - α=0 : Totalement guidé par l'historique (exploitation)
  - α décroît linéairement de `alpha-start` à `alpha-end`

### Phase 2 : Recherche locale (version séquentielle)

- Perturbe légèrement les paramètres
- Teste les voisins
- Garde les améliorations

### Phase 3 : Mise à jour de la mémoire

- Stocke les bonnes valeurs de paramètres
- Pondère selon leur score
- Influence les prochaines constructions

### Version parallélisée

- Évalue **batch-size** configurations en parallèle
- Utilise tous les CPUs disponibles
- Mémoire partagée thread-safe

## 📁 Structure des résultats

```
BC26/grasp_results/
├── checkpoint_0003.json          # Checkpoints réguliers
├── checkpoint_0006.json
├── best_config.json               # Meilleure config trouvée
├── configs/                       # Configs temporaires
│   └── temp_*.json
└── temp_results/                  # Résultats temporaires
    └── results.json
```

### Format de `best_config.json`

```json
{
  "PARAMS_aggressivitySmall1": {
    "value": 75,
    "min": 1,
    "max": 100
  },
  ...
}
```

### Format de `checkpoint_XXXX.json`

```json
{
  "iteration": 20,
  "best_score": 68.5,
  "best_config": { ... },
  "n_solutions_evaluated": 160,
  "timestamp": "2026-01-20T12:34:56"
}
```

## 🎛️ Recommandations

### Pour optimisation rapide (test)

```bash
--iterations 2
--batch-size 2
--maps DefaultSmall
--workers 4
```

**Temps estimé :** ~30-60 minutes

### Pour optimisation standard

```bash
--iterations 5
--batch-size 2
--maps DefaultSmall,DefaultMedium
--workers 16
```

**Temps estimé :** ~2-3 heures

### Pour optimisation intensive

```bash
--iterations 20
--batch-size 5
--maps DefaultSmall,DefaultMedium,DefaultLarge
--workers 32
```

**Temps estimé :** ~10-15 heures

## 📈 Interprétation des résultats

### Score (win rate)

- **< 45%** : Configuration plus faible que la base
- **45-55%** : Configuration équivalente
- **55-65%** : Amélioration notable
- **> 65%** : Amélioration significative

### Convergence

- Surveillez `best_score` au fil des itérations
- Si plateau après 5+ itérations → convergence atteinte
- Si amélioration continue → augmenter `--iterations`

### Performance

- `eval/s` (évaluations/seconde) indique l'efficacité
- Si < 1 eval/s : simulations très longues
- Si > 5 eval/s : simulations rapides, possibilité d'augmenter `--batch-size`

## 🐛 Dépannage

### Erreur : "tqdm non disponible"

```bash
pip install tqdm
```

Le script fonctionne quand même, mais sans barres de progression.

### Erreur : "No module named 'numpy'"

```bash
pip install numpy
```

### Workers ne sont pas tous utilisés

- Vérifiez avec `htop` ou `top` pendant l'exécution
- Assurez-vous que `--batch-size` >= `--workers`
- Sur SLURM, vérifiez `#SBATCH --cpus-per-task`

### Simulations trop lentes

- Réduire le nombre de maps : `--maps DefaultSmall`
- Réduire `--batch-size`
- Utiliser des maps plus petites

### Manque de mémoire

- Réduire `--batch-size`
- Augmenter `#SBATCH --mem` sur SLURM
- Nettoyer les résultats temporaires régulièrement

## 🔬 Paramètres avancés

### Contrôle de l'exploration

```bash
# Plus d'exploration (divergent)
--alpha-start 0.8 --alpha-end 0.3

# Plus d'exploitation (convergent)
--alpha-start 0.3 --alpha-end 0.05
```

### Sauvegardes fréquentes (pour jobs instables)

```bash
--save-every 1  # Sauvegarde à chaque itération
```

### Optimisation ultra-rapide (debug)

```bash
--iterations 2 \
--batch-size 1 \
--maps DefaultSmall \
--save-every 1
```

## 📊 Analyse des résultats

### Visualiser les checkpoints

```python
import json
import matplotlib.pyplot as plt

# Charger tous les checkpoints
checkpoints = []
for i in range(1, 6):
    try:
        with open(f'BC26/grasp_results/checkpoint_{i:04d}.json') as f:
            checkpoints.append(json.load(f))
    except:
        pass

# Plot de la convergence
iterations = [c['iteration'] for c in checkpoints]
scores = [c['best_score'] for c in checkpoints]

plt.plot(iterations, scores)
plt.xlabel('Iteration')
plt.ylabel('Best Score (%)')
plt.title('GRASP Convergence')
plt.grid(True)
plt.savefig('convergence.png')
```

### Comparer plusieurs runs

```bash
# Run 1
python3 src/grasp_parallel.py ... --output-dir results/run1

# Run 2 avec paramètres différents
python3 src/grasp_parallel.py ... --output-dir results/run2

# Comparer les best_config.json
```

## 📚 Références

- [GRASP Algorithm](https://en.wikipedia.org/wiki/Greedy_randomized_adaptive_search_procedure)
- Documentation SLURM : `man sbatch`
- Battlecode 2026 : https://play.battlecode.org/bc26/

## 🤝 Support

Pour plus d'aide :
- Voir `README.md` pour la configuration générale
- Voir `COMPARE_CONFIGS_README.md` pour `compare_configs.py`
- Vérifier les logs dans `logs/grasp_*.out`

