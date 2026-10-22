package dev.skullition.lockium.command;

import static dev.skullition.lockium.util.FormatUtil.formatNumber;

import dev.skullition.lockium.model.AuctionListing;
import dev.skullition.lockium.model.GrowtopiaAuction;
import dev.skullition.lockium.model.ProxyPayload;
import dev.skullition.lockium.util.AppEmojis;
import dev.skullition.lockium.util.ContainerUtil;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jspecify.annotations.Nullable;

/** Renders a bounded, paginated view of one accepted auction snapshot. */
final class AuctionPageRenderer {
  private static final int PAGE_SIZE = 5;
  private static final int FILTER_LIMIT = 100;
  private static final int NAME_LIMIT = 100;
  private static final int OWNER_LIMIT = 50;
  private static final int WORLD_LIMIT = 50;
  private static final Duration OUT_OF_DATE_AGE = Duration.ofMinutes(2);
  private static final long MIN_DISPLAYABLE_EPOCH_SECOND =
      Instant.parse("0001-01-01T00:00:00Z").getEpochSecond();
  private static final long MAX_DISPLAYABLE_EPOCH_SECOND =
      Instant.parse("9999-12-31T23:59:59Z").getEpochSecond();

  private final Clock clock;
  private final Duration maxAge;

  AuctionPageRenderer(Clock clock, Duration maxAge) {
    this.clock = Objects.requireNonNull(clock);
    this.maxAge = Objects.requireNonNull(maxAge);
    if (maxAge.isNegative() || maxAge.isZero()) {
      throw new IllegalArgumentException("maxAge must be positive");
    }
  }

  /**
   * Renders one page from the accepted payload, retaining source order and duplicate listings.
   *
   * @param payload the last accepted proxy payload, or {@code null} before the first success
   * @param failedAt when the most recent refresh failed, independent of the accepted payload
   * @param filter optional item-name substring
   * @param pageIndex zero-based requested page index
   * @return rendered components and the clamped page metadata
   */
  RenderedPage render(
      @Nullable ProxyPayload<GrowtopiaAuction> payload,
      @Nullable Instant failedAt,
      String filter,
      int pageIndex) {
    final String normalizedFilter = normalizeFilter(filter);
    List<ContainerChildComponent> components = new ArrayList<>();

    if (payload == null) {
      components.add(
          TextDisplay.of(
              unavailableText(
                  failedAt == null
                      ? "Auction data is currently unavailable."
                      : "The latest refresh failed, so auction data is unavailable.")));
      return new RenderedPage(components, 0, 1);
    }

    Instant now = clock.instant();
    long ageSeconds = elapsedSeconds(payload.fetchedAt(), now);
    StringBuilder text = new StringBuilder();
    text.append("## %s Auction listings\n".formatted(AppEmojis.AUCTION));
    text.append("Snapshot: ")
        .append(relativeTimestamp(payload.fetchedAt()))
        .append(". Timers are estimates.");
    Duration age = Duration.ofSeconds(ageSeconds);
    appendFreshnessNotices(text, payload, failedAt, age);
    if (!normalizedFilter.isEmpty()) {
      text.append("\nFilter: **").append(escapeText(normalizedFilter, FILTER_LIMIT)).append("**.");
    }

    if (age.compareTo(maxAge) >= 0) {
      text.append("\nAuction data is too old to display. Run `/gt auction` again later.");
      components.add(TextDisplay.of(text.toString()));
      return new RenderedPage(components, 0, 1);
    }

    List<AuctionListing> listings = payload.data().auctions();
    if (listings.isEmpty()) {
      text.append("\nThis snapshot contains no auctions.");
      components.add(TextDisplay.of(text.toString()));
      return new RenderedPage(components, 0, 1);
    }

    List<AuctionListing> matches =
        normalizedFilter.isEmpty()
            ? listings
            : listings.stream()
                .filter(
                    listing ->
                        listing
                            .name()
                            .toLowerCase(Locale.ROOT)
                            .contains(normalizedFilter.toLowerCase(Locale.ROOT)))
                .toList();
    if (matches.isEmpty()) {
      text.append("\nNo auctions matched that item filter.");
      components.add(TextDisplay.of(text.toString()));
      return new RenderedPage(components, 0, 1);
    }

    int pageCount = (matches.size() - 1) / PAGE_SIZE + 1;
    int clampedPageIndex = Math.max(0, Math.min(pageIndex, pageCount - 1));
    text.append("\n").append(formatNumber(matches.size())).append(" results · Page ");
    text.append(formatNumber(clampedPageIndex + 1)).append(" of ").append(formatNumber(pageCount));

    int start = (int) ((long) clampedPageIndex * PAGE_SIZE);
    int end = Math.min(start + PAGE_SIZE, matches.size());
    for (int index = start; index < end; index++) {
      text.append("\n\n").append(renderListing(matches.get(index), now, payload.fetchedAt()));
    }

    components.add(TextDisplay.of(text.toString()));
    return new RenderedPage(components, clampedPageIndex, pageCount);
  }

  /** Renders the fixed expired-view message after the frozen snapshot outlives its allowed age. */
  Container expired() {
    return ContainerUtil.createGenericContainer(
        TextDisplay.of(
            "## Auction listings expired\n"
                + "This view is too old to display. Run `/gt auction` again."));
  }

  private static String unavailableText(String message) {
    return "## %s Auction listings\n%s".formatted(AppEmojis.AUCTION, message);
  }

  private static void appendFreshnessNotices(
      StringBuilder text,
      ProxyPayload<GrowtopiaAuction> payload,
      @Nullable Instant failedAt,
      Duration age) {
    var stale = payload.stale();
    if (stale != null) {
      text.append("\nThe proxy returned stale cached data (")
          .append(stale.reason().equals("parse") ? "source parsing failed" : "source unavailable")
          .append(").");
    } else if (age.compareTo(OUT_OF_DATE_AGE) >= 0) {
      text.append("\nThis snapshot is out of date.");
    }
    if (failedAt != null) {
      text.append("\nThe latest refresh failed; showing the last accepted snapshot.");
    }
    int warningCount = payload.warnings().size();
    if (warningCount > 0) {
      text.append("\nParsing warnings: ").append(formatNumber(warningCount)).append('.');
    }
  }

  private static String renderListing(AuctionListing listing, Instant now, Instant fetchedAt) {
    long elapsed = elapsedSeconds(fetchedAt, now);
    long remaining = Math.max(0, listing.remainingSeconds());
    Instant anchor = fetchedAt.isAfter(now) ? now : fetchedAt;
    boolean elapsedTimer = elapsed >= remaining;
    String timer = estimatedEnd(anchor, remaining, elapsedTimer);

    return "**%s**\nOwner: %s · World: %s\n%s Bid: %s WL · %s"
        .formatted(
            escapeText(listing.name(), NAME_LIMIT),
            escapeText(listing.owner(), OWNER_LIMIT),
            escapeText(listing.world(), WORLD_LIMIT),
            AppEmojis.WORLD_LOCK,
            formatNumber(listing.bidWl()),
            timer);
  }

  private static String estimatedEnd(Instant anchor, long remainingSeconds, boolean elapsedTimer) {
    String elapsedNotice = elapsedTimer ? " · Timer elapsed; outcome unknown" : "";
    try {
      Instant estimatedEnd = anchor.plusSeconds(remainingSeconds);
      long epochSecond = estimatedEnd.getEpochSecond();
      if (epochSecond < MIN_DISPLAYABLE_EPOCH_SECOND
          || epochSecond > MAX_DISPLAYABLE_EPOCH_SECOND) {
        return "Estimated end is too far away to display" + elapsedNotice;
      }
      return "Estimated end: " + relativeTimestamp(estimatedEnd) + elapsedNotice;
    } catch (DateTimeException | ArithmeticException exception) {
      return "Estimated end is too far away to display" + elapsedNotice;
    }
  }

  private static String relativeTimestamp(Instant instant) {
    if (instant.getEpochSecond() < MIN_DISPLAYABLE_EPOCH_SECOND
        || instant.getEpochSecond() > MAX_DISPLAYABLE_EPOCH_SECOND) {
      return "timestamp unavailable";
    }
    return TimeFormat.RELATIVE.format(instant);
  }

  private static String normalizeFilter(String filter) {
    String trimmed = Objects.requireNonNull(filter).trim();
    int codePoints = trimmed.codePointCount(0, trimmed.length());
    if (codePoints <= FILTER_LIMIT) {
      return trimmed;
    }
    return trimmed.substring(0, trimmed.offsetByCodePoints(0, FILTER_LIMIT));
  }

  private static String escapeText(String raw, int maxCodePoints) {
    int inputCodePoints = raw.codePointCount(0, raw.length());
    boolean truncated = inputCodePoints > maxCodePoints;
    int limit = truncated ? maxCodePoints - 1 : maxCodePoints;
    StringBuilder escaped = new StringBuilder();
    raw.codePoints().limit(limit).forEach(codePoint -> appendEscaped(escaped, codePoint));
    if (truncated) {
      escaped.append('…');
    }
    return escaped.toString();
  }

  private static void appendEscaped(StringBuilder escaped, int codePoint) {
    if (Character.isISOControl(codePoint) || Character.isWhitespace(codePoint)) {
      escaped.append(' ');
      return;
    }
    if (codePoint == '@') {
      escaped.append("@\u200B");
      return;
    }
    if ("\\*_~|`>[]()#!{}".indexOf(codePoint) >= 0) {
      escaped.append('\\');
    }
    escaped.appendCodePoint(codePoint);
  }

  private static long elapsedSeconds(Instant from, Instant to) {
    if (!to.isAfter(from)) {
      return 0;
    }
    return Duration.between(from, to).getSeconds();
  }

  /** A page description that can append its interactive controls without mutating itself. */
  record RenderedPage(List<ContainerChildComponent> components, int pageIndex, int pageCount) {
    RenderedPage {
      components = List.copyOf(components);
    }

    Container container(List<Button> controls) {
      List<ContainerChildComponent> children = new ArrayList<>(components);
      if (!controls.isEmpty()) {
        children.add(ActionRow.of(controls));
      }
      return ContainerUtil.createGenericContainer(children);
    }
  }
}
