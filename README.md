# 🗺️ TreasureHunt Plugin

A lightweight, database-driven treasure hunt system for Minecraft servers.  
Designed for multi-server setups where treasure claims are synchronized in real time via MySQL.

---

## ⚙️ Features

- ✅ **Persistent treasures** stored in MySQL  
- 🔄 **Cross-server claim synchronization** (instant)  
- 📦 **Treasure creation, listing, deletion, and claim history**  
- 🧭 **GUI-based management** (create, delete, completed players)  
- 🕒 **Claim timestamps** displayed in GUI and console  
- 🚀 **Async database operations** (no main-thread lag)  
- 🧱 **Safe cache system** for local treasure data  
- 🧩 **Full message customization** via `messages.yml`

---

## 🪄 Commands

| Command | Description | Permission |
|----------|-------------|-------------|
| `/tc create <id> <command>` | Create a treasure (right-click a block within 120s) | `albis.treasure.create` |
| `/tc delete <id>` | Delete a treasure via GUI confirmation | `albis.treasure.delete` |
| `/tc list [page]` | List all registered treasures (GUI + console) | `albis.treasure.list` |
| `/tc completed <id> [page]` | Show players who claimed a treasure (GUI + console) | `albis.treasure.completed` |

> 💡 The command inside the treasure will execute **as console**, with `%player%` replaced by the claiming player’s name.

---

## 🧰 Permissions

| Permission Node | Description |
|------------------|-------------|
| `albis.treasure.create` | Allows creating new treasures |
| `albis.treasure.delete` | Allows deleting treasures |
| `albis.treasure.list` | Allows listing all treasures |
| `albis.treasure.completed` | Allows viewing completed player lists |

---

## ⚙️ Configuration

**File:** `plugins/TreasureHunt/config.yml`

```yaml
database:
  host: localhost
  port: 3306
  database: treasurehunt
  user: root
  password: ""
  pool-size: 10
  useSsl: false
```

## 💾 Database Schema

Automatically created on first startup:

```sql
CREATE TABLE treasures (
  id VARCHAR(128) PRIMARY KEY,
  world VARCHAR(128) NOT NULL,
  x INT NOT NULL,
  y INT NOT NULL,
  z INT NOT NULL,
  command TEXT NOT NULL
);

CREATE TABLE treasure_claims (
  player_uuid CHAR(36) NOT NULL,
  treasure_id VARCHAR(128) NOT NULL,
  claimed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (player_uuid, treasure_id),
  FOREIGN KEY (treasure_id) REFERENCES treasures(id) ON DELETE CASCADE
);
```

---

## 🔄 Multi-Server Behavior

| Action                      | Sync behavior |
|-----------------------------|----------------|
| **Treasure creation/delete** | Requires a server restart to appear on other servers. |
| **Treasure claim**    | Instantly synchronized through MySQL (real-time). |
| **Claim check**             | Database-level atomic insert prevents double claiming. |

---

---

## 🛡️ Requirements

- Minecraft **1.20+** (Paper/Spigot)  
- Java **17+**  
- MySQL **5.7+** or **MariaDB**  
- Permission plugin *(optional, e.g., LuckPerms)*

---
## 👤 Author

**João Alberis (Albis / MrJoao)**  
**Project:** TreasureHunt  
**Language:** Java (Spigot/Paper 1.20)  
**Version:** 1.0.0

---

## 📜 License

This project is licensed under the **MIT License**.  
You are free to use, modify, and distribute this plugin, provided that proper credit is given to the original author.

© 2025 João Alberis

