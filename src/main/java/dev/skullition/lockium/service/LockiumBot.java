package dev.skullition.lockium.service;

import dev.skullition.lockium.properties.DiscordProperties;
import dev.skullition.lockium.properties.LockiumProperties;
import dev.skullition.lockium.properties.WikiApiProperties;
import io.github.freya022.botcommands.api.core.JDAService;
import io.github.freya022.botcommands.api.core.config.JDAConfiguration;
import io.github.freya022.botcommands.api.core.events.BReadyEvent;
import java.util.Set;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * BotCommands {@link JDAService} implementation for Lockium.
 *
 * <p>Responsible for building the single JDA instance with the intents, cache flags, and activity
 * status defined in application properties. Delegates intent/cache configuration to {@link
 * JDAConfiguration} to keep this class focused on bootstrapping.
 */
@Service
public class LockiumBot extends JDAService {
  private static final Logger logger = LoggerFactory.getLogger(LockiumBot.class);
  private final JDAConfiguration jdaConfiguration;
  private final DiscordProperties discordProperties;
  private final WikiApiProperties wikiApiProperties;
  private final LockiumProperties lockiumProperties;

  /**
   * Creates the bot service. Instantiated once by Spring and managed by BotCommands.
   *
   * @param jdaConfiguration BotCommands configuration providing intents and cache flags
   * @param discordProperties properties holding the bot token
   * @param wikiApiProperties Wiki API properties, logged on startup for diagnostics
   * @param lockiumProperties properties holding the custom activity status
   */
  public LockiumBot(
      JDAConfiguration jdaConfiguration,
      DiscordProperties discordProperties,
      WikiApiProperties wikiApiProperties,
      LockiumProperties lockiumProperties) {
    this.jdaConfiguration = jdaConfiguration;
    this.discordProperties = discordProperties;
    this.wikiApiProperties = wikiApiProperties;
    this.lockiumProperties = lockiumProperties;
  }

  @Override
  public Set<GatewayIntent> getIntents() {
    return jdaConfiguration.getIntents();
  }

  @Override
  public Set<CacheFlag> getCacheFlags() {
    return jdaConfiguration.getCacheFlags();
  }

  @Override
  protected void createJDA(BReadyEvent botReadyEvent, IEventManager eventManager) {
    JDABuilder.createDefault(discordProperties.token(), getIntents())
        .enableCache(getCacheFlags())
        .setActivity(Activity.customStatus(lockiumProperties.status()))
        .setEventManager(eventManager)
        .build();
    logger.info("Lockium started, Wiki API={}", wikiApiProperties.url());
  }
}
