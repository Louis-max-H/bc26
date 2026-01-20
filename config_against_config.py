#!/usr/bin/env python3
"""
Script pour lancer un match entre deux configurations sur une seule map.
Affiche la sortie complète de gradlew et nettoie les ressources après.
"""

import argparse
import json
import subprocess
import sys
import uuid
import shutil
from pathlib import Path
from datetime import datetime


def generate_random_id(params_file: Path) -> str:
    """Génère un ID aléatoire unique pour un joueur basé sur le nom du fichier de paramètres."""
    base_name = params_file.stem
    return f"{base_name}_{uuid.uuid4().hex[:8]}"


def copy_bot(source: str, destination: str, project_root: Path) -> Path:
    """
    Copie un bot vers un nouveau dossier.
    
    Args:
        source: Nom du dossier source
        destination: Nom du dossier de destination
        project_root: Racine du projet
        
    Returns:
        Path: Chemin complet du bot copié
    """
    script = project_root / "src" / "copybot.py"
    print(f"Copie de {source} vers {destination}...")
    
    result = subprocess.run(
        ["python3", str(script), source, destination],
        cwd=str(project_root / "src"),
        capture_output=True,
        text=True
    )
    
    if result.returncode != 0:
        raise RuntimeError(f"Erreur lors de la copie du bot: {result.stderr}")
    
    bot_path = project_root / "src" / destination
    return bot_path


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


def run_match(player_a: str, player_b: str, map_name: str, project_root: Path) -> dict:
    """
    Lance un match entre deux joueurs sur une map donnée.
    
    Args:
        player_a: Nom du premier joueur
        player_b: Nom du deuxième joueur
        map_name: Nom de la map
        project_root: Racine du projet
        
    Returns:
        Dict contenant les résultats du match
    """
    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    replay_name = f"matches/run-{timestamp}-{player_a}-vs-{player_b}-on-{map_name}.bc26"
    
    # Retirer l'extension .map26 si présente
    map_base = map_name.split('.')[0]
    
    print(f"\n{'='*60}")
    print(f"LANCEMENT DU MATCH")
    print(f"{'='*60}")
    print(f"Joueur A: {player_a}")
    print(f"Joueur B: {player_b}")
    print(f"Map: {map_base}")
    print(f"Replay: {replay_name}")
    print(f"{'='*60}\n")
    
    # Lancer gradlew run
    process = subprocess.run(
        [
            str(project_root / "gradlew"),
            "run",
            f"-PteamA={player_a}",
            f"-PteamB={player_b}",
            f"-Pmaps={map_base}",
            f"-PlanguageA=java",
            f"-PlanguageB=java",
            f"-Preplay={replay_name}",
        ],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        cwd=str(project_root)
    )
    
    stdout = process.stdout.decode("utf-8")
    print(stdout)
    
    if process.returncode != 0:
        return {
            "success": False,
            "error": f"Gradle a échoué avec le code {process.returncode}",
            "output": stdout,
            "returncode": process.returncode
        }
    
    # Parser les résultats
    lines = stdout.splitlines()
    
    try:
        winner_line = next(line for line in lines if ") wins (" in line)
        player_a_wins = "(A)" in winner_line
        
        win_condition_line = next(line for line in lines if line.startswith("[server] Reason: "))
        win_condition = win_condition_line.split(": ", 1)[1]
        
        winner = player_a if player_a_wins else player_b
        loser = player_b if player_a_wins else player_a
        
        # Afficher le résultat
        print(f"🏆 Vainqueur: {winner}")
        print(f"❌ Perdant  : {loser}")
        print(f"📋 Condition de victoire: {win_condition}")
        print(f"🎬 Replay: {replay_name}")
        
        return {
            "success": True,
            "player_a": player_a,
            "player_b": player_b,
            "map": map_base,
            "winner": winner,
            "loser": loser,
            "player_a_wins": player_a_wins,
            "win_condition": win_condition,
            "replay_name": replay_name,
            "output": stdout,
            "returncode": 0
        }
    
    except StopIteration:
        return {
            "success": False,
            "error": "Impossible de parser les résultats du match",
            "output": stdout,
            "returncode": process.returncode
        }


def cleanup_bot(module: str, project_root: Path) -> None:
    """
    Nettoie le dossier d'un bot temporaire.
    
    Args:
        module: Nom du module à nettoyer
        project_root: Racine du projet
    """
    bot_path = project_root / "src" / module
    # Vérifier si le module contient un UUID (indique un bot temporaire)
    if bot_path.exists() and "_" in module:
        print(f"Nettoyage de {module}...")
        shutil.rmtree(bot_path)
        print(f"✅ {module} supprimé")


def main():
    parser = argparse.ArgumentParser(
        description="Lance un match entre deux configurations sur une seule map"
    )
    
    parser.add_argument(
        "config_a",
        metavar="CONFIG_A",
        help="Fichier JSON de configuration pour le joueur A"
    )
    
    parser.add_argument(
        "config_b",
        metavar="CONFIG_B",
        help="Fichier JSON de configuration pour le joueur B"
    )
    
    parser.add_argument(
        "map",
        metavar="MAP",
        help="Nom de la map (avec ou sans extension .map26)"
    )
    
    parser.add_argument(
        "--source",
        default="current",
        help="Dossier source à copier (défaut: current)"
    )
    
    parser.add_argument(
        "--no-cleanup",
        action="store_true",
        help="Ne pas supprimer les dossiers temporaires après le match"
    )
    
    parser.add_argument(
        "--output",
        metavar="FILE",
        help="Fichier JSON pour sauvegarder les résultats (optionnel)"
    )
    
    args = parser.parse_args()
    
    # Résoudre les chemins
    script_dir = Path(__file__).parent
    project_root = script_dir
    
    config_a_path = Path(args.config_a).resolve()
    config_b_path = Path(args.config_b).resolve()
    
    if not config_a_path.exists():
        print(f"❌ Erreur: Le fichier {config_a_path} n'existe pas")
        sys.exit(1)
    
    if not config_b_path.exists():
        print(f"❌ Erreur: Le fichier {config_b_path} n'existe pas")
        sys.exit(1)
    
    # Générer les IDs aléatoires
    player_id_a = generate_random_id(config_a_path)
    player_id_b = generate_random_id(config_b_path)
    
    match_result = None
    
    try:
        # Étape 1: Copier les bots
        print("Copie des bots")
        copy_bot(args.source, player_id_a, project_root)
        copy_bot(args.source, player_id_b, project_root)
        
        # Étape 2: Générer les fichiers avec Jinja
        print("Génération des fichiers")
        run_jinja(player_id_a, config_a_path, project_root)
        run_jinja(player_id_b, config_b_path, project_root)
        
        # Étape 3: Lancer le match
        print("Lancement du match")
        match_result = run_match(player_id_a, player_id_b, args.map, project_root)
        
        # Étape 4: Sauvegarder les résultats si demandé
        if args.output and match_result:
            print("Sauvegarde des résultats")
            
            output_path = Path(args.output).resolve()
            output_path.parent.mkdir(parents=True, exist_ok=True)
            
            with open(output_path, 'w', encoding='utf-8') as f:
                json.dump(match_result, f, indent=2, ensure_ascii=False)
            
            print(f"✅ Résultats sauvegardés dans {output_path}")
        
        # Déterminer le code de sortie basé sur le résultat
        if match_result and not match_result.get("success", False):
            sys.exit(1)
        
    except Exception as e:
        print(f"\n❌ Erreur: {e}", file=sys.stderr)
        sys.exit(1)
    
    finally:
        # Nettoyage des dossiers temporaires
        if not args.no_cleanup:
            print(f"\nNettoyage des dossiers temporaires...")
            print("-" * 60)
            cleanup_bot(player_id_a, project_root)
            cleanup_bot(player_id_b, project_root)
            print(f"✅ Nettoyage terminé\n")


if __name__ == "__main__":
    main()

