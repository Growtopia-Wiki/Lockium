package dev.skullition.lockium.command;

import dev.skullition.lockium.model.GrowtopiaObject;
import dev.skullition.lockium.model.GrowtopiaObject.CategoryInfo;
import dev.skullition.lockium.model.GrowtopiaObject.ColorInfo;
import dev.skullition.lockium.model.GrowtopiaObject.PropFlag;
import dev.skullition.lockium.model.GrowtopiaObject.TypeInfo;
import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import java.util.List;

/** Stable API objects shared by command response tests. */
final class CommandFixtures {
  static final ItemCatalogue DIRT_CATALOGUE = new ItemCatalogue(1, 2, 3, "Dirt", "");
  static final ItemCatalogue COMET_DUST_CATALOGUE =
      new ItemCatalogue(4, 5, 6, "Comet Dust", "Starseed");

  private CommandFixtures() {}

  static ItemDetailResponse dirtDetail() {
    GrowtopiaObject item = object(2, "Dirt", "It's dirt.", 1_800);
    GrowtopiaObject seed = object(3, "Dirt Seed", "A dirt seed.", 1_800);
    return new ItemDetailResponse(item, seed);
  }

  static ItemDetailResponse cometDustDetail() {
    GrowtopiaObject item = object(5, "Comet Dust", "Dust from a comet.", 3_600);
    GrowtopiaObject seed = object(6, "Starseed", "A seed from the stars.", 3_600);
    return new ItemDetailResponse(item, seed);
  }

  /** Creates a rarityless item whose game ID can differ from its catalogue index. */
  static ItemDetailResponse raritylessDetail(int itemId, String name) {
    return new ItemDetailResponse(
        object(itemId, name, "An item without rarity.", 0, 999),
        object(itemId + 1, name + " Seed", "A seed.", 0, 999));
  }

  private static GrowtopiaObject object(int id, String name, String description, int growTime) {
    return object(id, name, description, growTime, 20);
  }

  private static GrowtopiaObject object(
      int id, String name, String description, int growTime, int rarity) {
    return new GrowtopiaObject(
        id,
        name,
        description,
        rarity,
        new CategoryInfo(17, "Dirt", null),
        null,
        new TypeInfo(0, "Solid"),
        new TypeInfo(0, "Default"),
        new PropFlag(0, List.of()),
        new PropFlag(0, List.of()),
        12,
        null,
        4,
        growTime,
        0,
        0,
        new ColorInfo(0x7A4E35FFL, "#7A4E35FF"),
        new ColorInfo(0x5C3927FFL, "#5C3927FF"),
        null,
        null);
  }
}
