package dev.skullition.lockium.service;

import dev.skullition.lockium.properties.DiscordProperties;
import dev.skullition.lockium.properties.WikiApiProperties;
import io.github.freya022.botcommands.api.core.JDAService;
import io.github.freya022.botcommands.api.core.config.JDAConfiguration;
import io.github.freya022.botcommands.api.core.events.BReadyEvent;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class LockiumBot extends JDAService {
    private static final Logger logger = LoggerFactory.getLogger(LockiumBot.class);
    private final JDAConfiguration jdaConfiguration;
    private final DiscordProperties discordProperties;
    private final WikiApiProperties wikiApiProperties;
    private final String defaultStatus;

    public LockiumBot(JDAConfiguration jdaConfiguration,
                      DiscordProperties discordProperties,
                      WikiApiProperties wikiApiProperties, 
                      @Value("${lockium.status}") String defaultStatus) {
        this.jdaConfiguration = jdaConfiguration;
        this.discordProperties = discordProperties;
        this.wikiApiProperties = wikiApiProperties;
        this.defaultStatus = defaultStatus;
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
    protected void createJDA(BReadyEvent bReadyEvent, IEventManager iEventManager) {
        JDABuilder.createDefault(discordProperties.token(), getIntents())
                .enableCache(getCacheFlags())
                .setActivity(Activity.customStatus(defaultStatus))
                .setEventManager(iEventManager)
                .build();
        logger.info("Lockium started, Wiki API={}", wikiApiProperties.url());
    }
}
