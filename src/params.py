#!/usr/bin/env python3
import argparse
import json
import re
import os
from pathlib import Path


def parse_params_java(java_file_path):
    """
    Parse le fichier Params.java et extrait les paramètres commençant par PARAMS_
    avec leurs valeurs et contraintes min/max.
    
    Returns:
        dict: Dictionnaire avec les paramètres {nom: {value, min, max}}
    """
    params = {}
    
    with open(java_file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Pattern pour matcher les lignes de paramètres
    # Ex: public static int PARAMS_aggressivitySmall1 = 0; // [1, 100]
    # Ex: public static int PARAMS_DANGER_OUT_OF_ENEMY_VIEW      = -6; // [-30, -1]
    pattern = r'public\s+static\s+int\s+(PARAMS_\w+)\s*=\s*(-?\d+)\s*;\s*//\s*\[(-?\d+),\s*(-?\d+)\]'
    
    for match in re.finditer(pattern, content):
        param_name = match.group(1)
        param_value = int(match.group(2))
        param_min = int(match.group(3))
        param_max = int(match.group(4))
        
        params[param_name] = {
            'value': param_value,
            'min': param_min,
            'max': param_max
        }
    
    return params


def export_params_to_json(java_file_path, json_file_path):
    """
    Exporte les paramètres du fichier Java vers un fichier JSON.
    """
    params = parse_params_java(java_file_path)
    
    if not params:
        print("⚠️  Aucun paramètre trouvé dans le fichier Java")
        return
    
    # Créer le répertoire de destination si nécessaire
    output_dir = os.path.dirname(json_file_path)
    if output_dir and not os.path.exists(output_dir):
        os.makedirs(output_dir, exist_ok=True)
    
    with open(json_file_path, 'w', encoding='utf-8') as f:
        json.dump(params, f, indent=2)
    
    print(f"✅ {len(params)} paramètre(s) exporté(s) vers {json_file_path}")
    print(f"   Paramètres exportés : {', '.join(params.keys())}")


def import_params_from_json(json_file_path, java_file_path):
    """
    Importe les paramètres depuis un fichier JSON et met à jour le fichier Java.
    """
    # Charger le JSON
    with open(json_file_path, 'r', encoding='utf-8') as f:
        params = json.load(f)
    
    if not params:
        print("⚠️  Aucun paramètre trouvé dans le fichier JSON")
        return
    
    # Lire le fichier Java
    with open(java_file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Pour chaque paramètre dans le JSON, mettre à jour la valeur dans le Java
    updated_count = 0
    for param_name, param_data in params.items():
        new_value = param_data['value']
        param_min = param_data['min']
        param_max = param_data['max']
        
        # Valider que la valeur est dans les limites
        if new_value < param_min or new_value > param_max:
            print(f"⚠️  Attention: {param_name} = {new_value} est hors limites [{param_min}, {param_max}]")
        
        # Pattern pour trouver et remplacer la ligne (gérer les espaces multiples)
        pattern = rf'(public\s+static\s+int\s+{re.escape(param_name)}\s+=\s+)(-?\d+)(\s*;\s*//\s*\[{re.escape(str(param_min))},\s*{re.escape(str(param_max))}\])'
        
        def replacer(match):
            return f"{match.group(1)}{new_value}{match.group(3)}"
        
        new_content = re.sub(pattern, replacer, content)
        
        if new_content != content:
            updated_count += 1
            content = new_content
    
    # Écrire le fichier Java mis à jour
    with open(java_file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"✅ {updated_count} paramètre(s) mis à jour dans {java_file_path}")


def load_params_for_jinja(json_file_path):
    """
    Charge les paramètres depuis un fichier JSON et retourne un dictionnaire
    simple {nom: valeur} pour utilisation dans Jinja.
    
    Returns:
        dict: Dictionnaire {nom_param: valeur}
    """
    with open(json_file_path, 'r', encoding='utf-8') as f:
        params = json.load(f)
    
    # Extraire seulement les valeurs
    return {name: data['value'] for name, data in params.items()}


def main():
    parser = argparse.ArgumentParser(
        description="Gestion des paramètres du fichier Params.java"
    )
    
    parser.add_argument(
        "module",
        help="Nom du module (dossier) contenant Params.java (ex: current, v000_demo, tmpXXXX)"
    )
    
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument(
        "--export",
        metavar="FILE",
        help="Exporter les paramètres de Params.java vers un fichier JSON"
    )
    group.add_argument(
        "--import",
        metavar="FILE",
        dest="import_file",
        help="Importer les paramètres depuis un fichier JSON vers Params.java"
    )
    
    args = parser.parse_args()
    
    # Construire le chemin vers le fichier Params.java basé sur le module
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    java_file = project_root / "src" / args.module / "Params.java"
    
    if not java_file.exists():
        print(f"❌ Erreur: Le fichier Java '{java_file}' n'existe pas")
        return
    
    if args.export:
        json_file = Path(args.export)
        if not json_file.is_absolute():
            json_file = Path.cwd() / json_file
        export_params_to_json(str(java_file), str(json_file))
    
    elif args.import_file:
        json_file = Path(args.import_file)
        if not json_file.is_absolute():
            json_file = Path.cwd() / json_file
        
        if not json_file.exists():
            print(f"❌ Erreur: Le fichier JSON '{json_file}' n'existe pas")
            return
        
        import_params_from_json(str(json_file), str(java_file))


if __name__ == "__main__":
    main()

