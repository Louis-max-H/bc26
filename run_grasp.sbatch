#!/bin/bash
#SBATCH --job-name=grasp_battlecode
#SBATCH --output=logs/grasp_%j.out
#SBATCH --error=logs/grasp_%j.err
#SBATCH --time=48:00:00
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=16
#SBATCH --mem=32G
#SBATCH --partition=compute

# Description: Script SLURM pour optimisation GRASP sur cluster
# Usage: sbatch run_grasp.sbatch

echo "=========================================="
echo "GRASP Battlecode Optimization"
echo "=========================================="
echo "Job ID: $SLURM_JOB_ID"
echo "Node: $SLURM_NODELIST"
echo "CPUs allocated: $SLURM_CPUS_PER_TASK"
echo "Memory allocated: $SLURM_MEM_PER_NODE MB"
echo "Working directory: $(pwd)"
echo "=========================================="

# Créer le dossier de logs si nécessaire
mkdir -p logs

# Variables d'environnement
export OMP_NUM_THREADS=$SLURM_CPUS_PER_TASK
export MKL_NUM_THREADS=$SLURM_CPUS_PER_TASK

# Activer l'environnement virtuel si nécessaire
# source venv/bin/activate

# Paramètres de l'optimisation
TEMPLATE="example_config.json"
BASE_CONFIG="example_config.json"
ITERATIONS=5
BATCH_SIZE=2
MAPS="DefaultSmall,DefaultMedium"
OUTPUT_DIR="BC26/grasp_results/job_${SLURM_JOB_ID}"
SAVE_EVERY=2

echo ""
echo "Configuration:"
echo "  Template: $TEMPLATE"
echo "  Base config: $BASE_CONFIG"
echo "  Iterations: $ITERATIONS"
echo "  Batch size: $BATCH_SIZE"
echo "  Maps: $MAPS"
echo "  Output: $OUTPUT_DIR"
echo "  Workers: Auto-detect ($SLURM_CPUS_PER_TASK CPUs)"
echo ""

# Créer le dossier de sortie
mkdir -p "$OUTPUT_DIR"

# Sauvegarder les informations du job
cat > "$OUTPUT_DIR/job_info.txt" << EOF
Job ID: $SLURM_JOB_ID
Node: $SLURM_NODELIST
CPUs: $SLURM_CPUS_PER_TASK
Memory: $SLURM_MEM_PER_NODE MB
Start time: $(date)
Working directory: $(pwd)
EOF

# Exécuter l'optimisation GRASP
echo "Starting GRASP optimization..."
echo "=========================================="

python3 src/grasp_parallel.py \
    --template "$TEMPLATE" \
    --base-config "$BASE_CONFIG" \
    --iterations $ITERATIONS \
    --batch-size $BATCH_SIZE \
    --maps "$MAPS" \
    --output-dir "$OUTPUT_DIR" \
    --save-every $SAVE_EVERY \
    --alpha-start 0.5 \
    --alpha-end 0.1

EXIT_CODE=$?

# Sauvegarder le statut
echo "End time: $(date)" >> "$OUTPUT_DIR/job_info.txt"
echo "Exit code: $EXIT_CODE" >> "$OUTPUT_DIR/job_info.txt"

if [ $EXIT_CODE -eq 0 ]; then
    echo "=========================================="
    echo "✅ GRASP optimization completed successfully!"
    echo "Results saved in: $OUTPUT_DIR"
    echo "=========================================="
else
    echo "=========================================="
    echo "❌ GRASP optimization failed with code $EXIT_CODE"
    echo "=========================================="
fi

exit $EXIT_CODE

