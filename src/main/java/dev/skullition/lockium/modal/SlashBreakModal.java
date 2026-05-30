package dev.skullition.lockium.modal;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import dev.skullition.lockium.util.ItemUtils;
import io.github.freya022.botcommands.api.core.annotations.Handler;
import io.github.freya022.botcommands.api.modals.ModalEvent;
import io.github.freya022.botcommands.api.modals.annotations.ModalData;
import io.github.freya022.botcommands.api.modals.annotations.ModalHandler;
import io.github.freya022.botcommands.api.modals.annotations.ModalInput;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;

import java.util.ArrayList;
import java.util.List;


@Handler
public class SlashBreakModal {
    public static final String MODAL_NAME = "SlashBreak: break";
    public static final String INPUT_LUCKY = "SlashBreak: lucky";
    public static final String INPUT_BUDDY = "SlashBreak: buddy";
    public static final String INPUT_ANCES = "SlashBreak: ances";

    @ModalHandler(MODAL_NAME)
    public void onBreakModal(
            ModalEvent event,
            @ModalData ItemDetailResponse itemDetail,
            @ModalData ItemCatalogue itemCatalogue,
            @ModalData int count,
            @ModalInput(INPUT_LUCKY) String luckyString,
            @ModalInput(INPUT_BUDDY) String buddyString,
            @ModalInput(INPUT_ANCES) String ancesString
    ) {
        boolean lucky = parseBoolean(luckyString);
        boolean buddy = parseBoolean(buddyString);
        int ances = tryParseInt(ancesString, event);
        if (ances == -1) {
            return;
        } else if (ances > 6) {
            event.reply("The maximum level of Ancestral Tesseract of Dimensions is 6.").setEphemeral(true).queue();
            return;
        }

        List<ContainerChildComponent> components = new ArrayList<>();

        Container container = ItemUtils.createItemContainer(itemDetail, itemCatalogue, components);
        event.replyComponents(container)
                .useComponentsV2()
                .queue();
    }

    private boolean parseBoolean(String input) {
        return input.equals("True");
    }

    private int tryParseInt(String input, ModalEvent event) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            event.reply("`%s` is not a valid integer!".formatted(input)).setEphemeral(true).queue();
        }
        return -1;
    }
}
