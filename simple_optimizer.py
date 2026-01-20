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

map_by_size = {
  "MAP_SMALL": [ "DefaultSmall", "arrows", "cheesefarm", "dirtfulcat", "evileye", "starvation"],
  "MAP_MEDIUM": [ "DefaultMedium", "Meow", "ZeroDay", "pipes", "popthecork", "rift", "sittingducks", "thunderdome"],
  "MAP_LARGE": [ "DefaultLarge", "Nofreecheese", "cheeseguardians", "dirtpassageway", "keepout", "trapped", "wallsofparadis"],
  "ALL": [ "DefaultLarge", "DefaultMedium", "DefaultSmall", "Meow", "Nofreecheese", "ZeroDay", "arrows", "cheesefarm", "cheeseguardians", "dirtfulcat", "dirtpassageway", "evileye", "keepout", "pipes", "popthecork", "rift", "sittingducks", "starvation", "thunderdome", "trapped", "wallsofparadis"]
}


import argparse
import json
import subprocess
import sys
import uuid
import shutil
import time
import random
import requests
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Tuple, Any
from tqdm import tqdm
from multiprocessing.pool import ThreadPool
import threading


class DiscordNotifier:
    """Send notifications to Discord webhook."""
    
    def __init__(self, webhook_url: str):
        self.webhook_url = webhook_url
        self.last_message_time = time.time()
        self.min_time_between_messages = 600  # 10 minutes
    
    def send_message(self, content: str, file_path: Path = None, force: bool = False):
        """
        Send a message to Discord webhook.
        
        Args:
            content: Message text to send
            file_path: Optional file to attach
            force: If True, ignore time throttling
        """
        if not self.webhook_url:
            return
        
        current_time = time.time()
        time_since_last = current_time - self.last_message_time
        
        # Check if we should send (force, or 10 minutes passed)
        if not force and time_since_last < self.min_time_between_messages:
            return
        
        try:
            data = {
                "content": content,
                "username": "BC26 Optimizer"
            }
            
            files = None
            if file_path and file_path.exists():
                with open(file_path, 'rb') as f:
                    files = {
                        'file': (file_path.name, f, 'application/json')
                    }
                    response = requests.post(
                        self.webhook_url,
                        data=data,
                        files=files
                    )
            else:
                response = requests.post(
                    self.webhook_url,
                    json=data
                )
            
            if response.status_code in [200, 204]:
                self.last_message_time = current_time
                print(f"   📨 Discord notification sent")
            else:
                print(f"   ⚠️  Discord send failed: {response.status_code}")
        
        except Exception as e:
            print(f"   ⚠️  Discord error: {e}")
    
    def send_start(self, config_info: str, params_list: List[str]):
        """Send start notification with detailed info."""
        params_str = ", ".join(params_list)
        message = (
            f"**Optimization started**\n"
            f"{config_info}\n"
            f"**Parameters to optimize:** {params_str}"
        )
        self.send_message(message, force=True)
    
    def send_parameter_optimized(self, param_name: str, old_value: int, 
                                new_value: int, results_summary: str,
                                best_config_file: Path):
        """Send notification when a parameter has been optimized."""
        message = (
            f"**Parameter optimized: {param_name}**\n"
            f"Old value: {old_value} → New value: {new_value}\n"
            f"```\n{results_summary}\n```"
        )
        self.send_message(message, file_path=best_config_file, force=True)
    
    def send_iteration_complete(self, iteration: int, best_score: float, 
                                evaluations: int, best_config_file: Path):
        """Send iteration complete notification with best config."""
        message = (
            f"✅ **Iteration {iteration} completed**\n"
            f"Best score: {best_score:.2f}%\n"
            f"Total evaluations: {evaluations}"
        )
        self.send_message(message, file_path=best_config_file, force=True)
    
    def send_periodic_update(self, iteration: int, best_score: float, 
                            evaluations: int, best_config_file: Path):
        """Send periodic update (every 10 minutes)."""
        message = (
            f"**Periodic update**\n"
            f"Iteration: {iteration}\n"
            f"Best score: {best_score:.2f}%\n"
            f"Evaluations: {evaluations}"
        )
        self.send_message(message, file_path=best_config_file, force=False)
    
    def send_end(self, iterations: int, evaluations: int, best_score: float, 
                best_config_file: Path):
        """Send end notification with final best config."""
        message = (
            f"**Optimization completed**\n"
            f"Iterations: {iterations}\n"
            f"Total evaluations: {evaluations}\n"
            f"Final best score: {best_score:.2f}%"
        )
        self.send_message(message, file_path=best_config_file, force=True)


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
    
    # Global lock to serialize Gradle calls
    _gradle_lock = threading.Lock()
    
    def __init__(
        self,
        template_config: Dict,
        base_config: Dict,
        project_root: Path,
        output_dir: Path,
        source: str = "current",
        maps: str = None,
        threads: int = 1,
        skip_eval: bool = False,
        discord_webhook: str = None
    ):
        self.template_config = template_config
        self.base_config = base_config
        self.project_root = project_root
        self.output_dir = output_dir
        self.source = source
        self.maps = maps
        self.threads = threads
        self.skip_eval = skip_eval
        
        self.tracker = ProgressTracker(output_dir)
        self.discord = DiscordNotifier(discord_webhook) if discord_webhook else None
        
        # Initialize current config
        if skip_eval:
            # Use base_config values directly
            self.current_config = {}
            for param_name, param_data in template_config.items():
                if param_name in base_config:
                    # Use value from base_config if available
                    base_value = base_config[param_name]['value']
                else:
                    # Fallback to middle value if not in base_config
                    base_value = (param_data['min'] + param_data['max']) // 2
                
                self.current_config[param_name] = {
                    'value': base_value,
                    'min': param_data['min'],
                    'max': param_data['max']
                }
        else:
            # Initialize with middle values
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
            cwd=str(self.project_root / "src"),
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
    
    def get_maps_for_config(self, config: Dict) -> List[str]:
        """
        Determine which maps to use based on the configuration parameters.
        If MAP_SMALL, MAP_MEDIUM, or MAP_LARGE are in the config, use only those maps.
        Otherwise, use all maps or the maps specified in self.maps.
        """
        if self.maps:
            # User explicitly specified maps
            return [m.strip() for m in self.maps.split(',')]
        
        # Check if config contains map size parameters
        map_types = []
        for param_name in config.keys():
            if "MAP_SMALL" in param_name:
                map_types.append("MAP_SMALL")
            if "MAP_MEDIUM" in param_name:
                map_types.append("MAP_MEDIUM")
            if "MAP_LARGE" in param_name:
                map_types.append("MAP_LARGE")
        
        # Remove duplicates
        map_types = list(set(map_types))
        
        if map_types:
            # Use only the maps corresponding to the types found
            maps = []
            for map_type in map_types:
                maps.extend(map_by_size[map_type])
            return list(set(maps))  # Remove duplicates
        else:
            # Use all maps
            return map_by_size["ALL"]
    
    def run_single_match(self, bot_a: str, bot_b: str, map_name: str, reverse: bool) -> bool:
        """
        Run a single match between two bots on a specific map.
        Returns True if bot_a wins, False otherwise.
        """
        player1 = bot_a if not reverse else bot_b
        player2 = bot_b if not reverse else bot_a
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        replay_name = f"matches/opt-{timestamp}-{player1}-vs-{player2}-on-{map_name}.bc26"
        
        direction = "reverse" if reverse else "normal"
        print(f"      🎮 Starting match: {map_name} ({direction}) - {player1} vs {player2}", flush=True)
        
        # Use lock to serialize Gradle calls and avoid build conflicts
        with self._gradle_lock:
            result = subprocess.run(
                [
                    str(self.project_root / "gradlew"),
                    "run",
                    f"-PteamA={player1}",
                    f"-PteamB={player2}",
                    f"-Pmaps={map_name}",
                    "-PlanguageA=java",
                    "-PlanguageB=java",
                    f"-Preplay={replay_name}",
                ],
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                cwd=str(self.project_root)
            )
        
        if result.returncode != 0:
            stdout = result.stdout.decode("utf-8")
            raise RuntimeError(f"Match failed: {player1} vs {player2} on {map_name}\n{stdout}")
        
        stdout = result.stdout.decode("utf-8")
        lines = stdout.splitlines()
        
        # Find winner
        winner_line = next((line for line in lines if ") wins (" in line), None)
        if not winner_line:
            raise RuntimeError(f"Could not find winner in match output")
        
        player1_wins = "(A)" in winner_line
        
        # Find win condition
        win_condition_line = next((line for line in lines if line.startswith("[server] Reason: ")), None)
        win_condition = "Unknown"
        if win_condition_line:
            win_condition = win_condition_line.split(": ", 1)[1]
            # Shorten win condition
            win_condition = {
                "The winning team destroyed all of the enemy team's rat kings.": "Kings",
                "The winning team won arbitrarily (coin flip).": "Coin",
                "Other team has resigned. ": "Resignation",
            }.get(win_condition, win_condition)
        
        winner = player1 if player1_wins else player2
        print(f"      ✅ Match completed: {map_name} ({direction}) - Winner: {winner} ({win_condition})", flush=True)
        
        # Return True if bot_a won (taking into account reverse)
        if not reverse:
            return player1_wins
        else:
            return not player1_wins
    
    def compare_bots(self, bot_a: str, bot_b: str, config: Dict) -> float:
        """
        Compare two bots and return win rate for bot_a.
        Runs matches in both directions (A vs B and B vs A) for each map.
        Returns win rate as percentage (0-100).
        """
        maps = self.get_maps_for_config(config)
        
        print(f"   📍 Comparing on {len(maps)} map(s): {', '.join(maps[:3])}{' ...' if len(maps) > 3 else ''}")
        
        # Generate all match triplets (map, bot_a, bot_b) with both directions
        match_tasks = []
        for map_name in maps:
            for reverse in [False, True]:
                match_tasks.append((bot_a, bot_b, map_name, reverse))
        
        total_matches = len(match_tasks)
        wins = 0
        
        # Run matches in parallel using thread pool
        if self.threads > 1:
            with ThreadPool(self.threads) as pool:
                # Use imap_unordered to get results as soon as they're ready
                results_iter = pool.starmap(self.run_single_match, match_tasks)
                for bot_a_wins in results_iter:
                    if bot_a_wins:
                        wins += 1
        else:
            # Sequential execution
            for bot_a, bot_b, map_name, reverse in match_tasks:
                bot_a_wins = self.run_single_match(bot_a, bot_b, map_name, reverse)
                if bot_a_wins:
                    wins += 1
        
        win_rate = (wins / total_matches * 100) if total_matches > 0 else 0
        print(f"   📊 Result: {wins}/{total_matches} wins ({win_rate:.2f}%)")
        
        return win_rate
    
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
                score = self.compare_bots(bot_id, base_bot_id, config)
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
        print(f"   Using {self.threads} thread(s)")
        
        # Step 1: Create all bot variations for testing
        print(f"   🔧 Creating {len(test_values)} bot variations...")
        bot_variations = []  # List of (value, bot_id, config_path, test_config)
        
        try:
            for value in test_values:
                # Create test config
                test_config = self.current_config.copy()
                test_config[param_name] = {
                    'value': value,
                    'min': param_min,
                    'max': param_max
                }
                
                # Create bot with this config
                bot_id, config_path = self.create_bot_with_config(test_config)
                bot_variations.append((value, bot_id, config_path, test_config))
                print(f"      ✅ Bot created for {param_name}={value}: {bot_id}")
            
            # Create base bot (opponent)
            base_bot_id, base_config_path = self.create_bot_with_config(self.base_config)
            print(f"      ✅ Reference bot created: {base_bot_id}")
            
            # Step 2: Generate all match triplets (map, bot_variation, base_bot)
            maps = self.get_maps_for_config(self.current_config)
            print(f"   🗺️  Generating triplets for {len(maps)} map(s)...")
            
            match_tasks = []  # List of (value, bot_id, base_bot_id, map_name, reverse)
            for value, bot_id, _, _ in bot_variations:
                for map_name in maps:
                    for reverse in [False, True]:
                        match_tasks.append((value, bot_id, base_bot_id, map_name, reverse))
            
            total_matches = len(match_tasks)
            print(f"   🎮 Total of {total_matches} matches to run: reverse * nMaps * nBots = 2 * {len(maps)} * {len(bot_variations)} = {total_matches}")
            
            # Step 3: Run all matches in parallel using thread pool
            # Function to run a single match and return result with value
            def run_match_with_value(args):
                value, bot_id, base_bot_id, map_name, reverse = args
                bot_wins = self.run_single_match(bot_id, base_bot_id, map_name, reverse)
                return (value, bot_wins)
            
            # Results dictionary: value -> list of wins (True/False)
            results_by_value = {value: [] for value, _, _, _ in bot_variations}
            
            if self.threads > 1:
                # Parallel execution
                with ThreadPool(self.threads) as pool:
                    pbar = tqdm(total=total_matches, desc=f"   Matches in progress", unit="match", leave=False)
                    for value, bot_wins in pool.imap_unordered(run_match_with_value, match_tasks):
                        results_by_value[value].append(bot_wins)
                        pbar.update(1)
                    pbar.close()
            else:
                # Sequential execution
                pbar = tqdm(match_tasks, desc=f"   Matches in progress", unit="match", leave=False)
                for args in pbar:
                    value, bot_wins = run_match_with_value(args)
                    results_by_value[value].append(bot_wins)
                pbar.close()
            
            # Step 4: Aggregate results for each variation
            print(f"   📊 Aggregating results...")
            results = []
            for value, bot_id, config_path, test_config in bot_variations:
                wins = sum(results_by_value[value])
                total = len(results_by_value[value])
                score = (wins / total * 100) if total > 0 else 0
                results.append((value, score, test_config))
                print(f"      {param_name}={value}: {wins}/{total} wins ({score:.2f}%)")
                
                # Log evaluation
                self.tracker.log_evaluation(param_name, value, score, test_config)
                
                # Send periodic Discord update (respects 10 min throttling)
                if self.discord:
                    self.discord.send_periodic_update(
                        self.tracker.iteration,
                        self.tracker.best_score,
                        self.tracker.evaluations,
                        self.tracker.best_config_file
                    )
            
        finally:
            # Cleanup all bots
            print(f"   🧹 Cleaning up temporary bots...")
            for value, bot_id, config_path, _ in bot_variations:
                self.cleanup_bot(bot_id, config_path)
            if 'base_bot_id' in locals():
                self.cleanup_bot(base_bot_id, base_config_path)
        
        # Find best result
        best_value, best_score, best_config = max(results, key=lambda x: x[1])
        
        print(f"\n   ✅ Best value for {param_name}: {best_value} (score: {best_score:.2f}%)")
        
        # Build results summary for Discord
        results_summary = f"Results for {param_name}:\n"
        for value, score, _ in sorted(results, key=lambda x: x[1], reverse=True):
            results_summary += f"  {param_name}={value}: {score:.2f}%\n"
        results_summary += f"\nBest value: {best_value} ({best_score:.2f}%)"
        
        # Check if improvement
        improved = best_score > self.tracker.best_score or (
            best_score == self.tracker.best_score and best_value != current_value
        )
        
        if improved or best_value != current_value:
            # Update current config
            old_value = current_value
            self.current_config = best_config
            print(f"   ➡️  Updated {param_name}: {current_value} → {best_value}")
            
            # Send Discord notification for parameter optimization
            if self.discord:
                self.discord.send_parameter_optimized(
                    param_name,
                    old_value,
                    best_value,
                    results_summary,
                    self.tracker.best_config_file
                )
            
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
        print(f"Threads: {self.threads}")
        print(f"Skip initial evaluation: {self.skip_eval}")
        print(f"Output directory: {self.output_dir}")
        print(f"{'='*80}\n")
        
        # Send Discord start notification
        if self.discord:
            config_info = (
                f"**Bot tested:** {self.source}\n"
                f"**Number of cores:** {self.threads}\n"
                f"**Max iterations:** {max_iterations}\n"
                f"**Maps:** {self.maps or 'all'}\n"
                f"**Number of parameters:** {len(self.template_config)}"
            )
            self.discord.send_start(config_info, list(self.template_config.keys()))
        
        # Evaluate initial configuration
        if self.skip_eval:
            print("⏩ Skipping initial evaluation (using base-config values)...")
            print(f"Starting configuration: {self.current_config}\n")
            initial_score = 50.0  # Neutral score
        else:
            print("📊 Evaluating initial configuration...")
            initial_score = self.evaluate_config(self.current_config)
            print(f"Initial score: {initial_score:.2f}%\n")
        
        self.tracker.best_score = initial_score
        self.tracker.best_config = self.current_config.copy()
        self.tracker.save_best()
        
        # Main optimization loop
        iteration = 0
        no_improvement_count = 0
        
        iteration_pbar = tqdm(range(max_iterations), desc="Optimization", unit="iter")
        for iteration_num in iteration_pbar:
            self.tracker.start_iteration()
            
            improved_this_iteration = False
            
            # Shuffle parameters list to avoid always optimizing in the same order
            param_names = list(self.template_config.keys())
            random.shuffle(param_names)
            
            # Iterate through each parameter
            param_pbar = tqdm(param_names, 
                            desc=f"  Iteration {iteration + 1}", 
                            unit="param", 
                            leave=False)
            for param_name in param_pbar:
                param_pbar.set_postfix_str(f"optimizing {param_name}")
                improved = self.optimize_parameter(param_name)
                if improved:
                    improved_this_iteration = True
            param_pbar.close()
            
            iteration += 1
            
            # Send Discord iteration complete notification
            if self.discord:
                self.discord.send_iteration_complete(
                    iteration,
                    self.tracker.best_score,
                    self.tracker.evaluations,
                    self.tracker.best_config_file
                )
            
            # Update main progress bar
            iteration_pbar.set_postfix_str(
                f"best={self.tracker.best_score:.2f}%, evals={self.tracker.evaluations}"
            )
            
            # Check for convergence
            if not improved_this_iteration:
                no_improvement_count += 1
                print(f"\n⚠️  No improvement in iteration {iteration}")
                
                if no_improvement_count >= 3:
                    print(f"\n✅ Converged after {iteration} iterations (no improvement for 3 iterations)")
                    iteration_pbar.close()
                    break
            else:
                no_improvement_count = 0
        else:
            iteration_pbar.close()
        
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
        
        # Send Discord end notification
        if self.discord:
            self.discord.send_end(
                iteration,
                self.tracker.evaluations,
                self.tracker.best_score,
                self.tracker.best_config_file
            )


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
    
    parser.add_argument(
        "--threads",
        type=int,
        default=1,
        help="Number of threads to use for parallel evaluation (default: 1)"
    )
    
    parser.add_argument(
        "--skip-eval",
        action="store_true",
        help="Skip initial configuration evaluation and use base-config values directly"
    )
    
    parser.add_argument(
        "--discord-webhook",
        help="Discord webhook URL for notifications"
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
    
    # Send webhook notification that script has started
    if args.discord_webhook:
        try:
            webhook_url = "https://discord.com/api/webhooks/1463193559975985427/ZlEjhoDOyo54etCf5J_DfwXg7JbfcuuBjpmsu5lLuwPi0u9veO3M-vivujSI2GW-JZO6"
            message = {
                "content": (
                    f"🚀 **Script démarré**\n"
                    f"**Source:** {args.source}\n"
                    f"**Threads:** {args.threads}\n"
                    f"**Max iterations:** {args.max_iterations}\n"
                    f"**Maps:** {args.maps or 'all'}\n"
                    f"**Paramètres:** {len(template_config)}\n"
                    f"**Skip eval:** {args.skip_eval}"
                ),
                "username": "BC26 Optimizer"
            }
            response = requests.post(webhook_url, json=message)
            if response.status_code in [200, 204]:
                print("📨 Notification Discord envoyée : script démarré\n")
            else:
                print(f"⚠️  Échec de l'envoi Discord : {response.status_code}\n")
        except Exception as e:
            print(f"⚠️  Erreur Discord : {e}\n")
    
    # Find project root
    script_dir = Path(__file__).parent
    project_root = script_dir
    
    output_dir = Path(args.output_dir).resolve()
    
    # Create and run optimizer
    optimizer = SimpleOptimizer(
        template_config=template_config,
        base_config=base_config,
        project_root=project_root,
        output_dir=output_dir,
        source=args.source,
        maps=args.maps,
        threads=args.threads,
        skip_eval=args.skip_eval,
        discord_webhook=args.discord_webhook
    )
    
    optimizer.run(max_iterations=args.max_iterations)
    
    return 0


if __name__ == "__main__":
    sys.exit(main())

