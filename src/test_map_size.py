#!/usr/bin/env python3
import subprocess
import os
import re
import json

def get_map_files():
    """Récupère tous les fichiers .map26 du répertoire maps/"""
    maps_dir = "/home/lmx/bc26/maps"
    map_files = []
    
    for file in os.listdir(maps_dir):
        if file.endswith('.map26'):
            # Enlever l'extension .map26
            map_name = file.split('.')[0]
            map_files.append(map_name)
    
    return sorted(map_files)

def run_gradlew_for_map(map_name):
    """Exécute gradlew pour une map et retourne la sortie"""
    cmd = [
        "./gradlew", "run",
        f"-Pmaps={map_name}",
        "-PteamB=testMapSize",
        "-PteamA=testMapSize",
        "-PlanguageA=java",
        "-PlanguageB=java"
    ]
    
    try:
        result = subprocess.run(
            cmd,
            cwd="/home/lmx/bc26",
            capture_output=True,
            text=True,
            timeout=30
        )
        return result.stdout + result.stderr
    except subprocess.TimeoutExpired:
        print(f"Timeout pour la map {map_name}")
        return ""
    except Exception as e:
        print(f"Erreur pour la map {map_name}: {e}")
        return ""

def extract_map_type(output):
    """Extrait le type de map depuis la sortie de gradlew"""
    # Chercher les lignes contenant "MAPTYPE:"
    pattern = r'MAPTYPE:\s*(MAP_SMALL|MAP_MEDIUM|MAP_LARGE)'
    matches = re.findall(pattern, output)
    
    if matches:
        # Retourner le premier type trouvé (normalement ils sont tous identiques)
        return matches[0]
    return None

def main():
    print("Récupération de la liste des maps...")
    map_files = get_map_files()
    print(f"Trouvé {len(map_files)} maps: {', '.join(map_files)}\n")
    
    # Dictionnaire pour stocker les résultats
    results = {
        "MAP_SMALL": [],
        "MAP_MEDIUM": [],
        "MAP_LARGE": [],
        "ALL": []
    }
    
    # Tester chaque map
    for i, map_name in enumerate(map_files, 1):
        print(f"[{i}/{len(map_files)}] Test de la map: {map_name}...", end=" ", flush=True)
        
        output = run_gradlew_for_map(map_name)
        map_type = extract_map_type(output)
        
        if map_type:
            print(f"✓ {map_type}")
            results[map_type].append(map_name)
            results["ALL"].append(map_name)
        else:
            print("✗ Type non détecté")
            print("\n" + "="*60)
            print(f"OUTPUT COMPLET POUR {map_name}:")
            print("="*60)
            print(output)
            print("="*60 + "\n")
    
    # Sauvegarder les résultats en JSON
    output_file = "/home/lmx/bc26/map_sizes.json"
    with open(output_file, 'w') as f:
        json.dump(results, f, indent=2)
    
    print(f"\n{'='*60}")
    print("RÉSULTATS:")
    print(f"{'='*60}")
    print(f"Maps SMALL:  {len(results['MAP_SMALL'])} maps")
    print(f"Maps MEDIUM: {len(results['MAP_MEDIUM'])} maps")
    print(f"Maps LARGE:  {len(results['MAP_LARGE'])} maps")
    print(f"Total:       {len(results['ALL'])} maps")
    print(f"\nRésultats sauvegardés dans: {output_file}")
    print(f"\nContenu du JSON:")
    print(json.dumps(results, indent=2))

if __name__ == "__main__":
    main()

