package dev.skullition.lockium.model;

import dev.skullition.lockium.util.AppEmojis;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum ItemProperty {
    MULTI_FACING(0x01, "%s This item can be placed in two directions, depending on the direction you're facing.".formatted(AppEmojis.MULTI_FACING)),
    WRENCHABLE(0x02, "%s This item has special properties you can adjust with the Wrench.".formatted(AppEmojis.WRENCHABLE)),
    NO_SEED(0x04, "%s This item never drops any seeds.".formatted(AppEmojis.NO_SEED)),
    PERMANENT(0x08, null),
    NO_DROP(0x10, "%s This item never drops anything.".formatted(AppEmojis.NO)),
    NO_SELF(0x20, "%s This item can't be used on yourself.".formatted(AppEmojis.NO)),
    NO_SHADOW(0x40, null),
    WORLD_LOCK(0x80, "%s This item can only be used in World-Locked worlds.".formatted(AppEmojis.WORLD_LOCK)),
    BETA(0x100, "%s This item can only be placed in the world BETA.".formatted(AppEmojis.BETA)),
    AUTO_PICKUP(0x200, "%s This item can't be destroyed - smashing it will return it to your backpack if you have room!".formatted(AppEmojis.FIST)),
    MOD(0x400, "%s This item can only be picked up by mods.".formatted(AppEmojis.MOD)),
    RANDOM_GROW(0x800, "%s A tree of this type can bear surprising fruit!".formatted(AppEmojis.TRACTOR)),
    PUBLIC(0x1000, "%s This item is PUBLIC: Even if it's locked, anyone can smash it.".formatted(AppEmojis.GARBAGE)),
    FOREGROUND(0x2000, null),
    HOLIDAY(0x4000, null),
    UNTRADABLE(0x8000, "%s This item cannot be dropped or traded.".formatted(AppEmojis.UNTRADEABLE));

    private final int mask;
    @Nullable
    private final String description;

    ItemProperty(int mask, @Nullable String description) {
        this.mask = mask;
        this.description = description;
    }

    public static EnumSet<ItemProperty> fromInt(int flags) {
        EnumSet<ItemProperty> set = EnumSet.noneOf(ItemProperty.class);
        for (ItemProperty p : values()) {
            if ((flags & p.mask) != 0) set.add(p);
        }
        return set;
    }

    public static String toDisplay(int flags) {
        if (flags == 0) {
            return "This item has no properties.";
        }
        
        var set = fromInt(flags);
        List<String> lines = new ArrayList<>();

        boolean permanent = set.remove(PERMANENT);
        boolean autoPickup = set.remove(AUTO_PICKUP);

        // Classic SethHam spaghetti
        if (permanent) {
            if (autoPickup) {
                lines.add("%s This item can't be destroyed - smashing it will return it to your backpack if you have room!".formatted(AppEmojis.FIST));
            } else {
                lines.add("%s This item can't be destroyed - smashing it will always yield a new one.".formatted(AppEmojis.FIST));
            }
        } else if (autoPickup) {
            // AUTO_PICKUP without PERMANENT – keep its normal meaning
            lines.add("Auto-pickup");
        }

        // everything else is independent
        for (var p : set) {
            if (p.description() != null) lines.add(p.description());
        }

        if (lines.isEmpty()) return "No special properties";
        return "• " +String.join("\n• ", lines);
    }

    @Nullable
    public String description() {
        return description;
    }
}