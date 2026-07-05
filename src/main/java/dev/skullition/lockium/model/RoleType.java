package dev.skullition.lockium.model;

import dev.skullition.lockium.util.AppEmojis;
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji;

/**
 * A Growtopia "role" (profession) selectable in {@code /gt role}.
 *
 * <p>Each role belongs to a {@link RoleCategory} that determines its base XP and gem costs:
 * supplier roles share one set of values, crafter roles another.
 */
public enum RoleType {
  FISHING("Fisher", RoleCategory.SUPPLIER),
  STAR_CAPTAIN("Star Captain", RoleCategory.SUPPLIER),
  FARMER("Farmer", RoleCategory.SUPPLIER),
  CHEF("Chef", RoleCategory.CRAFTER),
  SURGEON("Surgeon", RoleCategory.SUPPLIER),
  BUILDER("Builder", RoleCategory.CRAFTER);

  private final String roleName;
  private final RoleCategory category;

  RoleType(String roleName, RoleCategory category) {
    this.roleName = roleName;
    this.category = category;
  }

  /**
   * Returns the application emoji used as the container icon.
   *
   * <p>Read on demand; only valid once BotCommands has loaded the app emojis (i.e. during command
   * execution, not during command registration).
   *
   * @return the emoji representing this role
   */
  public ApplicationEmoji getEmoji() {
    return switch (this) {
      case FISHING -> AppEmojis.FISHER;
      case STAR_CAPTAIN -> AppEmojis.STAR_CAPTAIN;
      case FARMER -> AppEmojis.FARMER;
      case CHEF -> AppEmojis.COOK;
      case SURGEON -> AppEmojis.SURGEON;
      case BUILDER -> AppEmojis.BUILDER;
    };
  }

  /** Returns the display name shown in choices and replies, e.g. {@code "Star Captain"}. */
  public String getRoleName() {
    return roleName;
  }

  /** Returns the base XP used by the level-1 requirement of this role's category. */
  public int getBaseXp() {
    return category.baseXp;
  }

  /** Returns the base gem cost of this role's daily quests. */
  public int getBaseGemCost() {
    return category.baseGemCost;
  }

  /** Groups roles by their shared base XP and gem-cost values. */
  private enum RoleCategory {
    SUPPLIER(1300, 3000),
    CRAFTER(1500, 4000);

    private final int baseXp;
    private final int baseGemCost;

    RoleCategory(int baseXp, int baseGemCost) {
      this.baseXp = baseXp;
      this.baseGemCost = baseGemCost;
    }
  }
}
