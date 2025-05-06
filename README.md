# QueuePlus

QueuePlus is a powerful and customizable queue management plugin designed for Velocity proxy servers. It provides a points-based queue system that effectively manages player connections to backend servers, ensuring fairness and priority during high server traffic or limited slots.

## Features

- **Points-Based Queue System:**  
  Players join a queue and earn points over time, increasing their priority to connect to a specified backend server.

- **Start Points:**  
  Assign players initial points to give them an entry advantage or modify these dynamically.

- **Automatic Player Sorting:**  
  The queue automatically sorts periodically to prioritize players with the highest points.

- **Automatic Connection:**  
  The player with the most points is automatically connected to the target server.

- **Centralized Commands under `/queueplus`:**  
  - `/queueplus reload` — Reload the configuration dynamically (admin only).  
  - `/queueplus checkpoints <player>` — Check a player's current points.  
  - `/queueplus join` — Join the queue (player command).  
  - `/queueplus leave` — Leave the queue (player command).  
  - `/queueplus addpoints <player> <amount>` — Add points to a player (admin command).  
  - `/queueplus removepoints <player> <amount>` — Remove points from a player (admin command).  
  - `/queueplus setpoints <player> <amount>` — Set exact points for a player (admin command).  
  - `/queueplus info` — Display current queue information and status.

- **Permissions Control:**  
  Administrative commands require `queueplus.admin` or `queueplus.reload` permissions.

- **Player Disconnect Handling:**  
  Automatically removes players from the queue when they disconnect.

- **Lightweight and Efficient:**  
  Runs asynchronous periodic tasks with performance in mind.

## Requirements

- Velocity Proxy version 3.4.0-SNAPSHOT or compatible  
- Java 17 or later

## Installation

1. Download the latest `QueuePlus.jar` plugin file.  
2. Place the JAR into your Velocity `plugins` directory.  
3. Configure the plugin by editing `plugins/queueplus/config.yaml`.  
4. Start or restart your Velocity proxy server.  
5. Use `/queueplus reload` to reload configuration without restarting the server.

## Configuration

The plugin is configured via `plugins/queueplus/config.yaml`. Below is an example configuration:
