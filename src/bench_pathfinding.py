#!/usr/bin/env python3
"""
Script pour parser les rapports de pathfinding et générer un graphique.
"""

import re
import sys
import matplotlib.pyplot as plt
import matplotlib.colors as mcolors
from pathlib import Path
from typing import List, Optional, Tuple


class PathfindingReport:
    """Classe pour stocker les données d'un rapport de pathfinding."""
    
    def __init__(self):
        self.iterations_normal = 0     # Somme des itérations normal (Normal + REVERSE)
        self.iterations_split = 0      # Somme des itérations split (Normal + REVERSE)
        self.iterations_return = 0     # Optionnel
        self.bytecode_total = 0       # Somme des bytecode (Normal + REVERSE)
        self.has_reverse = False       # Indique si un mode REVERSE a été trouvé


def parse_pathfinding_file(file_path: str) -> List[PathfindingReport]:
    """
    Parse un fichier texte pour extraire les rapports de pathfinding.
    
    Args:
        file_path: Chemin vers le fichier à parser
        
    Returns:
        Liste des rapports de pathfinding extraits
    """
    reports = []
    current_report = None
    in_normal_mode = False
    in_reverse_mode = False
    
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        
        # Détection du début d'un rapport Normal
        if '===Pathfinding report : Normal===' in line:
            # Finaliser le rapport précédent s'il existe et a un REVERSE
            if current_report is not None:
                if current_report.has_reverse and current_report.iterations_normal > 0:
                    reports.append(current_report)
                current_report = None
            # Créer un nouveau rapport pour chaque paire Normal+REVERSE
            current_report = PathfindingReport()
            in_normal_mode = True
            in_reverse_mode = False
            i += 1
            continue
        
        # Détection du début d'un rapport REVERSE
        if '===Pathfinding report : REVERSE ===' in line:
            if current_report is not None:
                # On marque qu'on a un mode REVERSE
                current_report.has_reverse = True
            else:
                # Si pas de rapport Normal avant, on crée un nouveau rapport
                current_report = PathfindingReport()
                current_report.has_reverse = True
            in_reverse_mode = True
            in_normal_mode = False
            i += 1
            continue
        
        # Extraction des données si on est dans un rapport
        if current_report is not None:
            # Iterations normal (on additionne Normal + REVERSE)
            match = re.search(r'Iterations normal\s*:\s*(\d+)', line)
            if match:
                current_report.iterations_normal += int(match.group(1))
            
            # Iterations split (on additionne Normal + REVERSE)
            match = re.search(r'Iterations split\s*:\s*(\d+)', line)
            if match:
                current_report.iterations_split += int(match.group(1))
            
            # Iterations return (optionnel)
            match = re.search(r'Iterations return\s*:\s*(\d+)', line)
            if match:
                current_report.iterations_return += int(match.group(1))
            
            # Bytecode used (on additionne Normal + REVERSE)
            match = re.search(r'Bytecode used\s*:\s*(\d+)', line)
            if match:
                current_report.bytecode_total += int(match.group(1))
            
            # Si on rencontre "Pathfinding: SUCCESS" après un REVERSE, on finalise le rapport
            if in_reverse_mode and 'Pathfinding: SUCCESS' in line:
                if current_report.has_reverse and current_report.iterations_normal > 0:
                    reports.append(current_report)
                    current_report = None
                    in_normal_mode = False
                    in_reverse_mode = False
        
        i += 1
    
    # Ajouter le dernier rapport s'il a un mode REVERSE et des iterations normal
    if current_report is not None and current_report.has_reverse and current_report.iterations_normal > 0:
        reports.append(current_report)
    
    return reports


def extract_data_for_plotting(reports: List[PathfindingReport]) -> Tuple[List[int], List[int], List[int]]:
    """
    Extrait les données pour le graphique.
    
    Args:
        reports: Liste des rapports de pathfinding
        
    Returns:
        Tuple (x_values, y_values, bytecode_values)
        - x_values: somme des itérations normal (Normal + REVERSE)
        - y_values: somme des itérations split (Normal + REVERSE)
        - bytecode_values: somme des bytecode utilisés (Normal + REVERSE)
    """
    x_values = []
    y_values = []
    bytecode_values = []
    
    for report in reports:
        if report.iterations_normal > 0:
            x_values.append(report.iterations_normal)
            y_values.append(report.iterations_split)
            bytecode_values.append(report.bytecode_total)
    
    return x_values, y_values, bytecode_values


def plot_pathfinding_results(x_values: List[int], y_values: List[int], bytecode_values: List[int], 
                             output_file: Optional[str] = None):
    """
    Crée un graphique des résultats de pathfinding.
    
    Args:
        x_values: Nombre d'itérations en mode normal (abscisse)
        y_values: Somme des itérations split (ordonnée)
        bytecode_values: Somme des bytecode utilisés (couleur)
        output_file: Nom du fichier de sortie (optionnel)
    """
    if not x_values:
        print("Aucune donnée à afficher.")
        return
    
    fig, ax = plt.subplots(figsize=(10, 8))
    
    # Normaliser les valeurs de bytecode pour la couleur
    if bytecode_values:
        norm = mcolors.Normalize(vmin=min(bytecode_values), vmax=max(bytecode_values))
        cmap = plt.cm.viridis  # Colormap pour la couleur
        
        scatter = ax.scatter(x_values, y_values, c=bytecode_values, 
                           cmap=cmap, norm=norm, s=100, alpha=0.6, edgecolors='black', linewidth=0.5)
        
        # Ajouter une colorbar
        cbar = plt.colorbar(scatter, ax=ax)
        cbar.set_label('Bytecode utilisé (somme)', rotation=270, labelpad=20)
    else:
        ax.scatter(x_values, y_values, s=100, alpha=0.6, edgecolors='black', linewidth=0.5)
    
    ax.set_xlabel('Iterations normal (somme Normal + REVERSE)', fontsize=12)
    ax.set_ylabel('Iterations split (somme Normal + REVERSE)', fontsize=12)
    ax.set_title('Rapport de Pathfinding\n(Couleur = Bytecode utilisé)', fontsize=14, fontweight='bold')
    ax.grid(True, alpha=0.3)
    
    plt.tight_layout()
    
    if output_file:
        plt.savefig(output_file, dpi=300, bbox_inches='tight')
        print(f"Graphique sauvegardé dans {output_file}")
    else:
        plt.show()


def main():
    """Fonction principale."""
    if len(sys.argv) < 2:
        print("Usage: python bench_pathfinding.py <fichier_texte> [fichier_sortie.png]")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else None
    
    if not Path(input_file).exists():
        print(f"Erreur: Le fichier {input_file} n'existe pas.")
        sys.exit(1)
    
    print(f"Parsing du fichier: {input_file}")
    reports = parse_pathfinding_file(input_file)
    
    print(f"Nombre de rapports extraits: {len(reports)}")
    
    if reports:
        x_values, y_values, bytecode_values = extract_data_for_plotting(reports)
        
        print(f"\nStatistiques:")
        print(f"  - Nombre de points: {len(x_values)}")
        if x_values:
            print(f"  - Iterations normal (min/max): {min(x_values)} / {max(x_values)}")
        if y_values:
            print(f"  - Iterations split (min/max): {min(y_values)} / {max(y_values)}")
        if bytecode_values:
            print(f"  - Bytecode total (min/max): {min(bytecode_values)} / {max(bytecode_values)}")
        
        plot_pathfinding_results(x_values, y_values, bytecode_values, output_file)
    else:
        print("Aucun rapport valide trouvé dans le fichier.")


if __name__ == '__main__':
    main()

