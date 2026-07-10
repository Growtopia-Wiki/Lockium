package dev.skullition.lockium.model;

/**
 * An item effect (in-game "mod") granted by wearing or consuming an item.
 *
 * <p>Effects come from two sources: the curated seed file {@code classpath:data/Effects.txt} and
 * pages scraped from the public Growtopia wiki at runtime. Both use the pipe-delimited row format
 * {@code itemId|name|applyMessage|removeMessage}, which this record mirrors minus the id (effects
 * are stored in an id-keyed map by {@code ItemEffectService}).
 *
 * <p>Messages are never {@code null}; an effect without an apply or remove message uses the empty
 * string so rows round-trip through the file format unchanged.
 *
 * @param name the mod name, e.g. {@code Enhanced Digging}
 * @param applyMessage the message shown in-game when the effect is gained, e.g. {@code You can
 *     smash bricks more quickly.}; empty if the effect has none
 * @param removeMessage the message shown in-game when the effect is lost, e.g. {@code Smash time is
 *     over.}; empty if the effect has none
 */
public record ItemEffect(String name, String applyMessage, String removeMessage) {}
