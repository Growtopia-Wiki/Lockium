package dev.skullition.lockium.model;

import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.Collectors;

public enum ItemProperty {
    MULTI_FACING(0x01, "This item can be placed in two directions, depending on the direction you're facing."),
    WRENCHABLE(0x02, "This item has special properties you can adjust with the Wrench."),
    NO_SEED(0x04, "This item never drops any seeds."),
    PERMANENT(0x08, null),
    NO_DROP(0x10, "This item never drops anything."),
    NO_SELF(0x20, "This item can't be used on yourself."),
    NO_SHADOW(0x40, null),
    WORLD_LOCK(0x80, "This item can only be used in World-Locked worlds."),
    BETA(0x100, "This item can only be placed in the world BETA."),
    AUTO_PICKUP(0x200, "This item can't be destroyed - smashing it will return it to your backpack if you have room!"),
    MOD(0x400, "This item can only be picked up by mods."),
    RANDOM_GROW(0x800, "A tree of this type can bear surprising fruit!"),
    PUBLIC(0x1000, "This item is PUBLIC: Even if it's locked, anyone can smash it."),
    FOREGROUND(0x2000, null),
    HOLIDAY(0x4000, null),
    UNTRADABLE(0x8000, "This item cannot be dropped or traded.");

    private final int mask;
    @Nullable
    private final String description;

    ItemProperty(int mask, @Nullable String description) {
        this.mask = mask;
        this.description = description;
    }

    @Nullable
    public String description() { return description; }

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
        return fromInt(flags).stream()
                .map(ItemProperty::description)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n• ", "• ", ""));
    }
}