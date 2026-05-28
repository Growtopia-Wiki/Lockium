package dev.skullition.lockium.model;


import dev.skullition.lockium.util.AppEmojis;
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji;
import org.jspecify.annotations.Nullable;

public enum ClothingType {
    HAT(0, AppEmojis.TOP_HAT, "Hat"),
    SHIRT(1, AppEmojis.GREEN_SHIRT, "Shirt"),
    PANTS(2, AppEmojis.JEANS, "Pants"),
    FEET(3, AppEmojis.BOOTS, "Feet"),
    FACE(4, AppEmojis.SHADES, "Face"),
    HAND(5, AppEmojis.FIST, "Hand"),
    BACK(6, AppEmojis.FAIRY_WINGS, "Back"),
    HAIR(7, AppEmojis.RED_HAIR, "Hair"),
    CHEST(8, AppEmojis.GOLD_CHAIN, "Chest");

    private static final ClothingType[] BY_ID = values();
    private final int id;
    private final ApplicationEmoji icon;
    private final String itemName;

    ClothingType(int id, ApplicationEmoji icon, String itemName) {
        this.id = id;
        this.icon = icon;
        this.itemName = itemName;
    }

    @Nullable
    public static ClothingType fromId(int id) {
        return (id >= 0 && id < BY_ID.length) ? BY_ID[id] : null;
    }

    public ApplicationEmoji getIcon() {
        return icon;
    }

    public String getItemName() {
        return itemName;
    }

}