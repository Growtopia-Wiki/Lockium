package dev.skullition.lockium.model;

import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Objects;

public enum ItemProperty2 {
    ROBOT_DEADLY(0x1, null),
    ROBOT_SHOOT_LEFT(0x2, null),
    ROBOT_SHOOT_RIGHT(0x4, null),
    ROBOT_SHOOT_DOWN(0x8, null),
    ROBOT_SHOOT_UP(0x10, null),
    ROBOT_CAN_SHOOT(0x20, null),
    ROBOT_LAVA(0x40, null),
    ROBOT_POINTY(0x80, null),
    ROBOT_SHOOT_DEADLY(0x100, null),
    GUILD_ITEM(0x200, null),
    GUILD_FLAG(0x400, null),
    STARSHIP_HELM(0x800, null),
    STARSHIP_REACTOR(0x1000, null),
    STARSHIP_VIEWSCREEN(0x2000, null),
    SMOD(0x4000, null),
    TILE_DEADLY_IF_ON(0x8000, null),
    LONG_HAND_ITEM64x32(0x10000, null),
    GEMLESS(0x20000, "This item never drops gems."),
    TRANSMUTABLE(0x40000, "This item can be transmuted.");

    private final int mask;
    @Nullable
    private final String description;

    ItemProperty2(int mask, @Nullable String description) {
        this.mask = mask;
        this.description = description;
    }

    @Nullable
    public String description() { return description; }

    public static EnumSet<ItemProperty2> fromInt(int flags) {
        EnumSet<ItemProperty2> set = EnumSet.noneOf(ItemProperty2.class);
        for (var p : values()) if ((flags & p.mask) != 0) set.add(p);
        return set;
    }

    @Nullable
    public static String toDisplay(int flags) {
        if (flags == 0) {
            return null;
        }
        return fromInt(flags).stream()
                .map(ItemProperty2::description)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.joining("\n• ", "\n• ", ""));
    }
}