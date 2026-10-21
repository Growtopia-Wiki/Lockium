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
- `/gt break <name> <count>` – calculate drops from breaking blocks, with modifiers (Lucky! mod, Buddy's Block Head, Galaxy Skin, Winter Wishing Star, Ancestral Tesseract); clothing block bonuses respect the in-game 15% cap, which the Winter Wishing Star bypasses for an effective 17% maximum
- `/gt harvest <name> <count>` – calculate block/seed/gem drops from harvesting trees, with and without a Harvester
- `/gt recycle <name> <count>` – calculate gems received for recycling an item
- `/gt role <role>` – show XP requirements per level and daily quest gem costs for a role
- `/gt wotd` – render today's World of the Day
- `/gt world <name>` – show a world's render from growtopiagame.com, with its last render date
- `/gt stats` – game server stats: server status, online users, today's WOTD, and a graph of the player count over the last 24 hours
- `/gt leaderboard [league]` – the computed overall top 20, or a single one of the 18 league boards
- `/gt auction [item]` – browse auction listings, optionally filtered by item name, with WL bids and estimated timers; Previous/Next keep a stable snapshot, while Refresh reads the latest cached snapshot
- `/gt events` – timers for the Daily Challenge, Night of the Comet, Pet Battle Tournament, and the daily block drop rotation
- `/gt time` – current Growtopia (US Eastern) time
- `/gt startdate <days>` – convert your account's age in days to the date you started playing
- `/gt xp [min] [max]` – XP needed between two levels, with block-breaking equivalents and multipliers
- `/gt mooncakes <name> <count>` – expected mooncake drops from harvesting trees
- `/gt search <query>` – search items by name
- `/gt riddle <description>` – find which block an ancestral riddle requires
- `/gt telephone` – all dialable telephone numbers
- `/gt provider atm <count>` – estimated ATM gem earnings
- `/gt provider tackle <count>` – estimated Tackle Box bait drops
- `/gt provider science <count>` – estimated Science Station chemical drops

Item name options support autocomplete backed by the cached Wiki item index.

There are also owner-only slash commands (`/owner activity`, `/owner guilds`, and `/owner reload`)
in test guilds.

Owner command registration is checked when configured guilds become ready. If Discord's command
IDs or subcommands disagree with the bot's routing, the bot attempts a guild registration repair.
An unresolved owner invocation also requests a check; retry the command after recovery completes.
Checks allow one attempt per guild per minute and log registration IDs and outcomes. They respect
`botcommands.application.testGuildIds` and any `botcommands.application.guildsToUpdate` restriction.

## Tech Stack

- **Java 25** (project is built on the latest JDK)
- Spring Boot 4
- JDA 6 + [BotCommands 3.4](https://github.com/freya022/BotCommands) – annotation-driven slash commands
- Caffeine – in-memory caching of Wiki API data
- H2 (in-memory) + Flyway – player-count history; the schema is shared with BotCommands
- XChart – renders the player-count graph
- Maven

Code follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

## Testing

The default suite is offline and deterministic. It calls each slash-command handler directly,
captures the JDA reply (including Components V2 payloads, modals, attachments, ephemeral state, and
allowed mentions), and compares successful responses with snapshots in
`src/test/resources/snapshots`.

```bash
./mvnw test
```

When an intentional command response change makes a snapshot fail, review the output and rewrite
the snapshots explicitly:

```bash
./mvnw test -DupdateSnapshots=true
```

Run the same formatting and style checks as CI with:

```bash
./mvnw verify
```

To apply Google Java Format before verifying:

```bash
./mvnw spotless:apply
```

The live Spring context smoke test remains separate because it connects the test bot to Discord and
the Wiki API:

```bash
./mvnw test -Dtest=LockiumApplicationIT
```

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
| `lockium.proxy.auction.enabled` | `true` | Enable shared auction polling (also controls owner reload) |
| `lockium.proxy.auction.poll-interval` | `1m` | Auction polling interval |
| `lockium.proxy.auction.max-age` | `10m` | Maximum snapshot age measured from the proxy's `fetched_at` |
| `lockium.proxy.auction.max-backoff` | `10m` | Maximum retry delay after failed auction refreshes |

Auction browsing is read-only. Only the person who opened a view can use its controls. Views expire
after ten minutes, or sooner if their snapshot reaches the maximum age. Timers are estimates;
an elapsed timer does not establish the auction's outcome. Local proxy access may receive a WAF
403; polling backs off and the command reports unavailable data or a retained snapshot's age.

## Support
Join the Growtopia Wiki server: https://discord.gg/VYK9tuQQQE
