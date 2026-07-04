# Lockium

> The official Discord companion for the Growtopia Wiki.

[![Java](https://img.shields.io/badge/Java-25%2B-orange)](https://www.java.com/en/download/manual.jsp)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.x-6DB33F)](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![Discord](https://discord.com/api/guilds/1483908386348208223/widget.png)](https://discord.gg/VYK9tuQQQE)

Lockium connects Discord directly to the Growtopia Wiki's internal API. Instead of opening a browser, users can run slash commands to pull items, worlds, and recipes in-chat.

---

## Commands

All commands work in servers, DMs, and as a user-install app.

- `/ping` – shows Discord gateway latency and Wiki API latency
- `/gt item <name>` – look up an item: properties, category, rarity, hardness, colors, grow time, and gem drops
- `/gt sprite <name>` – show an item's block, seed, and tree sprites
- `/gt break <name> <count>` – calculate drops from breaking blocks, with modifiers (Lucky! mod, Buddy's Block, Ancestral Tesseract)
- `/gt harvest <name> <count>` – calculate block/seed/gem drops from harvesting trees, with and without a Harvester
- `/gt recycle <name> <count>` – calculate gems received for recycling an item
- `/gt role <role>` – show XP requirements per level and daily quest gem costs for a role
- `/gt wotd` – render today's World of the Day
- `/gt world <name>` – show a world's render from growtopiagame.com, with its last render date
- `/gt stats` – game server stats: server status, online users, and today's WOTD
- `/gt events` – timers for the Daily Challenge, Night of the Comet, Pet Battle Tournament, and the daily block drop rotation
- `/gt time` – current Growtopia (US Eastern) time
- `/gt startdate <days>` – convert your account's age in days to the date you started playing
- `/gt xp [min] [max]` – XP needed between two levels, with block-breaking equivalents and multipliers
- `/gt mooncakes <name> <count>` – expected mooncake drops from harvesting trees
- `/gt search <query>` – search items by name
- `/gt telephone` – all dialable telephone numbers
- `/gt provider atm <count>` – estimated ATM gem earnings
- `/gt provider tackle <count>` – estimated Tackle Box bait drops
- `/gt provider science <count>` – estimated Science Station chemical drops

Item name options support autocomplete backed by the cached Wiki item index.

There are also owner-only text commands (`activity`, `reload`), invoked by mentioning the bot.

## Tech Stack

- **Java 25** (project is built on the latest JDK)
- Spring Boot 4
- JDA 6 + [BotCommands 3.2](https://github.com/freya022/BotCommands) – annotation-driven slash commands
- Caffeine – in-memory caching of Wiki API data
- Maven

Code follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

## Requirements

- JDK 25+
- Maven 3.9+ (or use the included Maven wrapper)
- A Discord bot token
- A Growtopia Wiki API key (public API, key required)

## Quick Start

1. Clone
   ```bash
   git clone https://github.com/Growtopia-Wiki/Lockium.git
   cd Lockium
   ```
2. Create `src/main/resources/config/secrets.properties` (this file is gitignored):
   ```properties
   # Discord
   discord.token=YOUR_DISCORD_BOT_TOKEN

   # Growtopia Wiki API
   wiki.api.key=YOUR_WIKI_API_KEY
   ```
3. Run locally
   ```bash
   ./mvnw spring-boot:run
   ```

## Docker

Build and run the container image:

```bash
docker build -t lockium .
docker run -e DISCORD_TOKEN=YOUR_DISCORD_BOT_TOKEN -e WIKI_API_KEY=YOUR_WIKI_API_KEY lockium
```

Secrets are passed as environment variables; Spring Boot's relaxed binding maps `DISCORD_TOKEN` → `discord.token` and `WIKI_API_KEY` → `wiki.api.key`.

## Configuration

Defaults live in `src/main/resources/application.properties` and can be overridden per environment:

| Property                       | Default                         | Description                       |
| ------------------------------ | ------------------------------- | --------------------------------- |
| `lockium.status`               | `Enjoy your day!`               | Custom Discord activity status    |
| `lockium.items-cache-duration` | `1h`                            | TTL for the cached Wiki item list |
| `wiki.api.url`                 | `https://api.growtopiawiki.com` | Wiki API base URL                 |

## Support
Join the Growtopia Wiki server: https://discord.gg/VYK9tuQQQE
