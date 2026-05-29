package dev.skullition.lockium.modal;

import dev.skullition.lockium.model.ItemCatalogue;
import io.github.freya022.botcommands.api.core.annotations.Handler;
import io.github.freya022.botcommands.api.modals.ModalEvent;
import io.github.freya022.botcommands.api.modals.annotations.ModalData;
import io.github.freya022.botcommands.api.modals.annotations.ModalHandler;
import io.github.freya022.botcommands.api.modals.annotations.ModalInput;

import java.util.List;


@Handler
public class SlashBreakModal {
    public static final String MODAL_NAME = "SlashBreak: break";
    public static final String INPUT_LUCKY = "SlashBreak: lucky";
    
    @ModalHandler(MODAL_NAME)
    public void onBreakModal(
            ModalEvent event,
            @ModalData ItemCatalogue itemQuery,
            @ModalData int count,
            @ModalInput(INPUT_LUCKY) List<String> lucky
            ) {
        event.reply("%s = %s using clover = %s".formatted(itemQuery.itemName(), count, lucky)).queue();
    }
}
