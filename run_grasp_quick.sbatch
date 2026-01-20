#!/bin/bash

#SBATCH --job-name=grasp_quick
#SBATCH --partition=court # <48h
#SBATCH --time=4:00:00
#SBATCH --time=10:0 # temps max alloué au job (format = m:s ou h:m:s ou j-h:m:s)
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=12
#SBATCH --mem=16G
#SBATCH --immediate # le job est tué si les ressources ne sont pas immédiatement disponibles
#SBATCH --mail-type=END # notification par mail, type = BEGIN, END, FAIL ou ALL
#SBATCH --mail-user="louis-max.harter@protonmail.com" # adresse mail à qui envoyer les notifications
#SBATCH --output=logs/grasp_quick_%j.out
#SBATCH --error=logs/grasp_quick_%j.err

# Description: Version rapide pour tests (moins d'itérations)
# Usage: sbatch run_grasp_quick.sbatch

echo "=========================================="
echo "GRASP Quick Test"
echo "=========================================="
echo "Job ID: $SLURM_JOB_ID"
echo "Node: $SLURM_NODELIST"
echo "CPUs: $SLURM_CPUS_PER_TASK"
echo "=========================================="

mkdir -p logs

export OMP_NUM_THREADS=$SLURM_CPUS_PER_TASK
export MKL_NUM_THREADS=$SLURM_CPUS_PER_TASK

# Paramètres pour test rapide
TEMPLATE="example_config.json"
BASE_CONFIG="example_config.json"
ITERATIONS=2
BATCH_SIZE=2
MAPS="DefaultSmall"
OUTPUT_DIR="BC26/grasp_results/quick_${SLURM_JOB_ID}"

mkdir -p "$OUTPUT_DIR"

echo "Quick test configuration:"
echo "  Iterations: $ITERATIONS"
echo "  Batch size: $BATCH_SIZE"
echo "  Maps: $MAPS (only one for speed)"
echo ""

# Utilisation de Docker avec Java 21 et Python
# Image contenant Python 3.11 et compatible avec Java 21
DOCKER_IMAGE="python:3.11-slim"
WORK_DIR="/workspace"

echo "Pulling Docker image..."
docker pull "$DOCKER_IMAGE"

echo "Running GRASP with Docker (Java 21 + Python)..."
docker run --rm \
    -v "$(pwd):$WORK_DIR" \
    -w "$WORK_DIR" \
    -e OMP_NUM_THREADS="$OMP_NUM_THREADS" \
    -e MKL_NUM_THREADS="$MKL_NUM_THREADS" \
    -e SLURM_CPUS_PER_TASK="$SLURM_CPUS_PER_TASK" \
    --cpus="$SLURM_CPUS_PER_TASK" \
    --memory="16g" \
    "$DOCKER_IMAGE" \
    bash -c "\
        apt-get update && \
        apt-get install -y openjdk-21-jdk-headless && \
        export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 && \
        export PATH=\$JAVA_HOME/bin:\$PATH && \
        java -version && \
        python3 src/grasp_parallel.py \
            --template '$TEMPLATE' \
            --base-config '$BASE_CONFIG' \
            --iterations $ITERATIONS \
            --batch-size $BATCH_SIZE \
            --maps '$MAPS' \
            --output-dir '$OUTPUT_DIR' \
            --save-every 2"

echo "Quick test completed!"

