package dev.skullition.lockium.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.client.GrowtopiaWikiClient;
import dev.skullition.lockium.model.GrowtopiaObject;
import dev.skullition.lockium.model.ItemCategory;
import dev.skullition.lockium.model.ItemEffect;
import dev.skullition.lockium.properties.LockiumProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Tests loading, scraping, persistence, and miss behavior in {@link ItemEffectService}. */
class ItemEffectServiceTests {
  private static final String PICKAXE_WIKITEXT =
      """
      ==Function==
      {{Added|You can smash bricks more quickly.}}
      {{Removed|Smash time is over.}}
      {{Item/Mod|Enhanced Digging|Punch Damage}}
      """;

  @TempDir Path tempDirectory;

  private static GrowtopiaObject item(int id, String name, ItemCategory category) {
    GrowtopiaObject item = mock(GrowtopiaObject.class);
    when(item.id()).thenReturn(id);
    when(item.name()).thenReturn(name);
    when(item.getItemCategory()).thenReturn(category);
    return item;
  }

  @Test
  void parsesEffectTemplatesAndMissingMessages() {
    String wikitext =
        """
        {{ Added |Applied first.}}
        {{Item/Mod|First Effect|Other}}
        {{item/mod|Second Effect|Other}}
        {{ Removed |Removed first.}}
        """;

    assertEquals(
        List.of(
            new ItemEffect("First Effect", "Applied first.", "Removed first."),
            new ItemEffect("Second Effect", "", "")),
        ItemEffectService.parseWikitext(wikitext));
  }

  @Test
  void loadsSeedThenAddsDistinctOverlayEffects() throws Exception {
    Path overlay = tempDirectory.resolve("ScrapedEffects.txt");
    Files.writeString(
        overlay,
        """
                    98|Enhanced Digging|You can smash bricks more quickly.|Smash time is over.
                    98|Additional Effect|Applied.|Removed.|ignored
                    """,
        StandardCharsets.UTF_8);
    GrowtopiaWikiClient wikiClient = mock(GrowtopiaWikiClient.class);
    ItemEffectService service = createService(wikiClient, overlay);

    service.load();

    assertEquals(1_053, service.size());
    assertEquals(
        List.of(
            new ItemEffect(
                "Enhanced Digging", "You can smash bricks more quickly.", "Smash time is over."),
            new ItemEffect("Additional Effect", "Applied.", "Removed.")),
        service.getEffects(item(98, "Pickaxe", ItemCategory.CLOTHES)));
    assertEquals(
        "",
        service
            .getEffects(item(3_274, "T-Shirt Cannon", ItemCategory.CLOTHES))
            .getFirst()
            .removeMessage());
    assertEquals(
        "Meee-oww!",
        service
            .getEffects(item(6_338, "Eye of the Tiger", ItemCategory.CLOTHES))
            .getFirst()
            .removeMessage());
    verifyNoInteractions(wikiClient);
  }

  @Test
  void successfulScrapePersistsAndCachesEffect() throws Exception {
    Path overlay = tempDirectory.resolve("nested/ScrapedEffects.txt");
    GrowtopiaWikiClient wikiClient = mock(GrowtopiaWikiClient.class);
    when(wikiClient.getRawPage("Uncatalogued_Pickaxe")).thenReturn(PICKAXE_WIKITEXT);
    ItemEffectService service = createService(wikiClient, overlay);
    service.load();
    GrowtopiaObject item = item(200_000, "Uncatalogued Pickaxe", ItemCategory.CLOTHES);

    assertTrue(service.requiresScrape(item));
    List<ItemEffect> first = service.getEffects(item);
    List<ItemEffect> second = service.getEffects(item);

    assertEquals(
        List.of(
            new ItemEffect(
                "Enhanced Digging", "You can smash bricks more quickly.", "Smash time is over.")),
        first);
    assertEquals(first, second);
    assertFalse(service.requiresScrape(item));
    assertEquals(
        "200000|Enhanced Digging|You can smash bricks more quickly.|Smash time is over.\n",
        Files.readString(overlay, StandardCharsets.UTF_8));
    verify(wikiClient).getRawPage("Uncatalogued_Pickaxe");
  }

  @Test
  void definitiveMissIsNegativeCached() {
    Path overlay = tempDirectory.resolve("ScrapedEffects.txt");
    GrowtopiaWikiClient wikiClient = mock(GrowtopiaWikiClient.class);
    when(wikiClient.getRawPage("Missing_Item"))
        .thenThrow(
            new RestClientResponseException(
                "Not Found",
                HttpStatus.NOT_FOUND,
                "Not Found",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8));
    ItemEffectService service = createService(wikiClient, overlay);
    service.load();
    GrowtopiaObject item = item(200_002, "Missing Item", ItemCategory.CONSUMABLES);

    assertTrue(service.getEffects(item).isEmpty());
    assertTrue(service.getEffects(item).isEmpty());

    assertFalse(service.requiresScrape(item));
    verify(wikiClient).getRawPage("Missing_Item");
  }

  @Test
  void pageWithoutModIsNegativeCached() {
    Path overlay = tempDirectory.resolve("ScrapedEffects.txt");
    GrowtopiaWikiClient wikiClient = mock(GrowtopiaWikiClient.class);
    when(wikiClient.getRawPage("Effectless_Item")).thenReturn("{{Item|No mod here.}}");
    ItemEffectService service = createService(wikiClient, overlay);
    service.load();
    GrowtopiaObject item = item(200_003, "Effectless Item", ItemCategory.CONSUMABLES);

    assertTrue(service.getEffects(item).isEmpty());
    assertTrue(service.getEffects(item).isEmpty());

    assertFalse(service.requiresScrape(item));
    verify(wikiClient).getRawPage("Effectless_Item");
  }

  @Test
  void transientFailureRetriesOnNextLookup() {
    Path overlay = tempDirectory.resolve("ScrapedEffects.txt");
    GrowtopiaWikiClient wikiClient = mock(GrowtopiaWikiClient.class);
    when(wikiClient.getRawPage("Retry_Item"))
        .thenThrow(new RestClientException("Temporary failure"));
    ItemEffectService service = createService(wikiClient, overlay);
    service.load();
    GrowtopiaObject item = item(200_004, "Retry Item", ItemCategory.CLOTHES);

    assertTrue(service.getEffects(item).isEmpty());
    assertTrue(service.getEffects(item).isEmpty());

    assertTrue(service.requiresScrape(item));
    verify(wikiClient, times(2)).getRawPage("Retry_Item");
  }

  @Test
  void ineligibleCategoryDoesNotScrape() {
    Path overlay = tempDirectory.resolve("ScrapedEffects.txt");
    GrowtopiaWikiClient wikiClient = mock(GrowtopiaWikiClient.class);
    ItemEffectService service = createService(wikiClient, overlay);
    service.load();
    GrowtopiaObject item = item(200_006, "Ordinary Block", ItemCategory.DIRT);

    assertTrue(service.getEffects(item).isEmpty());
    assertFalse(service.requiresScrape(item));
    verifyNoInteractions(wikiClient);
  }

  @Test
  void appendSeparatesAnExistingUnterminatedRow() throws Exception {
    Path overlay = tempDirectory.resolve("ScrapedEffects.txt");
    String existing = "200010|Existing Effect|Applied.|Removed.";
    Files.writeString(overlay, existing, StandardCharsets.UTF_8);
    GrowtopiaWikiClient wikiClient = mock(GrowtopiaWikiClient.class);
    when(wikiClient.getRawPage("New_Item")).thenReturn(PICKAXE_WIKITEXT);
    ItemEffectService service = createService(wikiClient, overlay);
    service.load();

    service.getEffects(item(200_012, "New Item", ItemCategory.CLOTHES));

    assertEquals(
        existing
            + "\n200012|Enhanced Digging|You can smash bricks more quickly.|"
            + "Smash time is over.\n",
        Files.readString(overlay, StandardCharsets.UTF_8));
  }

  @Test
  void concurrentLookupsShareOneSuccessfulScrape() throws Exception {
    Path overlay = tempDirectory.resolve("ScrapedEffects.txt");
    GrowtopiaWikiClient wikiClient = mock(GrowtopiaWikiClient.class);
    var scrapeStarted = new CountDownLatch(1);
    var releaseScrape = new CountDownLatch(1);
    when(wikiClient.getRawPage("Concurrent_Item"))
        .thenAnswer(
            _ -> {
              scrapeStarted.countDown();
              assertTrue(releaseScrape.await(5, TimeUnit.SECONDS));
              return PICKAXE_WIKITEXT;
            });
    ItemEffectService service = createService(wikiClient, overlay);
    service.load();
    GrowtopiaObject item = item(200_008, "Concurrent Item", ItemCategory.CLOTHES);

    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> service.getEffects(item));
      assertTrue(scrapeStarted.await(5, TimeUnit.SECONDS));
      var second = executor.submit(() -> service.getEffects(item));
      releaseScrape.countDown();

      assertEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
    }
    verify(wikiClient).getRawPage("Concurrent_Item");
  }

  private ItemEffectService createService(GrowtopiaWikiClient wikiClient, Path overlay) {
    var properties =
        new LockiumProperties(
            "Enjoy your day!",
            "https://growtopiagame.com/detail",
            "https://s3.amazonaws.com/world.growtopiagame.com/",
            Duration.ofHours(1),
            "https://growtopiawiki.com",
            overlay.toString());
    return new ItemEffectService(wikiClient, properties);
  }
}
