package dev.skullition.lockium.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.skullition.lockium.model.PlayerCountSample;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests player-count chart rendering and the downsampling that feeds it. */
class ChartUtilTests {

  private static final Instant START = Instant.parse("2026-09-16T00:00:00Z");

  @Test
  void rendersPngForTwoOrMoreSamples() {
    byte[] png =
        ChartUtil.renderPlayerCountChart(samples(10), 40_000, GrowtopiaTimeUtil.GROWTOPIA_ZONE);

    assertNotNull(png);
    // Exact bytes vary with the JDK's rasteriser, so assert the PNG signature instead.
    assertTrue(png.length > 1_000);
    assertEquals((byte) 0x89, png[0]);
    assertEquals((byte) 'P', png[1]);
    assertEquals((byte) 'N', png[2]);
    assertEquals((byte) 'G', png[3]);
  }

  @Test
  void returnsNullForEmptySamples() {
    assertNull(ChartUtil.renderPlayerCountChart(List.of(), 0, GrowtopiaTimeUtil.GROWTOPIA_ZONE));
  }

  @Test
  void returnsNullForSingleSample() {
    assertNull(ChartUtil.renderPlayerCountChart(samples(1), 100, GrowtopiaTimeUtil.GROWTOPIA_ZONE));
  }

  @Test
  void rendersConstantSeries() {
    List<PlayerCountSample> flat = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      flat.add(new PlayerCountSample(START.plusSeconds(60L * i), 30_000));
    }

    // min == max would otherwise produce a zero-height axis.
    assertNotNull(ChartUtil.renderPlayerCountChart(flat, 30_000, GrowtopiaTimeUtil.GROWTOPIA_ZONE));
  }

  @Test
  void downsamplesLongSeriesToTargetPoints() {
    List<PlayerCountSample> downsampled = ChartUtil.downsample(samples(1_440));

    assertEquals(ChartUtil.TARGET_POINTS, downsampled.size());
    // Buckets stay in ascending time order.
    for (int i = 1; i < downsampled.size(); i++) {
      assertTrue(downsampled.get(i - 1).sampledAt().isBefore(downsampled.get(i).sampledAt()));
    }
  }

  @Test
  void leavesShortSeriesUntouched() {
    List<PlayerCountSample> shortSeries = samples(20);

    assertEquals(shortSeries, ChartUtil.downsample(shortSeries));
  }

  @Test
  void downsamplingAveragesRatherThanDropping() {
    // A single spike must still lift its bucket instead of disappearing.
    List<PlayerCountSample> spiky = new ArrayList<>(samples(ChartUtil.TARGET_POINTS * 2));
    spiky.set(0, new PlayerCountSample(START, 1_000_000));

    List<PlayerCountSample> downsampled = ChartUtil.downsample(spiky);

    assertTrue(downsampled.getFirst().onlineCount() > 1_000);
  }

  /**
   * Builds a gently rising series one minute apart.
   *
   * @param count how many samples to create
   * @return the samples, oldest first
   */
  private static List<PlayerCountSample> samples(int count) {
    List<PlayerCountSample> samples = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      samples.add(new PlayerCountSample(START.plusSeconds(60L * i), 30_000 + (i * 7)));
    }
    return samples;
  }
}
