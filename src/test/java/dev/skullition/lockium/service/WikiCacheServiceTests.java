package dev.skullition.lockium.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.model.ItemsResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Tests atomic orchestration of the items and name-index cache refreshes. */
class WikiCacheServiceTests {

  @Test
  void refreshBuildsBothCachesFromTheSameResponse() {
    WikiDataService dataService = mock(WikiDataService.class);
    ItemsResponse response = new ItemsResponse(Map.of());
    when(dataService.refreshItems()).thenReturn(response);
    when(dataService.refreshNameIndex(response)).thenReturn(Map.of());

    new WikiCacheService(dataService).refreshCaches();

    var captor = ArgumentCaptor.forClass(ItemsResponse.class);
    verify(dataService).refreshNameIndex(captor.capture());
    assertSame(response, captor.getValue());
  }

  @Test
  void failedItemRefreshDoesNotRebuildNameIndex() {
    WikiDataService dataService = mock(WikiDataService.class);
    RuntimeException failure = new RuntimeException("Wiki unavailable");
    when(dataService.refreshItems()).thenThrow(failure);

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> new WikiCacheService(dataService).refreshCaches());

    assertSame(failure, thrown);
    verify(dataService, never()).refreshNameIndex(org.mockito.ArgumentMatchers.any());
  }
}
