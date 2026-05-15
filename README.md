# Lockium

> The official Discord companion for the Growtopia Wiki.

[![Java](https://img.shields.io/badge/Java-25%2B-orange)](https://www.java.com/en/download/manual.jsp)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F)](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![Discord](https://discord.com/api/guilds/1483908386348208223/widget.png)](https://discord.gg/VYK9tuQQQE)

Lockium connects Discord directly to the Growtopia Wiki's internal API. Instead of opening a browser, users can run slash commands to pull items, worlds, and recipes in-chat.

---

## Features

- `/ping` – shows Discord gateway latency and Wiki API latency
- `/item <name>` – fetch wiki data for an item *(in development)*
- `/world <name>` – fetch world info *(planned)*
- `/recipe <item>` – show crafting recipe *(planned)*

Early alpha – expect breaking changes.

## Tech Stack

- **Java 25** (project is built on the latest JDK)
- Spring Boot 3
- JDA 5 + [BotCommands 3.1](https://github.com/freya022/BotCommands) – annotation-driven slash commands
- Maven

## Requirements

- JDK 25+
- Maven 3.9+
- A Discord bot token
- A Growtopia Wiki API key (public API, key required)

## Quick Start

1. Clone
   ```bash
   git clone https://github.com/Growtopia-Wiki/Lockium.git
   cd Lockium
   ```
2. Create secrets.properties in the project root (this file is gitignored):
   ```properties
   # Discord
   discord.token=YOUR_DISCORD_BOT_TOKEN

   # Growtopia Wiki API
   growtopia.api.key=YOUR_WIKI_API_KEY
   growtopia.api.url=https://api.growtopiawiki.com
   ```
3. Run locally
   ```bash
   ./mvnw spring-boot:run
   ```

## Support
Join the Growtopia Wiki server: https://discord.gg/VYK9tuQQQE