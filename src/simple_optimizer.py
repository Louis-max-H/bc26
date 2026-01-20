#!/usr/bin/env python3
"""
Simple Coordinate Descent Optimizer for Battlecode.

This optimizer:
1. Takes one parameter at a time
2. Tests 3 values for that parameter
3. Keeps the best configuration
4. Moves to the next parameter
5. Repeats until convergence or max iterations
"""

import argparse
import json
import subprocess
import sys
import uuid
import shutil
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Tuple, Any


class ProgressTracker:
    """Track optimization progress."""
    
    def __init__(self, output_dir: Path):
        self.output_dir = output_dir
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        self.iteration = 0
        self.evaluations = 0
        self.best_score = 0.0
        self.best_config = None
        self.history = []
        
        self.progress_file = output_dir / "progress.json"
        self.best_config_file = output_dir / "best_config.json"
        self.history_file = output_dir / "history.json"
    
    def log_evaluation(self, param_name: str, param_value: int, score: float, config: Dict):
        """Log a single evaluation."""
        self.evaluations += 1
        
        entry = {
            "evaluation": self.evaluations,
            "iteration": self.iteration,
            "parameter": param_name,
            "value": param_value,
            "score": score,
            "timestamp": datetime.now().isoformat()
        }
        
        self.history.append(entry)
        
        # Update best if better
        if score > self.best_score:
            self.best_score = score
            self.best_config = config.copy()
            print(f"   🌟 New best score: {score:.2f}%")
            self.save_best()
        
        self.save_progress()
    
    def start_iteration(self):
        """Start a new iteration."""
        self.iteration += 1
        print(f"\n{'='*80}")
        print(f"🔄 Iteration {self.iteration}")
        print(f"{'='*80}")
        print(f"Current best score: {self.best_score:.2f}%")
        print(f"Total evaluations: {self.evaluations}")
        print(f"{'='*80}\n")
    
    def save_progress(self):
        """Save progress to disk."""
        progress = {
            "iteration": self.iteration,
            "evaluations": self.evaluations,
            "best_score": self.best_score,
            "best_config": self.best_config,
            "last_update": datetime.now().isoformat()
        }
        
        with open(self.progress_file, 'w') as f:
            json.dump(progress, f, indent=2)
        
        with open(self.history_file, 'w') as f:
            json.dump(self.history, f, indent=2)
    
    def save_best(self):
        """Save best configuration."""
        if self.best_config:
            with open(self.best_config_file, 'w') as f:
                json.dump(self.best_config, f, indent=2)


class SimpleOptimizer:
    """Simple coordinate descent optimizer."""
    
    def __init__(
        self,
        template_config: Dict,
        base_config: Dict,
        project_root: Path,
        output_dir: Path,
        source: str = "current",
        maps: str = None
    ):
        self.template_config = template_config
        self.base_config = base_config
        self.project_root = project_root
        self.output_dir = output_dir
        self.source = source
        self.maps = maps
        
        self.tracker = ProgressTracker(output_dir)
        
        # Initialize current config with middle values
        self.current_config = {}
        for param_name, param_data in template_config.items():
            mid_value = (param_data['min'] + param_data['max']) // 2
            self.current_config[param_name] = {
                'value': mid_value,
                'min': param_data['min'],
                'max': param_data['max']
            }
    
    def create_bot_with_config(self, config: Dict) -> Tuple[str, Path]:
        """
        Create a temporary bot with given configuration.
        Returns (bot_name, config_path).
        """
        # Generate unique ID
        bot_id = f"tmp{uuid.uuid4().hex[:8]}"
        
        # Save config to temporary file
        temp_configs_dir = self.output_dir / "temp_configs"
        temp_configs_dir.mkdir(parents=True, exist_ok=True)
        config_path = temp_configs_dir / f"{bot_id}.json"
        
        with open(config_path, 'w') as f:
            json.dump(config, f, indent=2)
        
        # Copy bot
        result = subprocess.run(
            ["python3", str(self.project_root / "src" / "copybot.py"), self.source, bot_id],
            cwd=str(self.project_root),
            capture_output=True,
            text=True
        )
        
        if result.returncode != 0:
            raise RuntimeError(f"Failed to copy bot: {result.stderr}")
        
        # Import params
        result = subprocess.run(
            ["python3", str(self.project_root / "src" / "params.py"), bot_id, "--import", str(config_path)],
            cwd=str(self.project_root),
            capture_output=True,
            text=True
        )
        
        if result.returncode != 0:
            raise RuntimeError(f"Failed to import params: {result.stderr}")
        
        # Run jinja
        result = subprocess.run(
            ["python3", str(self.project_root / "src" / "jinja.py"), 
             str(self.project_root / "src" / bot_id), "--prod", "--params", str(config_path)],
            cwd=str(self.project_root),
            capture_output=True,
            text=True
        )
        
        if result.returncode != 0:
            raise RuntimeError(f"Failed to run jinja: {result.stderr}")
        
        return bot_id, config_path
    
    def cleanup_bot(self, bot_id: str, config_path: Path):
        """Clean up temporary bot and config."""
        bot_path = self.project_root / "src" / bot_id
        if bot_path.exists() and bot_id.startswith("tmp"):
            shutil.rmtree(bot_path)
        
        if config_path.exists():
            config_path.unlink()
    
    def compare_bots(self, bot_a: str, bot_b: str) -> float:
        """
        Compare two bots and return win rate for bot_a.
        Returns win rate as percentage (0-100).
        """
        cmd = [
            "python3",
            str(self.project_root / "src" / "compare_bots.py"),
            bot_a,
            bot_b,
            "--json"
        ]
        
        if self.maps:
            cmd.extend(["--maps", self.maps])
        
        result = subprocess.run(
            cmd,
            cwd=str(self.project_root),
            capture_output=True,
            text=True,
            timeout=3600
        )
        
        if result.returncode != 0:
            raise RuntimeError(f"Failed to compare bots: {result.stderr}")
        
        # Parse JSON output
        data = json.loads(result.stdout)
        return data['player1_stats']['win_rate']
    
    def evaluate_config(self, config: Dict) -> float:
        """
        Evaluate a configuration by creating a bot and comparing it against base.
        Returns win rate as percentage.
        """
        bot_id = None
        config_path = None
        
        try:
            # Create bot with config
            bot_id, config_path = self.create_bot_with_config(config)
            
            # Create base bot (opponent)
            base_bot_id, base_config_path = self.create_bot_with_config(self.base_config)
            
            try:
                # Compare bots
                score = self.compare_bots(bot_id, base_bot_id)
                return score
            finally:
                # Clean up base bot
                self.cleanup_bot(base_bot_id, base_config_path)
        
        finally:
            # Clean up test bot
            if bot_id:
                self.cleanup_bot(bot_id, config_path)
    
    def optimize_parameter(self, param_name: str) -> bool:
        """
        Optimize a single parameter by testing 3 values.
        Returns True if improvement was found.
        """
        param_data = self.template_config[param_name]
        current_value = self.current_config[param_name]['value']
        param_min = param_data['min']
        param_max = param_data['max']
        
        print(f"\n📊 Optimizing parameter: {param_name}")
        print(f"   Current value: {current_value}")
        print(f"   Range: [{param_min}, {param_max}]")
        
        # Generate 3 test values
        # Strategy: current value, and two points around it
        range_size = param_max - param_min
        step = max(1, range_size // 5)  # 20% of range
        
        test_values = []
        
        # Value 1: lower than current
        val1 = max(param_min, current_value - step)
        if val1 not in test_values and val1 != current_value:
            test_values.append(val1)
        
        # Value 2: current value
        if current_value not in test_values:
            test_values.append(current_value)
        
        # Value 3: higher than current
        val3 = min(param_max, current_value + step)
        if val3 not in test_values and val3 != current_value:
            test_values.append(val3)
        
        # If we only have current value, add min and max
        while len(test_values) < 3:
            if param_min not in test_values:
                test_values.insert(0, param_min)
            if len(test_values) < 3 and param_max not in test_values:
                test_values.append(param_max)
            if len(test_values) < 3:
                # Add a random value
                import random
                val = random.randint(param_min, param_max)
                if val not in test_values:
                    test_values.append(val)
        
        test_values = test_values[:3]  # Keep only 3
        
        print(f"   Testing values: {test_values}")
        
        # Evaluate each value
        results = []
        for i, value in enumerate(test_values, 1):
            print(f"\n   [{i}/3] Testing {param_name} = {value}...")
            
            # Create test config
            test_config = self.current_config.copy()
            test_config[param_name] = {
                'value': value,
                'min': param_min,
                'max': param_max
            }
            
            # Evaluate
            score = self.evaluate_config(test_config)
            results.append((value, score, test_config))
            
            print(f"         Score: {score:.2f}%")
            
            # Log evaluation
            self.tracker.log_evaluation(param_name, value, score, test_config)
        
        # Find best result
        best_value, best_score, best_config = max(results, key=lambda x: x[1])
        
        print(f"\n   ✅ Best value for {param_name}: {best_value} (score: {best_score:.2f}%)")
        
        # Check if improvement
        improved = best_score > self.tracker.best_score or (
            best_score == self.tracker.best_score and best_value != current_value
        )
        
        if improved or best_value != current_value:
            # Update current config
            self.current_config = best_config
            print(f"   ➡️  Updated {param_name}: {current_value} → {best_value}")
            return True
        else:
            print(f"   ⏸️  No improvement, keeping {param_name} = {current_value}")
            return False
    
    def run(self, max_iterations: int = 100):
        """
        Run the optimization loop.
        
        Args:
            max_iterations: Maximum number of complete iterations through all parameters
        """
        print(f"\n{'='*80}")
        print(f"🚀 Starting Simple Coordinate Descent Optimization")
        print(f"{'='*80}")
        print(f"Parameters to optimize: {list(self.template_config.keys())}")
        print(f"Max iterations: {max_iterations}")
        print(f"Source: {self.source}")
        print(f"Maps: {self.maps or 'all'}")
        print(f"Output directory: {self.output_dir}")
        print(f"{'='*80}\n")
        
        # Evaluate initial configuration
        print("📊 Evaluating initial configuration...")
        initial_score = self.evaluate_config(self.current_config)
        print(f"Initial score: {initial_score:.2f}%\n")
        
        self.tracker.best_score = initial_score
        self.tracker.best_config = self.current_config.copy()
        self.tracker.save_best()
        
        # Main optimization loop
        iteration = 0
        no_improvement_count = 0
        
        while iteration < max_iterations:
            self.tracker.start_iteration()
            
            improved_this_iteration = False
            
            # Iterate through each parameter
            for param_name in self.template_config.keys():
                improved = self.optimize_parameter(param_name)
                if improved:
                    improved_this_iteration = True
            
            iteration += 1
            
            # Check for convergence
            if not improved_this_iteration:
                no_improvement_count += 1
                print(f"\n⚠️  No improvement in iteration {iteration}")
                
                if no_improvement_count >= 3:
                    print(f"\n✅ Converged after {iteration} iterations (no improvement for 3 iterations)")
                    break
            else:
                no_improvement_count = 0
        
        # Final summary
        print(f"\n{'='*80}")
        print(f"✅ Optimization Complete!")
        print(f"{'='*80}")
        print(f"Total iterations: {iteration}")
        print(f"Total evaluations: {self.tracker.evaluations}")
        print(f"Best score: {self.tracker.best_score:.2f}%")
        print(f"Best configuration saved to: {self.tracker.best_config_file}")
        print(f"Progress saved to: {self.tracker.progress_file}")
        print(f"History saved to: {self.tracker.history_file}")
        print(f"{'='*80}\n")


def main():
    parser = argparse.ArgumentParser(
        description="Simple coordinate descent optimizer for Battlecode"
    )
    
    parser.add_argument(
        "--template",
        required=True,
        help="Template configuration file (JSON with min/max bounds)"
    )
    
    parser.add_argument(
        "--base-config",
        required=True,
        help="Base configuration (opponent to compare against)"
    )
    
    parser.add_argument(
        "--source",
        default="current",
        help="Source bot directory (default: current)"
    )
    
    parser.add_argument(
        "--maps",
        help="Comma-separated list of maps (e.g., DefaultSmall,DefaultMedium)"
    )
    
    parser.add_argument(
        "--output-dir",
        default="BC26/simple_optimizer",
        help="Output directory for results (default: BC26/simple_optimizer)"
    )
    
    parser.add_argument(
        "--max-iterations",
        type=int,
        default=100,
        help="Maximum number of iterations (default: 100)"
    )
    
    args = parser.parse_args()
    
    # Load configurations
    template_path = Path(args.template).resolve()
    base_config_path = Path(args.base_config).resolve()
    
    if not template_path.exists():
        print(f"❌ Template file not found: {template_path}")
        return 1
    
    if not base_config_path.exists():
        print(f"❌ Base config file not found: {base_config_path}")
        return 1
    
    with open(template_path, 'r') as f:
        template_config = json.load(f)
    
    with open(base_config_path, 'r') as f:
        base_config = json.load(f)
    
    # Find project root
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    
    output_dir = Path(args.output_dir).resolve()
    
    # Create and run optimizer
    optimizer = SimpleOptimizer(
        template_config=template_config,
        base_config=base_config,
        project_root=project_root,
        output_dir=output_dir,
        source=args.source,
        maps=args.maps
    )
    
    optimizer.run(max_iterations=args.max_iterations)
    
    return 0


if __name__ == "__main__":
    sys.exit(main())

