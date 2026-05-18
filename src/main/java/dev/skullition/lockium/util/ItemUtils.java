package dev.skullition.lockium.util;

import java.util.Locale;

public class ItemUtils {

    public static String norm(String itemName) {
        return itemName.trim().toLowerCase(Locale.ROOT);
    }
}
