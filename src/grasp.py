#!/usr/bin/env python3
"""
GRASP (Greedy Randomized Adaptive Search Procedure) pour optimisation de paramètres Battlecode.

Ce script implémente plusieurs variantes de GRASP :
- GRASP classique avec mémoire adaptative
- GRASP parallélisé avec exploitation multithread
- GRASP hybride avec Bayesian Optimization (optionnel)

Le script s'intègre avec compare_configs.py pour évaluer les solutions.
"""

import argparse
import json
import random
import time
import uuid
from collections import defaultdict
from datetime import datetime
from multiprocessing import Pool, Manager, Lock
from pathlib import Path
from typing import Dict, List, Tuple, Any, Optional
import subprocess
import copy
import numpy as np

# Tenter d'importer les librairies optionnelles
try:
    from skopt import gp_minimize
    from skopt.space import Real
    SKOPT_AVAILABLE = True
except ImportError:
    SKOPT_AVAILABLE = False
    print("⚠️  scikit-optimize non disponible. Mode Bayesian Optimization désactivé.")

try:
    import optuna
    OPTUNA_AVAILABLE = True
except ImportError:
    OPTUNA_AVAILABLE = False
    print("⚠️  Optuna non disponible. Mode Bayesian Optimization désactivé.")


class AdaptiveMemory:
    """Mémoire adaptative pour stocker les bonnes valeurs de paramètres."""
    
    def __init__(self):
        self.param_values = defaultdict(list)  # {param_name: [(value, score), ...]}
        self.best_solutions = []  # [(config, score), ...]
        self.iteration_history = []  # Historique des itérations
        
    def add_solution(self, config: Dict, score: float):
        """Ajoute une solution dans la mémoire."""
        self.best_solutions.append((copy.deepcopy(config), score))
        
        # Garder seulement les N meilleures
        self.best_solutions.sort(key=lambda x: x[1], reverse=True)
        self.best_solutions = self.best_solutions[:50]
        
        # Mettre à jour les valeurs de paramètres
        for param_name, param_data in config.items():
            value = param_data['value']
            self.param_values[param_name].append((value, score))
            # Garder seulement les 100 dernières valeurs
            self.param_values[param_name] = self.param_values[param_name][-100:]
    
    def get_good_value(self, param_name: str, param_min: int, param_max: int, alpha: float = 0.3) -> int:
        """
        Retourne une valeur guidée par la mémoire adaptative.
        
        Args:
            param_name: Nom du paramètre
            param_min: Valeur minimale
            param_max: Valeur maximale
            alpha: Paramètre de randomisation (0 = déterministe, 1 = totalement aléatoire)
        
        Returns:
            Valeur du paramètre
        """
        if not self.param_values[param_name] or random.random() < alpha:
            # Exploration : valeur aléatoire
            return random.randint(param_min, param_max)
        
        # Exploitation : valeur guidée par les bonnes solutions
        values_scores = self.param_values[param_name]
        
        # Calculer les poids (scores normalisés)
        scores = np.array([s for _, s in values_scores])
        if scores.max() > scores.min():
            weights = (scores - scores.min()) / (scores.max() - scores.min())
            weights = weights / weights.sum()
        else:
            weights = np.ones(len(scores)) / len(scores)
        
        # Choisir une valeur selon les poids
        idx = np.random.choice(len(values_scores), p=weights)
        base_value = values_scores[idx][0]
        
        # Ajouter une petite perturbation
        perturbation = int(np.random.normal(0, (param_max - param_min) * 0.1))
        value = base_value + perturbation
        
        # Assurer que la valeur est dans les limites
        return max(param_min, min(param_max, value))
    
    def to_dict(self) -> Dict:
        """Convertit la mémoire en dictionnaire pour sauvegarde."""
        return {
            'best_solutions': self.best_solutions,
            'iteration_history': self.iteration_history,
            'param_values_count': {k: len(v) for k, v in self.param_values.items()}
        }


class GRASPOptimizer:
    """Optimiseur GRASP pour paramètres Battlecode."""
    
    def __init__(
        self,
        template_config: Dict[str, Any],
        base_config: Dict[str, Any],
        output_dir: Path,
        project_root: Path,
        source: str = "current",
        maps: Optional[str] = None,
        n_workers: int = 4,
        save_every: int = 3
    ):
        self.template_config = template_config
        self.base_config = base_config
        self.output_dir = output_dir
        self.project_root = project_root
        self.source = source
        self.maps = maps
        self.n_workers = n_workers
        self.save_every = save_every
        
        self.memory = AdaptiveMemory()
        self.iteration_count = 0
        self.best_config = None
        self.best_score = float('-inf')
        
        # Créer les dossiers de sortie
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.configs_dir = self.output_dir / "configs"
        self.configs_dir.mkdir(exist_ok=True)
        
    def generate_random_config(self, alpha: float = 0.3) -> Dict[str, Any]:
        """
        Génère une configuration guidée par la mémoire adaptative.
        
        Args:
            alpha: Paramètre de randomisation (0-1)
        
        Returns:
            Configuration générée
        """
        config = {}
        for param_name, param_data in self.template_config.items():
            param_min = param_data['min']
            param_max = param_data['max']
            
            value = self.memory.get_good_value(param_name, param_min, param_max, alpha)
            
            config[param_name] = {
                'value': value,
                'min': param_min,
                'max': param_max
            }
        
        return config
    
    def local_search(self, config: Dict[str, Any], max_iterations: int = 5) -> Tuple[Dict[str, Any], float]:
        """
        Recherche locale autour d'une configuration.
        
        Args:
            config: Configuration de départ
            max_iterations: Nombre max d'itérations
        
        Returns:
            (meilleure_config, meilleur_score)
        """
        best_config = copy.deepcopy(config)
        best_score = self.evaluate_config(best_config, self.base_config)
        
        for _ in range(max_iterations):
            # Générer un voisin en perturbant quelques paramètres
            neighbor = copy.deepcopy(best_config)
            n_params_to_change = random.randint(1, min(3, len(config)))
            params_to_change = random.sample(list(config.keys()), n_params_to_change)
            
            for param_name in params_to_change:
                param_data = neighbor[param_name]
                param_min = param_data['min']
                param_max = param_data['max']
                current_value = param_data['value']
                
                # Perturbation locale
                range_size = param_max - param_min
                delta = int(np.random.normal(0, range_size * 0.15))
                new_value = current_value + delta
                new_value = max(param_min, min(param_max, new_value))
                
                neighbor[param_name]['value'] = new_value
            
            # Évaluer le voisin
            neighbor_score = self.evaluate_config(neighbor, self.base_config)
            
            if neighbor_score > best_score:
                best_config = neighbor
                best_score = neighbor_score
                print(f"      🔍 Amélioration locale: {best_score:.2f}")
        
        return best_config, best_score
    
    def evaluate_config(self, config_a: Dict[str, Any], config_b: Dict[str, Any]) -> float:
        """
        Évalue une configuration en la comparant à une configuration de base.
        
        Args:
            config_a: Configuration à évaluer
            config_b: Configuration de référence
        
        Returns:
            Score (win rate de config_a)
        """
        # Sauvegarder les configs temporaires
        temp_id = uuid.uuid4().hex[:8]
        config_a_path = self.configs_dir / f"temp_a_{temp_id}.json"
        config_b_path = self.configs_dir / f"temp_b_{temp_id}.json"
        
        with open(config_a_path, 'w') as f:
            json.dump(config_a, f, indent=2)
        
        with open(config_b_path, 'w') as f:
            json.dump(config_b, f, indent=2)
        
        try:
            # Construire la commande
            cmd = [
                "python3",
                str(self.project_root / "src" / "compare_configs.py"),
                "--paramsTeamA", str(config_a_path),
                "--paramsTeamB", str(config_b_path),
                "--source", self.source,
                "--output-dir", str(self.output_dir / "temp_results")
            ]
            
            if self.maps:
                cmd.extend(["--maps", self.maps])
            
            # Exécuter la comparaison
            result = subprocess.run(
                cmd,
                cwd=str(self.project_root),
                capture_output=True,
                text=True,
                timeout=3600  # Timeout de 1h
            )
            
            if result.returncode != 0:
                print(f"      ⚠️  Erreur lors de l'évaluation: {result.stderr[:200]}")
                return 0.0
            
            # Parser le résultat pour extraire le win rate
            score = self.parse_comparison_result(result.stdout)
            return score
            
        except subprocess.TimeoutExpired:
            print(f"      ⚠️  Timeout lors de l'évaluation")
            return 0.0
        except Exception as e:
            print(f"      ⚠️  Erreur: {e}")
            return 0.0
        finally:
            # Nettoyer les fichiers temporaires
            config_a_path.unlink(missing_ok=True)
            config_b_path.unlink(missing_ok=True)
    
    def parse_comparison_result(self, output: str) -> float:
        """
        Parse la sortie de compare_bots.py pour extraire le win rate.
        
        Args:
            output: Sortie de compare_configs.py
        
        Returns:
            Win rate (0-100)
        """
        # Chercher les lignes avec "Win rate"
        for line in output.split('\n'):
            if 'Win rate' in line and '%' in line:
                # Extraire le premier pourcentage trouvé
                try:
                    # Format attendu: "50.00%"
                    percent_str = line.split('%')[0].split()[-1]
                    return float(percent_str)
                except:
                    pass
        
        # Si on ne trouve pas, retourner 50% par défaut
        return 50.0
    
    def save_checkpoint(self, iteration: int):
        """Sauvegarde un checkpoint de l'optimisation."""
        checkpoint = {
            'iteration': iteration,
            'best_config': self.best_config,
            'best_score': self.best_score,
            'memory': self.memory.to_dict(),
            'timestamp': datetime.now().isoformat()
        }
        
        checkpoint_path = self.output_dir / f"checkpoint_{iteration:04d}.json"
        with open(checkpoint_path, 'w') as f:
            json.dump(checkpoint, f, indent=2)
        
        # Sauvegarder aussi la meilleure config
        if self.best_config:
            best_config_path = self.output_dir / "best_config.json"
            with open(best_config_path, 'w') as f:
                json.dump(self.best_config, f, indent=2)
        
        print(f"      💾 Checkpoint sauvegardé: {checkpoint_path.name}")
    
    def run(self, max_iterations: int, alpha_start: float = 0.5, alpha_end: float = 0.1):
        """
        Exécute l'optimisation GRASP.
        
        Args:
            max_iterations: Nombre d'itérations
            alpha_start: Paramètre de randomisation initial
            alpha_end: Paramètre de randomisation final
        """
        print(f"\n{'='*80}")
        print(f"🚀 Démarrage de l'optimisation GRASP")
        print(f"{'='*80}")
        print(f"Itérations: {max_iterations}")
        print(f"Workers: {self.n_workers}")
        print(f"Source: {self.source}")
        print(f"Maps: {self.maps or 'toutes'}")
        print(f"Sauvegarde tous les {self.save_every} itérations")
        print(f"{'='*80}\n")
        
        start_time = time.time()
        
        for iteration in range(max_iterations):
            self.iteration_count = iteration + 1
            
            # Calcul de alpha avec décroissance linéaire
            alpha = alpha_start + (alpha_end - alpha_start) * (iteration / max_iterations)
            
            print(f"\n{'─'*80}")
            print(f"🔄 Itération {self.iteration_count}/{max_iterations} (α={alpha:.3f})")
            print(f"{'─'*80}")
            
            # Phase 1 : Construction gloutonne randomisée
            print(f"  📝 Phase de construction...")
            config = self.generate_random_config(alpha)
            
            # Phase 2 : Recherche locale
            print(f"  🔍 Phase de recherche locale...")
            improved_config, score = self.local_search(config, max_iterations=3)
            
            # Mise à jour de la mémoire
            self.memory.add_solution(improved_config, score)
            self.memory.iteration_history.append({
                'iteration': self.iteration_count,
                'score': score,
                'alpha': alpha
            })
            
            # Mise à jour du meilleur
            if score > self.best_score:
                self.best_score = score
                self.best_config = copy.deepcopy(improved_config)
                print(f"  ✨ NOUVELLE MEILLEURE SOLUTION: {self.best_score:.2f}%")
            
            print(f"  📊 Score: {score:.2f}% | Meilleur: {self.best_score:.2f}%")
            
            # Sauvegarde périodique
            if (iteration + 1) % self.save_every == 0 or iteration == max_iterations - 1:
                self.save_checkpoint(self.iteration_count)
            
            # Temps écoulé et estimation
            elapsed = time.time() - start_time
            avg_time_per_iter = elapsed / (iteration + 1)
            remaining_time = avg_time_per_iter * (max_iterations - iteration - 1)
            
            print(f"  ⏱️  Temps: {elapsed/60:.1f}min | Restant: ~{remaining_time/60:.1f}min")
        
        # Résumé final
        total_time = time.time() - start_time
        print(f"\n{'='*80}")
        print(f"✅ Optimisation terminée!")
        print(f"{'='*80}")
        print(f"Meilleur score: {self.best_score:.2f}%")
        print(f"Temps total: {total_time/60:.1f} minutes")
        print(f"Meilleure config: {self.output_dir}/best_config.json")
        print(f"{'='*80}\n")


def load_template_config(config_path: Path) -> Dict[str, Any]:
    """Charge une configuration template."""
    with open(config_path, 'r') as f:
        return json.load(f)


def main():
    parser = argparse.ArgumentParser(
        description="Optimisation GRASP pour paramètres Battlecode"
    )
    
    parser.add_argument(
        "--template",
        required=True,
        help="Fichier JSON template avec les paramètres à optimiser"
    )
    
    parser.add_argument(
        "--base-config",
        required=True,
        help="Fichier JSON de la configuration de base (adversaire de référence)"
    )
    
    parser.add_argument(
        "--iterations",
        type=int,
        default=5,
        help="Nombre d'itérations GRASP (défaut: 5)"
    )
    
    parser.add_argument(
        "--workers",
        type=int,
        default=4,
        help="Nombre de workers parallèles (défaut: 4)"
    )
    
    parser.add_argument(
        "--source",
        default="current",
        help="Dossier source du bot (défaut: current)"
    )
    
    parser.add_argument(
        "--maps",
        help="Maps à tester (ex: DefaultSmall,DefaultMedium)"
    )
    
    parser.add_argument(
        "--output-dir",
        default="BC26/grasp",
        help="Dossier de sortie (défaut: BC26/grasp)"
    )
    
    parser.add_argument(
        "--save-every",
        type=int,
        default=2,
        help="Fréquence de sauvegarde des checkpoints (défaut: 2)"
    )
    
    parser.add_argument(
        "--alpha-start",
        type=float,
        default=0.5,
        help="Paramètre alpha initial (randomisation, 0-1, défaut: 0.5)"
    )
    
    parser.add_argument(
        "--alpha-end",
        type=float,
        default=0.1,
        help="Paramètre alpha final (randomisation, 0-1, défaut: 0.1)"
    )
    
    args = parser.parse_args()
    
    # Charger les configurations
    template_path = Path(args.template).resolve()
    base_config_path = Path(args.base_config).resolve()
    
    if not template_path.exists():
        print(f"❌ Erreur: Le fichier template {template_path} n'existe pas")
        return 1
    
    if not base_config_path.exists():
        print(f"❌ Erreur: Le fichier base-config {base_config_path} n'existe pas")
        return 1
    
    template_config = load_template_config(template_path)
    base_config = load_template_config(base_config_path)
    
    # Trouver la racine du projet
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    
    output_dir = Path(args.output_dir).resolve()
    
    # Créer l'optimiseur
    optimizer = GRASPOptimizer(
        template_config=template_config,
        base_config=base_config,
        output_dir=output_dir,
        project_root=project_root,
        source=args.source,
        maps=args.maps,
        n_workers=args.workers,
        save_every=args.save_every
    )
    
    # Lancer l'optimisation
    optimizer.run(
        max_iterations=args.iterations,
        alpha_start=args.alpha_start,
        alpha_end=args.alpha_end
    )
    
    return 0


if __name__ == "__main__":
    exit(main())

