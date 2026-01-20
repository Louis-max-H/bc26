#!/usr/bin/env python3
"""
Script pour comparer deux configurations de bots Battlecode.
Ce script est thread-safe et peut être utilisé en parallèle.
"""

import argparse
import json
import os
import subprocess
import sys
import uuid
from pathlib import Path
from typing import Dict, Any
import threading
import shutil

# Verrou global pour les opérations d'écriture de fichiers critiques
file_lock = threading.Lock()


def generate_random_id(params_file: Path) -> str:
    """Génère un ID aléatoire unique pour un joueur basé sur le nom du fichier de paramètres."""
    base_name = params_file.stem  # Obtient le nom sans extension
    return f"{base_name}_{uuid.uuid4().hex[:8]}"


def copy_bot(source: str, destination: str, project_root: Path) -> None:
    """
    Copie un bot vers un nouveau dossier.
    
    Args:
        source: Nom du dossier source
        destination: Nom du dossier de destination
        project_root: Racine du projet
    """
    print(f"Copie de {source} vers {destination}...")
    
    result = subprocess.run(
        ["python3", str(project_root / "src" / "copybot.py"), source, destination],
        cwd=str(project_root),
        capture_output=True,
        text=True
    )
    
    if result.returncode != 0:
        raise RuntimeError(f"Erreur lors de la copie du bot: {result.stderr}")


def import_params(module: str, params_file: Path, project_root: Path) -> None:
    """
    Importe les paramètres depuis un fichier JSON vers Params.java.
    
    Args:
        module: Nom du module (dossier)
        params_file: Chemin vers le fichier JSON des paramètres
        project_root: Racine du projet
    """
    print(f"Import des paramètres dans {module}...")
    
    result = subprocess.run(
        ["python3", str(project_root / "src" / "params.py"), module, "--import", str(params_file)],
        cwd=str(project_root),
        capture_output=True,
        text=True
    )
    
    if result.returncode != 0:
        raise RuntimeError(f"Erreur lors de l'import des paramètres: {result.stderr}")


def run_jinja(module: str, params_file: Path, project_root: Path) -> None:
    """
    Exécute jinja pour générer les fichiers Java à partir des templates.
    
    Args:
        module: Nom du module (dossier)
        params_file: Chemin vers le fichier JSON des paramètres
        project_root: Racine du projet
    """
    print(f"Génération des fichiers avec Jinja pour {module}...")
    
    module_path = project_root / "src" / module
    
    result = subprocess.run(
        [
            "python3", 
            str(project_root / "src" / "jinja.py"),
            str(module_path),
            "--prod",
            "--params", 
            str(params_file)
        ],
        cwd=str(project_root),
        capture_output=True,
        text=True
    )
    
    if result.returncode != 0:
        raise RuntimeError(f"Erreur lors de l'exécution de Jinja: {result.stderr}")


def compare_bots(player_a: str, player_b: str, project_root: Path, maps: str = None) -> Dict[str, Any]:
    """
    Compare deux bots et retourne les résultats.
    Affiche la sortie en temps réel tout en capturant pour le JSON final.
    
    Args:
        player_a: Nom du premier joueur
        player_b: Nom du deuxième joueur
        project_root: Racine du projet
        maps: Liste de maps séparées par des virgules (optionnel)
        
    Returns:
        Dict contenant les résultats de la comparaison
    """
    print(f"Comparaison de {player_a} vs {player_b}...")
    
    # Construire la commande avec --json pour obtenir une sortie structurée
    cmd = ["python3", str(project_root / "src" / "compare_bots.py"), player_a, player_b, "--json"]
    
    # Ajouter les maps si spécifiées
    if maps:
        cmd.extend(["--maps", maps])
        print(f"Maps: {maps}")
    else:
        print(f"Maps: toutes les maps du dossier maps/")
    
    print()  # Ligne vide pour séparer
    
    # Utiliser Popen pour capturer stdout (JSON) et stderr (messages de progression) séparément
    process = subprocess.Popen(
        cmd,
        cwd=str(project_root),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        bufsize=1  # Ligne par ligne
    )
    
    # Lire stdout et stderr en parallèle
    import threading
    
    stdout_lines = []
    stderr_lines = []
    
    def read_stderr():
        """Lire stderr et afficher en temps réel"""
        try:
            for line in process.stderr:
                print(line, end='', flush=True)  # Afficher immédiatement
                stderr_lines.append(line)
        except:
            pass
    
    def read_stdout():
        """Lire stdout (JSON) silencieusement"""
        try:
            for line in process.stdout:
                stdout_lines.append(line)
        except:
            pass
    
    # Démarrer les threads de lecture
    stderr_thread = threading.Thread(target=read_stderr)
    stdout_thread = threading.Thread(target=read_stdout)
    
    stderr_thread.start()
    stdout_thread.start()
    
    try:
        # Attendre que le processus se termine
        return_code = process.wait()
        
        # Attendre que les threads de lecture se terminent
        stderr_thread.join()
        stdout_thread.join()
    except KeyboardInterrupt:
        process.terminate()
        process.wait()
        raise
    
    if return_code != 0:
        raise RuntimeError(f"Erreur lors de la comparaison des bots (code: {return_code})")
    
    print(f"\nComparaison terminée: {player_a} vs {player_b}")
    
    # Parser le JSON retourné par compare_bots.py (stdout uniquement)
    full_output = ''.join(stdout_lines)
    try:
        results = json.loads(full_output)
        return results
    except json.JSONDecodeError as e:
        # En cas d'erreur de parsing, retourner la sortie brute
        return {
            "player_a": player_a,
            "player_b": player_b,
            "raw_output": full_output,
            "stderr": ''.join(stderr_lines),
            "parsing_error": f"Failed to parse JSON output: {str(e)}"
        }


def save_results(results: Dict[str, Any], results_file: Path) -> None:
    """
    Sauvegarde les résultats dans un fichier JSON (thread-safe).
    
    Args:
        results: Dictionnaire des résultats
        results_file: Chemin vers le fichier de sauvegarde
    """
    with file_lock:
        # Créer le dossier parent si nécessaire
        results_file.parent.mkdir(parents=True, exist_ok=True)
        
        # Charger les résultats existants s'ils existent
        existing_results = []
        if results_file.exists():
            try:
                with open(results_file, 'r', encoding='utf-8') as f:
                    existing_results = json.load(f)
                    if not isinstance(existing_results, list):
                        existing_results = [existing_results]
            except json.JSONDecodeError:
                print(f"Attention: Le fichier {results_file} existe mais n'est pas un JSON valide")
                existing_results = []
        
        # Ajouter les nouveaux résultats
        existing_results.append(results)
        
        # Sauvegarder
        with open(results_file, 'w', encoding='utf-8') as f:
            json.dump(existing_results, f, indent=2, ensure_ascii=False)
        
        print(f"Résultats sauvegardés dans {results_file}")


def save_config(config: Dict[str, Any], config_file: Path) -> None:
    """
    Sauvegarde la configuration dans un fichier JSON (thread-safe).
    
    Args:
        config: Dictionnaire de configuration
        config_file: Chemin vers le fichier de sauvegarde
    """
    with file_lock:
        # Créer le dossier parent si nécessaire
        config_file.parent.mkdir(parents=True, exist_ok=True)
        
        # Sauvegarder
        with open(config_file, 'w', encoding='utf-8') as f:
            json.dump(config, f, indent=2, ensure_ascii=False)
        
        print(f"Configuration sauvegardée dans {config_file}")


def cleanup_bot(module: str, project_root: Path) -> None:
    """
    Nettoie le dossier d'un bot temporaire.
    
    Args:
        module: Nom du module à nettoyer
        project_root: Racine du projet
    """
    bot_path = project_root / "src" / module
    # Vérifier si le module contient un UUID (indique un bot temporaire)
    if bot_path.exists() and "_" in module and any(c in module.split("_")[-1] for c in "0123456789abcdef"):
        print(f"Nettoyage de {module}...")
        shutil.rmtree(bot_path)
        print(f"{module} supprimé")


def main():
    parser = argparse.ArgumentParser(
        description="Compare deux configurations de bots Battlecode"
    )
    
    parser.add_argument(
        "--paramsTeamA",
        required=True,
        metavar="FILE",
        help="Fichier JSON de configuration pour l'équipe A"
    )
    
    parser.add_argument(
        "--paramsTeamB",
        required=True,
        metavar="FILE",
        help="Fichier JSON de configuration pour l'équipe B"
    )
    
    parser.add_argument(
        "--source",
        default="current",
        help="Dossier source à copier (défaut: current)"
    )
    
    parser.add_argument(
        "--no-cleanup",
        action="store_true",
        help="Ne pas supprimer les dossiers temporaires après la comparaison"
    )
    
    parser.add_argument(
        "--output-dir",
        default="BC26/grasp",
        help="Dossier de sortie pour les résultats (défaut: BC26/grasp)"
    )
    
    parser.add_argument(
        "--maps",
        metavar="MAP1,MAP2,...",
        help="Liste de maps séparées par des virgules (ex: DefaultSmall,DefaultMedium). "
             "Si non spécifié, utilise toutes les maps du dossier maps/"
    )
    
    args = parser.parse_args()
    
    # Résoudre les chemins
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    
    params_team_a = Path(args.paramsTeamA).resolve()
    params_team_b = Path(args.paramsTeamB).resolve()
    
    if not params_team_a.exists():
        print(f"❌ Erreur: Le fichier {params_team_a} n'existe pas")
        sys.exit(1)
    
    if not params_team_b.exists():
        print(f"❌ Erreur: Le fichier {params_team_b} n'existe pas")
        sys.exit(1)
    
    # Créer les dossiers de sortie
    output_dir = Path(args.output_dir).resolve()
    results_dir = output_dir / "results"
    bots_dir = output_dir / "bots"
    
    results_dir.mkdir(parents=True, exist_ok=True)
    bots_dir.mkdir(parents=True, exist_ok=True)
    
    # Générer les IDs aléatoires
    player_id_a = generate_random_id(params_team_a)
    player_id_b = generate_random_id(params_team_b)
    
    print(f"\n{'='*60}")
    print(f"Comparaison de configurations")
    print(f"{'='*60}")
    print(f"Source: {args.source}")
    print(f"Team A: {player_id_a} (config: {params_team_a.name})")
    print(f"Team B: {player_id_b} (config: {params_team_b.name})")
    print(f"{'='*60}\n")
    
    try:
        # Étape 1: Copier les bots
        print("\nÉtape 1/5: Copie des bots")
        print("-" * 60)
        copy_bot(args.source, player_id_a, project_root)
        copy_bot(args.source, player_id_b, project_root)
        
        # Étape 2: Importer les paramètres
        print("\nÉtape 2/5: Import des paramètres")
        print("-" * 60)
        import_params(player_id_a, params_team_a, project_root)
        import_params(player_id_b, params_team_b, project_root)
        
        # Étape 3: Générer les fichiers avec Jinja
        print("\nÉtape 3/5: Génération des fichiers")
        print("-" * 60)
        run_jinja(player_id_a, params_team_a, project_root)
        run_jinja(player_id_b, params_team_b, project_root)
        
        # Étape 4: Comparer les bots
        print("\nÉtape 4/5: Comparaison des bots")
        print("-" * 60)
        comparison_results = compare_bots(player_id_a, player_id_b, project_root, args.maps)
        
        # Afficher les résultats avant sauvegarde
        print("\n" + "="*60)
        print("RÉSULTATS DE LA COMPARAISON")
        print("="*60)
        
        # Si c'est une sortie brute, l'afficher directement
        if "raw_output" in comparison_results:
            print(comparison_results["raw_output"])
        else:
            # Sinon, afficher le JSON structuré
            print(json.dumps(comparison_results, indent=2, ensure_ascii=False))
        
        print("="*60 + "\n")
        
        # Étape 5: Sauvegarder les résultats
        print("\nÉtape 5/5: Sauvegarde des résultats")
        print("-" * 60)
        
        # Charger les configs
        with open(params_team_a, 'r', encoding='utf-8') as f:
            config_a = json.load(f)
        
        with open(params_team_b, 'r', encoding='utf-8') as f:
            config_b = json.load(f)
        
        # Sauvegarder les résultats
        results = {
            "player_id_a": player_id_a,
            "player_id_b": player_id_b,
            "config_a_file": str(params_team_a),
            "config_b_file": str(params_team_b),
            "comparison": comparison_results
        }
        
        results_file = results_dir / "results.json"
        save_results(results, results_file)
        
        # Sauvegarder les configs
        config_a_file = bots_dir / f"{player_id_a}.json"
        config_b_file = bots_dir / f"{player_id_b}.json"
        
        save_config(config_a, config_a_file)
        save_config(config_b, config_b_file)
        
        print(f"\n{'='*60}")
        print(f"Comparaison terminée avec succès!")
        print(f"{'='*60}")
        print(f"Résultats: {results_file}")
        print(f"Config Team A: {config_a_file}")
        print(f"Config Team B: {config_b_file}")
        print(f"{'='*60}\n")
        
    except Exception as e:
        print(f"\nErreur: {e}", file=sys.stderr)
        sys.exit(1)
    
    finally:
        # Nettoyage des dossiers temporaires
        if not args.no_cleanup:
            print(f"\nNettoyage des dossiers temporaires...")
            cleanup_bot(player_id_a, project_root)
            cleanup_bot(player_id_b, project_root)
            print(f"Nettoyage terminé")


if __name__ == "__main__":
    main()

