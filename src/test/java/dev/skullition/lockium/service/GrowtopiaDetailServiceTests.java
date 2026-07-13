package dev.skullition.lockium.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.client.GrowtopiaDetailClient;
import dev.skullition.lockium.model.GrowtopiaDetail;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

/** Tests fresh-detail retrieval and the bounded last-good fallback cache. */
class GrowtopiaDetailServiceTests {

  @Test
  void successfulRequestReturnsFreshDetail() {
    GrowtopiaDetailClient client = mock(GrowtopiaDetailClient.class);
    GrowtopiaDetail detail = detail("1234", "worlds/TEST.png");
    when(client.getGrowtopiaDetail()).thenReturn(detail);

    assertSame(detail, new GrowtopiaDetailService(client).getDetail());
  }

  @Test
  void failureWithoutCachedDetailReturnsNull() {
    GrowtopiaDetailClient client = mock(GrowtopiaDetailClient.class);
    when(client.getGrowtopiaDetail()).thenThrow(new RestClientException("Unavailable"));

    assertNull(new GrowtopiaDetailService(client).getDetail());
  }

  @Test
  void failureWithinTwentyFourHoursReturnsLastGoodDetail() {
    GrowtopiaDetailClient client = mock(GrowtopiaDetailClient.class);
    GrowtopiaDetail detail = detail("1234", "worlds/TEST.png");
    when(client.getGrowtopiaDetail())
        .thenReturn(detail)
        .thenThrow(new RestClientException("Unavailable"));
    Clock clock = mock(Clock.class);
    when(clock.millis()).thenReturn(1_000L, 1_000L + Duration.ofHours(23).toMillis());
    GrowtopiaDetailService service = new GrowtopiaDetailService(client, clock);

    assertSame(detail, service.getDetail());
    assertSame(detail, service.getDetail());
    verify(client, times(2)).getGrowtopiaDetail();
  }

  @Test
  void cacheExpiresAtTwentyFourHours() {
    GrowtopiaDetailClient client = mock(GrowtopiaDetailClient.class);
    GrowtopiaDetail detail = detail("1234", "worlds/TEST.png");
    when(client.getGrowtopiaDetail())
        .thenReturn(detail)
        .thenThrow(new RestClientException("Unavailable"));
    Clock clock = mock(Clock.class);
    when(clock.millis()).thenReturn(1_000L, 1_000L + Duration.ofHours(24).toMillis());
    GrowtopiaDetailService service = new GrowtopiaDetailService(client, clock);

    assertSame(detail, service.getDetail());
    assertNull(service.getDetail());
  }

  @Test
  void laterSuccessReplacesTheFallbackValue() {
    GrowtopiaDetailClient client = mock(GrowtopiaDetailClient.class);
    GrowtopiaDetail first = detail("100", "worlds/FIRST.png");
    GrowtopiaDetail second = detail("200", "worlds/SECOND.png");
    when(client.getGrowtopiaDetail())
        .thenReturn(first)
        .thenReturn(second)
        .thenThrow(new RestClientException("Unavailable"));
    Clock clock = mock(Clock.class);
    when(clock.millis()).thenReturn(1_000L, 2_000L, 3_000L);
    GrowtopiaDetailService service = new GrowtopiaDetailService(client, clock);

    assertSame(first, service.getDetail());
    assertSame(second, service.getDetail());
    assertSame(second, service.getDetail());
  }

  private static GrowtopiaDetail detail(String onlineUsers, String fullSize) {
    return new GrowtopiaDetail(
        onlineUsers, new GrowtopiaDetail.WotdImages(fullSize, "worlds/RESIZED.png"));
  }
}
