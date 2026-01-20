#!/bin/bash
# Script de test pour vérifier que l'installation GRASP est correcte

echo "=========================================="
echo "Test de l'installation GRASP"
echo "=========================================="
echo ""

# Vérifier Python
echo "1. Vérification de Python..."
if command -v python3 &> /dev/null; then
    echo "   ✅ Python3: $(python3 --version)"
else
    echo "   ❌ Python3 non trouvé"
    exit 1
fi

# Vérifier NumPy
echo ""
echo "2. Vérification de NumPy..."
if python3 -c "import numpy" 2>/dev/null; then
    echo "   ✅ NumPy installé"
else
    echo "   ⚠️  NumPy non installé (requis)"
    echo "   Installation: pip install numpy"
fi

# Vérifier tqdm (optionnel)
echo ""
echo "3. Vérification de tqdm..."
if python3 -c "import tqdm" 2>/dev/null; then
    echo "   ✅ tqdm installé (barres de progression activées)"
else
    echo "   ⚠️  tqdm non installé (optionnel mais recommandé)"
    echo "   Installation: pip install tqdm"
fi

# Vérifier les scripts
echo ""
echo "4. Vérification des scripts..."
scripts=(
    "src/params.py"
    "src/copybot.py"
    "src/jinja.py"
    "src/compare_bots.py"
    "src/compare_configs.py"
    "src/grasp.py"
    "src/grasp_parallel.py"
)

all_ok=true
for script in "${scripts[@]}"; do
    if [ -f "$script" ]; then
        if [ -x "$script" ]; then
            echo "   ✅ $script (exécutable)"
        else
            echo "   ⚠️  $script (non exécutable)"
        fi
    else
        echo "   ❌ $script (manquant)"
        all_ok=false
    fi
done

# Vérifier les fichiers SLURM
echo ""
echo "5. Vérification des fichiers SLURM..."
slurm_files=(
    "run_grasp.sbatch"
    "run_grasp_quick.sbatch"
)

for file in "${slurm_files[@]}"; do
    if [ -f "$file" ]; then
        echo "   ✅ $file"
    else
        echo "   ❌ $file (manquant)"
        all_ok=false
    fi
done

# Vérifier les dossiers
echo ""
echo "6. Vérification des dossiers..."
dirs=(
    "logs"
    "BC26/grasp/results"
    "BC26/grasp/bots"
)

for dir in "${dirs[@]}"; do
    if [ -d "$dir" ]; then
        echo "   ✅ $dir/"
    else
        echo "   ⚠️  $dir/ (sera créé automatiquement)"
    fi
done

# Vérifier la documentation
echo ""
echo "7. Vérification de la documentation..."
docs=(
    "README.md"
    "PARAMS_README.md"
    "GRASP_README.md"
)

for doc in "${docs[@]}"; do
    if [ -f "$doc" ]; then
        echo "   ✅ $doc"
    else
        echo "   ❌ $doc (manquant)"
    fi
done

# Test rapide des scripts
echo ""
echo "8. Test rapide des scripts..."
if python3 src/grasp_parallel.py --help > /dev/null 2>&1; then
    echo "   ✅ grasp_parallel.py fonctionne"
else
    echo "   ❌ grasp_parallel.py a des erreurs"
    all_ok=false
fi

if python3 src/compare_configs.py --help > /dev/null 2>&1; then
    echo "   ✅ compare_configs.py fonctionne"
else
    echo "   ❌ compare_configs.py a des erreurs"
    all_ok=false
fi

# Résumé
echo ""
echo "=========================================="
if [ "$all_ok" = true ]; then
    echo "✅ Installation complète et fonctionnelle!"
    echo ""
    echo "Pour commencer:"
    echo "  - Local: python3 src/grasp_parallel.py --help"
    echo "  - Cluster: sbatch run_grasp_quick.sbatch"
    echo ""
    echo "Documentation: voir GRASP_README.md"
else
    echo "⚠️  Quelques éléments manquent ou ont des erreurs"
    echo "Vérifiez les messages ci-dessus"
fi
echo "=========================================="

