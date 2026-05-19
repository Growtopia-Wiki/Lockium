package dev.skullition.lockium.model;


import org.jspecify.annotations.Nullable;

import java.util.List;

public record GrowtopiaObject(
        int id,
        String name,
        String description,
        int rarity,
        CategoryInfo category,
        @Nullable CategoryInfo clothingType,
        TypeInfo collisionType,
        TypeInfo textureType,
        PropFlag propFlag,
        PropFlag propFlag2,
        int hardness,
        @Nullable Integer cookTime,
        int restoreTime,
        int growTime,
        int baseStyle,
        int overStyle,
        ColorInfo baseColor,
        ColorInfo overColor
) {
    public record CategoryInfo(int id, String name, @Nullable String type) {}
    public record TypeInfo(int id, @Nullable String name) {}
    public record PropFlag(int raw, List<String> names) {}
    public record ColorInfo(long raw, @Nullable String hex) {
        public String hexOrTransparent() {
            return hex != null ? hex : "#00000000";
        }
    }
}
