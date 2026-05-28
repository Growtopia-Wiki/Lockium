package dev.skullition.lockium.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record GrowtopiaObject(
        int id,
        String name,
        String description,
        int rarity,
        @JsonProperty("category")
        CategoryInfo categoryInfo,
        @JsonProperty("clothingType")
        @Nullable CategoryInfo clothingCategoryInfo,
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
    public String propFlagText() {
        return ItemProperty.toDisplay(propFlag.raw());
    }

    @Nullable
    public String propFlag2Text() {
        return ItemProperty2.toDisplay(propFlag2.raw());
    }

    public ItemCategory getItemCategory() {
        return ItemCategory.fromId(categoryInfo.id()); // e.g. 51 -> BUNNY_EGG
    }
    
    @Nullable
    public ClothingType getClothingType() {
        if (clothingCategoryInfo == null) {
            return null;
        }
        return ClothingType.fromId(clothingCategoryInfo.id());
    }

    public record CategoryInfo(int id, String name, @Nullable String type) {
    }

    public record TypeInfo(int id, @Nullable String name) {
    }

    public record PropFlag(int raw, List<String> names) {
    }

    public record ColorInfo(long raw, @Nullable String hex) {
        public int intOrTransparent() {
            if (hex == null) {
                return 0;
            }
            String h = hex.replace("#", "");
            if (h.length() == 3)
                h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2) + "FF";
            if (h.length() == 4)
                h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2) + h.charAt(3) + h.charAt(3);
            if (h.length() == 6) h = h + "FF"; // assume RRGGBB → add alpha

            // assume input is RRGGBBAA → convert to AARRGGBB
            if (h.length() == 8) {
                h = h.substring(6, 8) + h.substring(0, 6);
            }
            return (int) Long.parseLong(h, 16);
        }
    }
}
