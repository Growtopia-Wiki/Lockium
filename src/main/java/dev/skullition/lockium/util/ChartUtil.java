package dev.skullition.lockium.util;

import static dev.skullition.lockium.util.FormatUtil.formatNumber;

import dev.skullition.lockium.model.PlayerCountSample;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.jspecify.annotations.Nullable;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders the online-player history as a PNG line chart.
 *
 * <p>Colours follow the Growtopia Wiki's own scheme so the image sits naturally beside the site:
 * a deep teal ground, a light cyan accent for the series, and white text.
 *
 * <p>Rendering is pure computation with no Spring dependencies, matching {@link FormatUtil} and
 * {@link ItemUtils}. A failure never propagates — callers receive {@code null} and are expected to
 * reply without the image rather than fail the interaction.
 */
public class ChartUtil {
  private static final Logger logger = LoggerFactory.getLogger(ChartUtil.class);

  private ChartUtil() {}

  /** Page background of growtopiawiki.com. */
  private static final Color BACKGROUND = new Color(0x06, 0x26, 0x29);

  /** The site's divider tone, lifted enough to read as gridlines against the dark ground. */
  private static final Color GRID = new Color(0x2C, 0x5A, 0x5E);

  /** The site's link and heading accent. */
  private static final Color ACCENT = new Color(0x72, 0xD2, 0xDB);

  /** The accent at low opacity, filling the area under the line without hiding the gridlines. */
  private static final Color ACCENT_FILL = new Color(0x72, 0xD2, 0xDB, 38);

  /** Primary text colour. */
  private static final Color TEXT = Color.WHITE;

  /** Fewer samples than this cannot form a meaningful line. */
  static final int MINIMUM_SAMPLES = 2;

  /** Points to plot after downsampling; ~6-minute buckets across a 24-hour window. */
  static final int TARGET_POINTS = 240;

  /** Fraction of the value range added above and below the series. */
  private static final double RANGE_PADDING = 0.15;

  /**
   * Renders the player-count history.
   *
   * @param samples samples in ascending time order; may be empty
   * @param currentCount the latest online count, shown in the chart title
   * @param zone the time zone the x-axis is labelled in
   * @return PNG bytes, or {@code null} when there are too few samples to draw or rendering failed
   */
  public static byte @Nullable [] renderPlayerCountChart(
      List<PlayerCountSample> samples, int currentCount, ZoneId zone) {
    if (samples.size() < MINIMUM_SAMPLES) {
      logger.debug("renderPlayerCountChart: only {} sample(s), skipping chart", samples.size());
      return null;
    }

    List<PlayerCountSample> plotted = downsample(samples);
    List<Date> times = new ArrayList<>(plotted.size());
    List<Integer> counts = new ArrayList<>(plotted.size());
    for (PlayerCountSample sample : plotted) {
      times.add(Date.from(sample.sampledAt()));
      counts.add(sample.onlineCount());
    }

    XYChart chart = new XYChartBuilder().width(900).height(330).build();
    chart.setTitle("There are %s online players.".formatted(formatNumber(currentCount)));

    var styler = chart.getStyler();
    // One flat surface: a separate plot tone reads as a rectangle floating on the page.
    styler.setChartBackgroundColor(BACKGROUND);
    styler.setPlotBackgroundColor(BACKGROUND);
    styler.setPlotBorderVisible(false);
    styler.setChartTitleBoxVisible(false);
    styler.setChartTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
    styler.setChartFontColor(TEXT);
    styler.setLegendVisible(false);
    styler.setAxisTitlesVisible(false);
    styler.setAntiAlias(true);
    styler.setChartPadding(16);

    styler.setPlotGridLinesColor(GRID);
    styler.setPlotGridLinesVisible(true);
    styler.setPlotGridVerticalLinesVisible(true);

    styler.setAxisTicksMarksVisible(false);
    styler.setAxisTickLabelsColor(TEXT);
    styler.setAxisTickLabelsFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
    styler.setLocale(Locale.US);
    styler.setTimezone(TimeZone.getTimeZone(zone));
    styler.setDatePattern("HH:mm");
    styler.setXAxisTickMarkSpacingHint(110);
    styler.setYAxisDecimalPattern("#,###");

    applyPaddedRange(styler, plotted);

    XYSeries series = chart.addSeries("Players", times, counts);
    series.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area);
    series.setMarker(SeriesMarkers.NONE);
    series.setLineColor(ACCENT);
    series.setLineWidth(2.0f);
    series.setFillColor(ACCENT_FILL);

    try {
      return BitmapEncoder.getBitmapBytes(chart, BitmapFormat.PNG);
    } catch (IOException e) {
      logger.warn("Failed to render player-count chart: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Sets a y-axis range padded around the observed values.
   *
   * <p>The axis is deliberately not zero-based: online counts sit in the tens of thousands and move
   * by roughly a fifth across a day, which a zero-based axis would flatten into a straight line.
   * Commands are expected to print the peak and low alongside the image so the scale is explicit.
   *
   * @param styler the chart styler to configure
   * @param samples the samples being plotted, which must not be empty
   */
  private static void applyPaddedRange(
      org.knowm.xchart.style.XYStyler styler, List<PlayerCountSample> samples) {
    int min = samples.stream().mapToInt(PlayerCountSample::onlineCount).min().orElseThrow();
    int max = samples.stream().mapToInt(PlayerCountSample::onlineCount).max().orElseThrow();
    // A flat series still needs an axis with extent, hence the minimum of 1.
    long padding = Math.max(1, Math.round((max - min) * RANGE_PADDING));
    styler.setYAxisMin((double) Math.max(0, min - padding));
    styler.setYAxisMax((double) (max + padding));
  }

  /**
   * Averages samples into at most {@value #TARGET_POINTS} buckets.
   *
   * <p>A minute-resolution 24-hour window is around 1,440 points, far more than a 900px-wide plot
   * can resolve, and drawing them all produces a fuzzy line and a needlessly large PNG. Averaging
   * rather than keeping every nth point keeps a short spike visible as a smaller bump instead of
   * dropping it entirely.
   *
   * <p>Buckets are formed by index rather than by wall-clock time, so gaps left by failed polls do
   * not create empty buckets.
   *
   * @param samples samples in ascending time order
   * @return the downsampled series, or the input unchanged when it is already small enough
   */
  static List<PlayerCountSample> downsample(List<PlayerCountSample> samples) {
    if (samples.size() <= TARGET_POINTS) {
      return samples;
    }

    List<PlayerCountSample> buckets = new ArrayList<>(TARGET_POINTS);
    for (int bucket = 0; bucket < TARGET_POINTS; bucket++) {
      int start = (int) ((long) bucket * samples.size() / TARGET_POINTS);
      int end = (int) ((long) (bucket + 1) * samples.size() / TARGET_POINTS);
      if (start >= end) {
        continue;
      }
      long total = 0;
      for (int i = start; i < end; i++) {
        total += samples.get(i).onlineCount();
      }
      int mean = Math.round((float) total / (end - start));
      buckets.add(new PlayerCountSample(samples.get(start).sampledAt(), mean));
    }
    logger.debug("downsample: reduced {} sample(s) to {}", samples.size(), buckets.size());
    return buckets;
  }
}
