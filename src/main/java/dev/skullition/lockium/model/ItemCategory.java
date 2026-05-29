package dev.skullition.lockium.model;


import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum ItemCategory {
    FIST(0, "Fist", null),
    WRENCH(1, "Wrench", null),
    DOORS(2, "Door", "Doors"),
    LOCKS(3, "World Lock", "Locks"),
    GEMS(4, "Gems", null),
    TREASURE_BLOCKS(5, "Mystery Block", "Treasure Blocks"),
    DEADLY_BLOCKS(6, "Death Spikes", "Deadly Blocks"),
    TRAMPOLINE_BLOCKS(7, "Mushroom", "Trampoline Blocks"),
    CONSUMABLES(8, "Blueberry", "Consumables"),
    ENTRANCES(9, "House Entrance", "Entrances"),
    SIGNS(10, "Sign", "Signs"),
    SFX_FOREGROUND_BLOCKS(11, "Toilet", "SFX Foreground Blocks"),
    TOGGLEABLE_ANIMATED_FOREGROUND_BLOCKS(12, "Note Block", "Toggleable Animated Foreground Blocks"),
    MAIN_DOOR(13, "Main Door", null),
    PLATFORMS(14, "Wooden Platform", "Platforms"),
    BEDROCK(15, "Bedrock", null),
    PAIN_BLOCKS_LAVA(16, "Lava", "Pain Blocks (Lava)"),
    DIRT(17, "Dirt", null),
    CAVE_BACKGROUND(18, "Cave Background", null),
    DIRT_SEED(19, "Dirt Seed", null),
    CLOTHES(20, null, "Clothes"), // uses ClothingType
    ANIMATED_FOREGROUND_BLOCKS(21, "Evil Bricks", "Animated Foreground Blocks"),
    SFX_BACKGROUND_BLOCKS(22, "Bubble Wrap", "SFX Background Blocks"),
    TOGGLEABLE_BACKGROUND_BLOCKS(23, "Art Wall", "Toggleable Background Blocks"),
    BOUNCY_BLOCKS(24, "Pinball Bumper", "Bouncy Blocks"),
    PAIN_BLOCKS_SPIKE(25, "Carnival Spikeball", "Pain Blocks (Spike)"),
    PORTALS(26, "Time-Space Rupture", "Portals"),
    CHECKPOINTS(27, "Checkpoint", "Checkpoints"),
    SHEET_MUSIC(28, "Sheet Music: Piano Note", "Sheet Music"),
    SLIPPERY_BLOCKS(29, "Ice", "Slippery Blocks"),
    // 30 unused
    TOGGLEABLE_BLOCKS(31, "Dragon Gate", "Toggleable Blocks"),
    CHESTS(32, "Treasure Chest", "Chests"),
    MAILBOXES(33, "Mailbox", "Mailboxes"),
    BULLETIN_BOARDS(34, "Bulletin Board", "Bulletin Boards"),
    EVENT_MYSTERY_BLOCKS(35, "Pinata", "Event Mystery Blocks"),
    RANDOM_BLOCKS(36, "Dice Block", "Random Blocks"),
    COMPONENTS(37, "Dough", "Components"),
    PROVIDERS(38, "Chicken", "Providers"),
    CHEMICAL_COMBINERS(39, "E-Z Cook Oven", "Chemical Combiners"),
    ACHIEVEMENT_BLOCK(40, "Achievement Block", null),
    WEATHER_MACHINES(41, "Weather Machine - Sunny", "Weather Machines"),
    SCOREBOARD(42, "Scoreboard", null),
    SUNGATE(43, "Sungate", null),
    BLANK(44, "Blank", null),
    TOGGLEABLE_DEADLY_BLOCKS(45, "Evil Eye", "Toggleable Deadly Blocks"),
    HEART_MONITOR(46, "Heart Monitor", null),
    DONATION_BOXES(47, "Donation Box", "Donation Boxes"),
    // 48 unused
    MANNEQUINS(49, "Mannequin", "Mannequins"),
    SECURITY_CAMERAS(50, "Security Camera", "Security Cameras"),
    BUNNY_EGG(51, "Bunny Egg", null),
    GAME_BLOCKS(52, "Game Block", "Game Blocks"),
    GAME_GENERATOR(53, "Game Generator", null),
    XENONITE_CRYSTAL(54, "Xenonite Crystal", null),
    PHONE_BOOTH(55, "Phone Booth", null),
    CRYSTALS(56, "Blue Crystal", "Crystals"),
    CRIME_IN_PROGRESS(57, "Crime In Progress", null),
    CLOTHING_COMPACTOR(58, "Clothing Compactor", null),
    SPOTLIGHT(59, "Spotlight", null),
    PUSHING_BLOCKS(60, "Summer Breeze", "Pushing Blocks"),
    DISPLAY_BLOCK(61, "Display Block", null),
    VENDING_MACHINE(62, "Vending Machine", null),
    FISH_TANK_PORT(63, "Fish Tank Port", null),
    FISHES(64, "Goldfish", "Fishes"),
    SOLAR_COLLECTOR(65, "Solar Collector", null),
    FORGE(66, "Forge", null),
    GIVING_TREE(67, "Giving Tree", null),
    GIVING_TREE_STUMP(68, "Giving Tree Stump", null),
    STEAM_TUBES(69, "Steam Tubes", null),
    STEAM_VENT(70, "Steam Vent", null),
    STEAM_ORGAN(71, "Steam Organ", null),
    SILKWORM(72, "Silkworm", null),
    SEWING_MACHINE(73, "Sewing Machine", null),
    COUNTRY_FLAG(74, "Country Flag", null),
    LOBSTER_TRAP(75, "Lobster Trap", null),
    PAINTING_EASEL(76, "Painting Easel", null),
    BATTLE_PET_CAGE(77, "Battle Pet Cage", null),
    PET_TRAINER(78, "Pet Trainer", null),
    STEAM_ENGINE(79, "Steam Engine", null),
    LOCK_BOT(80, "Lock-Bot", null),
    WEATHER_MACHINES_81(81, "Weather Machine - Sunny", "Weather Machines"),
    SPIRIT_STORAGE_UNIT(82, "Spirit Storage Unit", null),
    DISPLAY_SHELF(83, "Display Shelf", null),
    VIP_ENTRANCE(84, "VIP Entrance", null),
    CHALLENGE_TIMER(85, "Challenge Timer", null),
    CHALLENGE_START_FLAG(86, "Challenge Start Flag", null),
    FISH_WALL_MOUNT(87, "Fish Wall Mount", null),
    PORTRAIT(88, "Portrait", null),
    WEATHER_MACHINES_89(89, "Weather Machine - Sunny", "Weather Machines"),
    FOSSIL_ROCK(90, "Fossil Rock", null),
    FOSSIL_PREP_STATION(91, "Fossil Prep Station", null),
    DNA_PROCESSOR(92, "DNA Processor", null),
    HOWLERS(93, "Howler", "Howlers"),
    VALHOWLA_TREASURE(94, "Valhowla Treasure", null),
    CHEMSYNTH_PROCESSOR(95, "Chemsynth Processor", null),
    CHEMSYNTH_TANK(96, "Chemsynth Tank", null),
    STORAGE_BOX_XTREME(97, "Storage Box Xtreme - Level 1", null),
    COOKING_OVENS(98, "Home Oven", "Cooking Ovens"),
    AUDIO_GEAR(99, "Audio Gear", null),
    GEIGER_CHARGER(100, "Geiger Charger", null),
    ADVENTURE_BEGIN(101, "Adventure Begin", null),
    TOMB_ROBBER(102, "Tomb Robber", null),
    BALLOON_O_MATIC(103, "Balloon-O-Matic", null),
    ENTRANCES_PUNCH(104, "Team Entrance - Punch", "Entrances"),
    ENTRANCES_GROW(105, "Team Entrance - Grow", "Entrances"),
    ENTRANCES_BUILD(106, "Team Entrance - Build", "Entrances"),
    ARTIFACTS(107, "Ancestral Tesseract of Dimensions", "Artifacts"),
    JELLY_BLOCKS(108, "Lemon Jelly Block", "Jelly Blocks"),
    TRAINING_PORT(109, "Training Port", null),
    URANIUM_BLOCK(110, "Uranium Block", null),
    MAGPLANT_5000(111, "MAGPLANT 5000", null),
    MAGPLANT_5000_REMOTE(112, "MAGPLANT 5000 Remote", null),
    CYBLOCK_BOTS(113, "ShockBot - Level 1", "CyBlock Bots"),
    CYBLOCK_COMMANDS(114, "Command - Move Right", "CyBlock Commands"),
    LUCKY_TOKENS(115, "Lucky Token", "Lucky Tokens"),
    GROWSCAN_9000(116, "GrowScan 9000", null),
    CONTAINMENT_FIELD_POWER_NODE(117, "Containment Field Power Node", null),
    SPIRIT_BOARD(118, "Spirit Board", null),
    WORLD_ARCHITECT(119, "World Architect", null),
    STARTOPIA_STARSHIP_BLOCKS(120, "Imperial Starship Helm - Mk.I", "Startopia Starship Blocks"),
    // 121 unused
    TOGGLEABLE_MULTI_FRAMED_ANIMATED_FOREGROUND_BLOCKS(122, "Gravity Well", "Toggleable Multi-Framed Animated Foreground Blocks"),
    AUTOBREAKING_BLOCKS_1(123, "Tesseract Manipulator", "Autobreaking Blocks"),
    AUTOBREAKING_BLOCKS_2(124, "Heart of Gaia", "Autobreaking Blocks"),
    AUTOBREAKING_BLOCKS_3(125, "Techno-Organic Engine", "Autobreaking Blocks"),
    STORM_CLOUD(126, "Storm Cloud", null),
    CRACKED_STONE_SLAB(127, "Cracked Stone Slab", null),
    PUDDLE_BLOCKS(128, "Mud Puddle", "Puddle Blocks"),
    COMPONENTS_2(129, "Bountiful Jungle Temple Background Root Cutting", "Components"),
    SAFE_VAULT(130, "Safe Vault", null),
    ANGELIC_COUNTING_CLOUD(131, "Angelic Counting Cloud", null),
    MINING_EXPLOSIVES(132, "Mining Explosives", null),
    // 133 unused
    INFINITY_WEATHER_MACHINE(134, "Infinity Weather Machine", null),
    GHOST_BLOCK(135, "Ghost Block", null),
    PAIN_BLOCKS_ACID(136, "Acid", "Pain Blocks (Acid)"),
    // 137 unused
    WAVING_INFLATABLE_ARM_GUY(138, "Waving Inflatable Arm Guy", null),
    // 139 unused
    PINEAPPLE_GUZZLER(140, "Pineapple Guzzler", null),
    KRANKENS_PATTERN_10(141, "Krankens Pattern 10", null),
    FRIENDS_ENTRANCE(142, "Friends Entrance", null),
    // 143 unused
    CONVEYOR_BELT(144, "Conveyor Belt", null),
    CLIMBING_ROCKS(145, "Climbing Rocks", null),
    UNKNOWN(-1, "Unknown", "Unknown");

    private static final Map<Integer, ItemCategory> BY_ID;

    static {
        Map<Integer, ItemCategory> map = new HashMap<>();
        for (ItemCategory c : values()) {
            if (c.id >= 0) {          // skip the -1 sentinel
                map.put(c.id, c);
            }
        }
        BY_ID = Collections.unmodifiableMap(map);
    }

    private final int id;
    private final @Nullable String icon;
    private final @Nullable String itemName;

    ItemCategory(int id, @Nullable String icon, @Nullable String itemName) {
        this.id = id;
        this.icon = icon;
        this.itemName = itemName;
    }

    public static ItemCategory fromId(int id) {
        return BY_ID.getOrDefault(id, UNKNOWN);
    }

    public int id() {
        return id;
    }

    public String getDisplayText() {
        if (itemName != null) {
            return itemName;
        } else if (icon != null) {
            return icon;
        }
        return "Category " + id;
    }

}