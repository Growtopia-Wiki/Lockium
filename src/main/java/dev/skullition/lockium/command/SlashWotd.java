package dev.skullition.lockium.command;

import dev.skullition.lockium.client.GrowtopiaDetailClient;
import dev.skullition.lockium.util.AppEmojis;
import dev.skullition.lockium.util.ContainerUtil;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import org.springframework.beans.factory.annotation.Value;

@Command
public class SlashWotd {
    private final GrowtopiaDetailClient client;
    private final String renderUrl;

    public SlashWotd(GrowtopiaDetailClient client, @Value("${lockium.render-url}") String renderUrl) {
        this.client = client;
        this.renderUrl = renderUrl;
    }

    @TopLevelSlashCommandData(
            contexts = {
                    InteractionContextType.BOT_DM, InteractionContextType.GUILD, InteractionContextType.PRIVATE_CHANNEL
            },
            integrationTypes = {
                    IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL
            }
    )
    @JDASlashCommand(name = "wotd", description = "Render today's World of the Day.")
    public void onSlashWotd(GlobalSlashEvent event) {
        var detail = client.getGrowtopiaDetail();
        String wotd = detail.wotd().fullSize().substring(7);
        int dotIndex = wotd.indexOf(".");

        var container = ContainerUtil.createGenericContainer(
                TextDisplay.of("## %s WOTD: %s".formatted(AppEmojis.WOTD, wotd.substring(0, dotIndex).toUpperCase())),
                MediaGallery.of(MediaGalleryItem.fromUrl(renderUrl + wotd.toLowerCase()))
        );

        event.replyComponents(container)
                .useComponentsV2()
                .queue();
    }
}
