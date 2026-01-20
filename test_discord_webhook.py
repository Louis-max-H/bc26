#!/usr/bin/env python3
"""
Test script to verify Discord webhook.
"""

import requests
import json
import sys
from pathlib import Path

# Discord webhook URL
WEBHOOK_URL = "https://discord.com/api/webhooks/1463193559975985427/ZlEjhoDOyo54etCf5J_DfwXg7JbfcuuBjpmsu5lLuwPi0u9veO3M-vivujSI2GW-JZO6"

def test_simple_message():
    """Test sending a simple message."""
    print("📤 Test 1: Sending a simple message...")
    
    data = {
        "content": "🧪 **Discord webhook test**\nThis is a test message from BC26 Optimizer!",
        "username": "BC26 Optimizer Test"
    }
    
    try:
        response = requests.post(WEBHOOK_URL, json=data)
        
        if response.status_code in [200, 204]:
            print("✅ Message sent successfully!")
            return True
        else:
            print(f"❌ Failed: code {response.status_code}")
            print(f"   Response: {response.text}")
            return False
    except Exception as e:
        print(f"❌ Error: {e}")
        return False


def test_message_with_file():
    """Test sending a message with JSON file."""
    print("\n📤 Test 2: Sending a message with JSON file...")
    
    # Create a test JSON file
    test_config = {
        "PARAM_1": {"value": 100, "min": 0, "max": 200},
        "PARAM_2": {"value": 50, "min": 0, "max": 100},
        "PARAM_3": {"value": 75, "min": 0, "max": 150}
    }
    
    test_file = Path("/tmp/test_config.json")
    with open(test_file, 'w') as f:
        json.dump(test_config, f, indent=2)
    
    try:
        data = {
            "content": "📎 **Test with attached file**\nHere's an example configuration:",
            "username": "BC26 Optimizer Test"
        }
        
        with open(test_file, 'rb') as f:
            files = {
                'file': ('test_config.json', f, 'application/json')
            }
            response = requests.post(WEBHOOK_URL, data=data, files=files)
        
        if response.status_code in [200, 204]:
            print("✅ Message with file sent successfully!")
            return True
        else:
            print(f"❌ Failed: code {response.status_code}")
            print(f"   Response: {response.text}")
            return False
    except Exception as e:
        print(f"❌ Error: {e}")
        return False
    finally:
        # Clean up test file
        if test_file.exists():
            test_file.unlink()


def test_formatted_message():
    """Test sending a formatted message (like in the optimizer)."""
    print("\n📤 Test 3: Sending a formatted message...")
    
    data = {
        "content": (
            "🚀 **Optimization started**\n"
            "```\n"
            "Parameters: 5\n"
            "Max iterations: 100\n"
            "Source: current\n"
            "Maps: all\n"
            "Threads: 4\n"
            "```"
        ),
        "username": "BC26 Optimizer"
    }
    
    try:
        response = requests.post(WEBHOOK_URL, json=data)
        
        if response.status_code in [200, 204]:
            print("✅ Formatted message sent successfully!")
            return True
        else:
            print(f"❌ Failed: code {response.status_code}")
            print(f"   Response: {response.text}")
            return False
    except Exception as e:
        print(f"❌ Error: {e}")
        return False


def main():
    print("="*60)
    print("🧪 Discord webhook test for BC26 Optimizer")
    print("="*60)
    print(f"\nWebhook URL: {WEBHOOK_URL[:50]}...")
    print()
    
    results = []
    
    # Test 1: Simple message
    results.append(test_simple_message())
    
    # Test 2: Message with file
    results.append(test_message_with_file())
    
    # Test 3: Formatted message
    results.append(test_formatted_message())
    
    # Summary
    print("\n" + "="*60)
    print("📊 Test summary")
    print("="*60)
    success_count = sum(results)
    total_count = len(results)
    
    if success_count == total_count:
        print(f"✅ All tests passed ({success_count}/{total_count})")
        print("\n🎉 Discord webhook works perfectly!")
        return 0
    else:
        print(f"⚠️  {success_count}/{total_count} tests passed")
        print("\n⚠️  Check the webhook URL and permissions")
        return 1


if __name__ == "__main__":
    sys.exit(main())

