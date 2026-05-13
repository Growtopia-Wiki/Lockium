package dev.skullition.lockium.service;

import dev.skullition.lockium.config.SecretsConfig;
import io.github.freya022.botcommands.api.core.JDAService;
import io.github.freya022.botcommands.api.core.config.JDAConfiguration;
import io.github.freya022.botcommands.api.core.events.BReadyEvent;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@NullMarked
public class LockiumBot extends JDAService {
    private final JDAConfiguration jdaConfiguration;
    private final SecretsConfig secretsConfig;

    public LockiumBot(JDAConfiguration jdaConfiguration, SecretsConfig secretsConfig) {
        this.jdaConfiguration = jdaConfiguration;
        this.secretsConfig = secretsConfig;
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
        JDABuilder.createDefault(secretsConfig.token(), getIntents())
                .enableCache(getCacheFlags())
                .setActivity(Activity.customStatus("Hello there! :)"))
                .setEventManager(iEventManager)
                .build();
    }
}
