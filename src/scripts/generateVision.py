import math
import matplotlib.pyplot as plt
from matplotlib.colors import ListedColormap
import numpy as np

directions = ["W", "NW", "SW", "S", "SE", "E", "NE", "N", "C"]
directionsWhitoutCenter = ["W", "NW", "SW", "S", "SE", "E", "NE", "N"]

anglesDirections = {
    "W":  180,
    "NW": 135,
    "SW": 225,
    "S":  270,
    "SE": 315,
    "E":  0,
    "NE": 45,
    "N":  90,
    "C":  0
}
unitsVisionRadius = {"Baby":20, "King":25, "Cat":30}
unitsVisionAngle = {"Baby":90, "King":360, "Cat":120}
unitsOrdinal = {"Baby":0, "King":1, "Cat":2}
directionsOrdinal = {"W": 0, "NW": 1, "N": 2, "NE": 3, "E": 4, "SE": 5, "S": 6, "SW": 7, "C": 8}

dirsDelta = {
    "N" :  (0, 1),
    "NE" : (1,1),
    "E" : (1,0),
    "SE" : (1,-1),
    "S" : (0,-1),
    "SW" : (-1,-1),
    "W" : (-1,0),
    "NW" : (-1,1),
    "C" : (0,0)
}

def isAngleInBounds(angle_min, angle_max, angle):
    """
    Returns True if the angle is within [angle_min, angle_max] in degrees, handling wrap-around and negative/over-360 angles.
    All input angles can be any real number. Intervals with angle_max < angle_min are considered wrapping across 0.
    """

    # Normalize all angles to [0, 360)
    norm = lambda a: a % 360
    amin = norm(angle_min)
    amax = norm(angle_max)
    a = norm(angle)

    if amin <= amax:
        # Simple non-wrapping interval
        return amin <= a <= amax
    else:
        # Wrapping interval (e.g., 300 to 60 means [300, 360) U [0, 60])
        return a >= amin or a <= amax


def getAngle(x, y):
    angle = math.atan2(y, x) # [-pi, pi]
    angle = angle * 180 / math.pi # [-180, 180]
    if angle < 0:
        angle += 360 # [0, 360]
    return angle


def visionCells(unit, direction):
    if not unit in unitsVisionRadius.keys():
        print(f"Unit {unit} is unknow. Should be in {list(unitsVisionRadius.keys())}")
        return []

    if not direction in anglesDirections.keys():
        print(f"Direction {direction} is unknow. Should be in {list(anglesDirections.keys())}")
        return []

    R = unitsVisionRadius[unit]
    A = unitsVisionAngle[unit]
    cappedR = int(R**0.5 + 1)
    angleDirection = anglesDirections[direction]

    cells = []
    for x in range(-cappedR, cappedR + 1):
        for y in range(-cappedR, cappedR + 1):

            if (x, y) == (0, 0):
                cells.append((x, y))
                continue

            dist = x**2 + y**2
            if (dist > R):
                continue

            if A < 360 and not isAngleInBounds(
                angleDirection - A/2,
                angleDirection + A/2,
                getAngle(x, y)
            ):
                continue

            cells.append((x, y))
    return cells

def visualizeVision():
    """Crée une visualisation pour chaque unité et chaque direction montrant les cellules visibles."""
    for unit in unitsVisionRadius.keys():
        R = unitsVisionRadius[unit]
        cappedR = int(R**0.5 + 1)
        grid_size = 2 * cappedR + 1
        
        # Créer une figure avec une sous-grille pour chaque direction
        directions_list = list(anglesDirections.keys())
        n_directions = len(directions_list)
        
        # Organiser en grille : 3 colonnes
        n_cols = 3
        n_rows = (n_directions + n_cols - 1) // n_cols
        
        fig, axes = plt.subplots(n_rows, n_cols, figsize=(15, 5 * n_rows))
        fig.suptitle(f'Vision pour l\'unité: {unit} (R={R})', fontsize=16, fontweight='bold')
        
        # Aplatir les axes pour faciliter l'itération
        if n_rows == 1:
            axes = axes.reshape(1, -1)
        axes_flat = axes.flatten()
        
        for idx, direction in enumerate(directions_list):
            ax = axes_flat[idx]
            
            # Obtenir les cellules visibles
            cells = visionCells(unit, direction)
            
            # Créer une grille vide (0 = non visible, 1 = visible)
            grid = np.zeros((grid_size, grid_size), dtype=int)
            
            # Marquer les cellules visibles
            for x, y in cells:
                # Convertir les coordonnées relatives en indices de grille
                # x et y vont de -cappedR à cappedR
                grid_x = x + cappedR
                grid_y = y + cappedR
                if 0 <= grid_x < grid_size and 0 <= grid_y < grid_size:
                    grid[grid_y, grid_x] = 1  # Note: grid_y en premier pour avoir y vers le bas
            
            # Créer la visualisation avec y inversé (vers le bas)
            # Utiliser une palette binaire : gris pour non visible, vert pour visible
            colors = ['lightgray', 'green']
            cmap = ListedColormap(colors)
            im = ax.imshow(grid, cmap=cmap, vmin=0, vmax=1, origin='upper', aspect='equal')
            
            # Marquer la cellule centrale (0, 0) avec un point
            center_x, center_y = cappedR, cappedR
            ax.plot(center_x, center_y, 'ro', markersize=8, markeredgecolor='darkred', 
                   markeredgewidth=1.5, label='Centre (0,0)')
            
            # Configuration de l'axe
            ax.set_title(f'{direction}', fontsize=12, fontweight='bold')
            ax.set_xlabel('X')
            ax.set_ylabel('Y')
            
            # Définir les ticks pour montrer les coordonnées
            tick_positions = range(0, grid_size, max(1, grid_size // 5))
            tick_labels = [str(i - cappedR) for i in tick_positions]
            ax.set_xticks(tick_positions)
            ax.set_xticklabels(tick_labels)
            ax.set_yticks(tick_positions)
            ax.set_yticklabels(tick_labels)
            
            # Ajouter une grille pour mieux voir les cellules
            ax.grid(True, color='gray', linestyle='-', linewidth=0.5, alpha=0.3)
            ax.set_xticks(np.arange(-0.5, grid_size, 1), minor=True)
            ax.set_yticks(np.arange(-0.5, grid_size, 1), minor=True)
            ax.grid(True, which='minor', color='black', linestyle='-', linewidth=0.5)
        
        # Masquer les axes non utilisés
        for idx in range(n_directions, len(axes_flat)):
            axes_flat[idx].axis('off')
        
        plt.tight_layout()
        plt.savefig(f'vision_{unit}.png', dpi=150, bbox_inches='tight')
        print(f"Visualisation sauvegardée: vision_{unit}.png")
        plt.close()



if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1 :
        if sys.argv[1] == "--visualize":
            visualizeVision()
        elif sys.argv[1] == "--code":
            generateGetCellsLocation()
        elif sys.argv[1] == "--combo":
            generateGetComboScore()
    else:
        print("Usage: python generateVision.py --visualize | --code")
        print("  --visualize: Visualize the vision of each unit and direction")
        print("  --code: Generate the code for the vision of each unit and direction")