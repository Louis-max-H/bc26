#!/usr/bin/env python3
"""
GRASP Parallélisé pour optimisation de paramètres Battlecode.

Cette version exploite pleinement le multithread en évaluant plusieurs
configurations en parallèle à chaque itération.
"""

import argparse
import json
import random
import time
import uuid
import os
from collections import defaultdict
from datetime import datetime
from multiprocessing import Pool, Manager, Lock, cpu_count
from pathlib import Path
from typing import Dict, List, Tuple, Any, Optional
import subprocess
import copy
import numpy as np

# Tenter d'importer tqdm pour les barres de progression
try:
    from tqdm import tqdm
    TQDM_AVAILABLE = True
except ImportError:
    TQDM_AVAILABLE = False
    print("⚠️  tqdm non disponible. Installer avec: pip install tqdm")


class SharedMemory:
    """Mémoire partagée thread-safe pour GRASP parallélisé."""
    
    def __init__(self, manager):
        self.lock = manager.Lock()
        self.best_solutions = manager.list()
        self.param_values = manager.dict()
        self.iteration_history = manager.list()
        self.best_score = manager.Value('d', float('-inf'))
        self.best_config = manager.dict()
    
    def add_solution(self, config: Dict, score: float):
        """Ajoute une solution de manière thread-safe."""
        with self.lock:
            self.best_solutions.append((copy.deepcopy(config), score))
            
            # Garder seulement les 100 meilleures
            sorted_solutions = sorted(list(self.best_solutions), key=lambda x: x[1], reverse=True)
            self.best_solutions[:] = sorted_solutions[:100]
            
            # Mettre à jour param_values
            for param_name, param_data in config.items():
                value = param_data['value']
                if param_name not in self.param_values:
                    self.param_values[param_name] = manager.list()
                
                param_list = self.param_values[param_name]
                param_list.append((value, score))
                
                # Garder seulement les 200 dernières
                if len(param_list) > 200:
                    self.param_values[param_name] = manager.list(list(param_list)[-200:])
            
            # Mettre à jour le meilleur global
            if score > self.best_score.value:
                self.best_score.value = score
                self.best_config.clear()
                self.best_config.update(copy.deepcopy(config))
    
    def get_good_value(self, param_name: str, param_min: int, param_max: int, alpha: float = 0.3) -> int:
        """Retourne une valeur guidée par la mémoire (thread-safe)."""
        with self.lock:
            if param_name not in self.param_values or len(self.param_values[param_name]) == 0:
                return random.randint(param_min, param_max)
            
            if random.random() < alpha:
                return random.randint(param_min, param_max)
            
            values_scores = list(self.param_values[param_name])
            
            # Calculer les poids
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
            
            return max(param_min, min(param_max, value))
    
    def get_stats(self) -> Dict:
        """Retourne les statistiques actuelles."""
        with self.lock:
            return {
                'best_score': self.best_score.value,
                'n_solutions': len(self.best_solutions),
                'n_params_tracked': len(self.param_values)
            }


def worker_evaluate(args):
    """
    Fonction worker pour évaluer une configuration.
    Conçue pour être exécutée en parallèle.
    """
    (config_id, config_a, config_b, project_root, source, maps, output_dir) = args
    
    # Sauvegarder les configs temporaires
    configs_dir = output_dir / "configs"
    configs_dir.mkdir(parents=True, exist_ok=True)
    
    temp_id = f"{config_id}_{uuid.uuid4().hex[:6]}"
    config_a_path = configs_dir / f"temp_a_{temp_id}.json"
    config_b_path = configs_dir / f"temp_b_{temp_id}.json"
    
    with open(config_a_path, 'w') as f:
        json.dump(config_a, f, indent=2)
    
    with open(config_b_path, 'w') as f:
        json.dump(config_b, f, indent=2)
    
    try:
        # Construire la commande
        cmd = [
            "python3",
            str(project_root / "src" / "compare_configs.py"),
            "--paramsTeamA", str(config_a_path),
            "--paramsTeamB", str(config_b_path),
            "--source", source,
            "--output-dir", str(output_dir / "temp_results")
        ]
        
        if maps:
            cmd.extend(["--maps", maps])
        
        # Exécuter la comparaison
        result = subprocess.run(
            cmd,
            cwd=str(project_root),
            capture_output=True,
            text=True,
            timeout=3600
        )
        
        if result.returncode != 0:
            return (config_id, config_a, 0.0, f"Error: {result.stderr[:100]}")
        
        # Parser le résultat
        score = parse_comparison_result(result.stdout)
        return (config_id, config_a, score, "Success")
        
    except subprocess.TimeoutExpired:
        return (config_id, config_a, 0.0, "Timeout")
    except Exception as e:
        return (config_id, config_a, 0.0, f"Exception: {str(e)}")
    finally:
        # Nettoyer
        config_a_path.unlink(missing_ok=True)
        config_b_path.unlink(missing_ok=True)


def parse_comparison_result(output: str) -> float:
    """Parse la sortie de compare_bots.py pour extraire le win rate."""
    for line in output.split('\n'):
        if 'Win rate' in line and '%' in line:
            try:
                percent_str = line.split('%')[0].split()[-1]
                return float(percent_str)
            except:
                pass
    return 50.0


def generate_config(template_config: Dict, shared_memory: SharedMemory, alpha: float) -> Dict:
    """Génère une configuration guidée par la mémoire partagée."""
    config = {}
    for param_name, param_data in template_config.items():
        param_min = param_data['min']
        param_max = param_data['max']
        
        value = shared_memory.get_good_value(param_name, param_min, param_max, alpha)
        
        config[param_name] = {
            'value': value,
            'min': param_min,
            'max': param_max
        }
    
    return config


def save_checkpoint(
    iteration: int,
    shared_memory: SharedMemory,
    output_dir: Path,
    template_config: Dict
):
    """Sauvegarde un checkpoint."""
    stats = shared_memory.get_stats()
    
    checkpoint = {
        'iteration': iteration,
        'best_score': stats['best_score'],
        'best_config': dict(shared_memory.best_config),
        'n_solutions_evaluated': stats['n_solutions'],
        'timestamp': datetime.now().isoformat()
    }
    
    checkpoint_path = output_dir / f"checkpoint_{iteration:04d}.json"
    with open(checkpoint_path, 'w') as f:
        json.dump(checkpoint, f, indent=2)
    
    # Sauvegarder aussi la meilleure config
    if shared_memory.best_config:
        best_config_path = output_dir / "best_config.json"
        with open(best_config_path, 'w') as f:
            json.dump(dict(shared_memory.best_config), f, indent=2)
    
    print(f"    💾 Checkpoint: iteration={iteration}, score={stats['best_score']:.2f}%")


def run_parallel_grasp(
    template_config: Dict,
    base_config: Dict,
    project_root: Path,
    output_dir: Path,
    source: str,
    maps: Optional[str],
    max_iterations: int,
    n_workers: int,
    batch_size: int,
    save_every: int,
    alpha_start: float,
    alpha_end: float
):
    """
    Exécute GRASP avec évaluations parallèles.
    
    Args:
        template_config: Configuration template
        base_config: Configuration de base (adversaire)
        project_root: Racine du projet
        output_dir: Dossier de sortie
        source: Dossier source du bot
        maps: Maps à tester
        max_iterations: Nombre d'itérations
        n_workers: Nombre de workers parallèles
        batch_size: Nombre de configs à évaluer par itération
        save_every: Fréquence de sauvegarde
        alpha_start: Alpha initial
        alpha_end: Alpha final
    """
    # Auto-detect workers si nécessaire
    if n_workers <= 0:
        n_workers = cpu_count()
        print(f"🔍 Auto-détection: {n_workers} CPUs disponibles")
    
    print(f"\n{'='*80}")
    print(f"🚀 Démarrage de GRASP Parallélisé")
    print(f"{'='*80}")
    print(f"Itérations: {max_iterations}")
    print(f"Workers: {n_workers} (CPUs disponibles: {cpu_count()})")
    print(f"Batch size: {batch_size} configs/itération")
    print(f"Évaluations totales prévues: {max_iterations * batch_size}")
    print(f"Source: {source}")
    print(f"Maps: {maps or 'toutes'}")
    print(f"Sauvegarde tous les {save_every} itérations")
    print(f"{'='*80}\n")
    
    # Initialiser la mémoire partagée
    manager = Manager()
    shared_memory = SharedMemory(manager)
    
    start_time = time.time()
    total_evaluations = 0
    
    # Pool de workers
    with Pool(processes=n_workers) as pool:
        # Barre de progression principale si tqdm disponible
        if TQDM_AVAILABLE:
            iteration_bar = tqdm(
                range(max_iterations),
                desc="GRASP Progress",
                unit="iter",
                ncols=100,
                bar_format='{l_bar}{bar}| {n_fmt}/{total_fmt} [{elapsed}<{remaining}, {rate_fmt}]'
            )
        else:
            iteration_bar = range(max_iterations)
        
        for iteration in iteration_bar:
            iter_start_time = time.time()
            
            # Calcul de alpha
            alpha = alpha_start + (alpha_end - alpha_start) * (iteration / max_iterations)
            
            if not TQDM_AVAILABLE:
                print(f"\n{'─'*80}")
                print(f"🔄 Itération {iteration + 1}/{max_iterations} (α={alpha:.3f})")
                print(f"{'─'*80}")
            
            # Phase de construction : générer batch_size configurations
            configs_to_evaluate = []
            for i in range(batch_size):
                config = generate_config(template_config, shared_memory, alpha)
                configs_to_evaluate.append(
                    (f"iter{iteration:04d}_cfg{i:02d}", config, base_config,
                     project_root, source, maps, output_dir)
                )
            
            # Phase d'évaluation parallèle avec barre de progression
            if TQDM_AVAILABLE:
                eval_bar = tqdm(
                    total=batch_size,
                    desc=f"  Eval iter {iteration+1}",
                    leave=False,
                    ncols=100,
                    bar_format='{desc}: {percentage:3.0f}%|{bar}| {n_fmt}/{total_fmt} [{elapsed}<{remaining}]'
                )
                
                results = []
                for result in pool.imap_unordered(worker_evaluate, configs_to_evaluate):
                    results.append(result)
                    eval_bar.update(1)
                    # Mettre à jour avec le score
                    if result[2] > 0:
                        eval_bar.set_postfix({'last_score': f'{result[2]:.1f}%'})
                eval_bar.close()
            else:
                print(f"  ⚡ Évaluation parallèle avec {n_workers} workers...")
                results = pool.map(worker_evaluate, configs_to_evaluate)
            
            total_evaluations += len(results)
            
            # Traiter les résultats
            scores_this_iter = []
            success_count = 0
            for config_id, config, score, status in results:
                if status == "Success":
                    shared_memory.add_solution(config, score)
                    scores_this_iter.append(score)
                    success_count += 1
                    if not TQDM_AVAILABLE:
                        print(f"     {config_id}: {score:.2f}%")
                else:
                    if not TQDM_AVAILABLE:
                        print(f"     {config_id}: ❌ {status}")
            
            # Statistiques de l'itération
            stats = shared_memory.get_stats()
            iter_time = time.time() - iter_start_time
            avg_score_iter = np.mean(scores_this_iter) if scores_this_iter else 0.0
            
            # Mise à jour de la barre de progression principale
            if TQDM_AVAILABLE:
                iteration_bar.set_postfix({
                    'best': f'{stats["best_score"]:.1f}%',
                    'avg': f'{avg_score_iter:.1f}%',
                    'workers': n_workers,
                    'eval/s': f'{batch_size/iter_time:.2f}'
                })
            else:
                print(f"  📊 Moyenne de l'itération: {avg_score_iter:.2f}%")
                print(f"  ✨ Meilleur global: {stats['best_score']:.2f}%")
                print(f"  ⚡ Performance: {batch_size/iter_time:.2f} eval/s")
            
            # Sauvegarde périodique
            if (iteration + 1) % save_every == 0 or iteration == max_iterations - 1:
                save_checkpoint(iteration + 1, shared_memory, output_dir, template_config)
            
            # Estimation du temps
            elapsed = time.time() - start_time
            avg_time_per_iter = elapsed / (iteration + 1)
            remaining_time = avg_time_per_iter * (max_iterations - iteration - 1)
            
            if not TQDM_AVAILABLE:
                print(f"  ⏱️  Temps: {elapsed/60:.1f}min | Restant: ~{remaining_time/60:.1f}min")
                print(f"  📈 Évaluations totales: {total_evaluations}")
        
        if TQDM_AVAILABLE:
            iteration_bar.close()
    
    # Résumé final
    total_time = time.time() - start_time
    stats = shared_memory.get_stats()
    
    print(f"\n{'='*80}")
    print(f"✅ Optimisation terminée!")
    print(f"{'='*80}")
    print(f"Meilleur score: {stats['best_score']:.2f}%")
    print(f"Évaluations totales: {total_evaluations}")
    print(f"Temps total: {total_time/60:.1f} minutes")
    print(f"Temps moyen/évaluation: {total_time/total_evaluations:.1f}s")
    print(f"Meilleure config: {output_dir}/best_config.json")
    print(f"{'='*80}\n")


def main():
    parser = argparse.ArgumentParser(
        description="Optimisation GRASP parallélisée pour Battlecode"
    )
    
    parser.add_argument(
        "--template",
        required=True,
        help="Fichier JSON template"
    )
    
    parser.add_argument(
        "--base-config",
        required=True,
        help="Configuration de base (adversaire)"
    )
    
    parser.add_argument(
        "--iterations",
        type=int,
        default=5,
        help="Nombre d'itérations (défaut: 5)"
    )
    
    parser.add_argument(
        "--workers",
        type=int,
        default=0,
        help="Nombre de workers parallèles (0 = auto-détection, défaut: 0)"
    )
    
    parser.add_argument(
        "--batch-size",
        type=int,
        default=2,
        help="Nombre de configs à évaluer par itération (défaut: 2)"
    )
    
    parser.add_argument(
        "--source",
        default="current",
        help="Dossier source (défaut: current)"
    )
    
    parser.add_argument(
        "--maps",
        help="Maps à tester (ex: DefaultSmall,DefaultMedium)"
    )
    
    parser.add_argument(
        "--output-dir",
        default="BC26/grasp_parallel",
        help="Dossier de sortie (défaut: BC26/grasp_parallel)"
    )
    
    parser.add_argument(
        "--save-every",
        type=int,
        default=2,
        help="Fréquence de sauvegarde (défaut: 2)"
    )
    
    parser.add_argument(
        "--alpha-start",
        type=float,
        default=0.5,
        help="Alpha initial (défaut: 0.5)"
    )
    
    parser.add_argument(
        "--alpha-end",
        type=float,
        default=0.1,
        help="Alpha final (défaut: 0.1)"
    )
    
    args = parser.parse_args()
    
    # Charger les configurations
    template_path = Path(args.template).resolve()
    base_config_path = Path(args.base_config).resolve()
    
    if not template_path.exists():
        print(f"❌ Fichier template introuvable: {template_path}")
        return 1
    
    if not base_config_path.exists():
        print(f"❌ Fichier base-config introuvable: {base_config_path}")
        return 1
    
    with open(template_path, 'r') as f:
        template_config = json.load(f)
    
    with open(base_config_path, 'r') as f:
        base_config = json.load(f)
    
    # Trouver la racine du projet
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    
    output_dir = Path(args.output_dir).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Lancer l'optimisation
    run_parallel_grasp(
        template_config=template_config,
        base_config=base_config,
        project_root=project_root,
        output_dir=output_dir,
        source=args.source,
        maps=args.maps,
        max_iterations=args.iterations,
        n_workers=args.workers,
        batch_size=args.batch_size,
        save_every=args.save_every,
        alpha_start=args.alpha_start,
        alpha_end=args.alpha_end
    )
    
    return 0


if __name__ == "__main__":
    exit(main())

