package dev.skullition.lockium.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent;
import io.github.freya022.botcommands.api.components.event.ButtonEvent;
import io.github.freya022.botcommands.api.localization.interaction.LocalizableInteractionHook;
import io.github.freya022.botcommands.api.modals.ModalEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ModalCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.mockito.Answers;

/** Captures Discord replies without connecting to Discord or starting Spring. */
final class DiscordEventHarness {
  private final ReplyCapture capture = new ReplyCapture();
  private final ReplyCallbackAction action = mockAction(capture);

  GlobalSlashEvent slashEvent() {
    GlobalSlashEvent event = mock(GlobalSlashEvent.class, Answers.CALLS_REAL_METHODS);
    LocalizableInteractionHook hook = hook();
    doReturn(hook).when(event).getHook();
    doAnswer(
            invocation -> {
              capture.delivery = "initial";
              capture.message = invocation.getArgument(0);
              return action;
            })
        .when(event)
        .reply(any(MessageCreateData.class));
    doAnswer(
            invocation -> {
              capture.delivery = "deferred";
              return action;
            })
        .when(event)
        .deferReply();
    ModalCallbackAction modalAction =
        mock(
            ModalCallbackAction.class,
            invocation -> {
              if (invocation.getMethod().getName().equals("queue")) {
                capture.queued = true;
              }
              return Answers.RETURNS_DEFAULTS.answer(invocation);
            });
    doAnswer(
            invocation -> {
              capture.delivery = "modal";
              capture.modal = invocation.getArgument(0);
              return modalAction;
            })
        .when(event)
        .replyModal(any(Modal.class));
    return event;
  }

  GuildSlashEvent guildEvent() {
    GuildSlashEvent event = mock(GuildSlashEvent.class, Answers.CALLS_REAL_METHODS);
    LocalizableInteractionHook hook = hook();
    doReturn(hook).when(event).getHook();
    doAnswer(
            invocation -> {
              capture.delivery = "initial";
              capture.message = invocation.getArgument(0);
              return action;
            })
        .when(event)
        .reply(any(MessageCreateData.class));
    doAnswer(
            invocation -> {
              capture.delivery = "deferred";
              return action;
            })
        .when(event)
        .deferReply();
    return event;
  }

  ModalEvent modalEvent() {
    ModalEvent event = mock(ModalEvent.class, Answers.CALLS_REAL_METHODS);
    doAnswer(
            invocation -> {
              capture.delivery = "initial";
              capture.message = invocation.getArgument(0);
              return action;
            })
        .when(event)
        .reply(any(MessageCreateData.class));
    doAnswer(
            invocation -> {
              capture.delivery = "deferred";
              return action;
            })
        .when(event)
        .deferReply();
    return event;
  }

  /** Captures both component edits and ephemeral replies from a button callback. */
  ButtonEvent buttonEvent() {
    ButtonEvent event = mock(ButtonEvent.class, Answers.CALLS_REAL_METHODS);
    doReturn(hook()).when(event).getHook();
    MessageEditCallbackAction[] edit = new MessageEditCallbackAction[1];
    edit[0] =
        mock(
            MessageEditCallbackAction.class,
            invocation -> captureAction(capture, edit[0], invocation));
    doAnswer(
            invocation -> {
              resetMessage("edit-original");
              return edit[0];
            })
        .when(event)
        .deferEdit();
    doAnswer(
            invocation -> {
              resetMessage("initial");
              capture.message = invocation.getArgument(0);
              return action;
            })
        .when(event)
        .reply(any(MessageCreateData.class));
    doAnswer(
            invocation -> {
              resetMessage("deferred");
              return action;
            })
        .when(event)
        .deferReply();
    return event;
  }

  /** Runs the saved success callback for an acknowledged deferred reply. */
  void completeReply() {
    if (capture.replySuccess == null || capture.interactionHook == null) {
      throw new AssertionError("The command did not queue a deferred-reply success callback");
    }
    capture.replySuccess.accept(capture.interactionHook);
  }

  /** Runs the saved failure callback for an unacknowledged deferred reply. */
  void failReply(Throwable failure) {
    if (capture.replyFailure == null) {
      throw new AssertionError("The command did not queue a deferred-reply failure callback");
    }
    capture.replyFailure.accept(failure);
  }

  private void resetMessage(String delivery) {
    final boolean preserveEphemeral = capture.ephemeral && delivery.equals("edit-original");
    capture.delivery = delivery;
    capture.message = null;
    capture.modal = null;
    capture.content = null;
    capture.components.clear();
    capture.ephemeral = preserveEphemeral;
    capture.componentsV2 = false;
    capture.queued = false;
    capture.allowedMentions = null;
    capture.attachments.clear();
  }

  String snapshot() {
    if (!capture.queued) {
      throw new AssertionError("The command did not queue a reply");
    }
    String payload;
    MessageCreateData message = capture.message;
    if (capture.modal != null) {
      payload = capture.modal.toData().toPrettyString();
    } else {
      if (message == null) {
        MessageCreateBuilder builder = new MessageCreateBuilder();
        if (capture.content != null) {
          builder.setContent(capture.content);
        }
        if (!capture.components.isEmpty()) {
          builder.setComponents(capture.components);
        }
        if (capture.allowedMentions != null) {
          builder.setAllowedMentions(capture.allowedMentions);
        }
        builder.useComponentsV2(capture.componentsV2);
        message = builder.build();
      }
      payload = message.toData().toPrettyString();
    }
    return ("delivery=%s\nephemeral=%s\ncomponentsV2=%s\nallowedMentions=%s\n"
            + "attachments=%s\npayload=%s\n")
        .formatted(
            capture.delivery,
            capture.ephemeral,
            capture.componentsV2,
            capture.allowedMentions,
            capture.attachments,
            payload)
        .stripTrailing();
  }

  private static ReplyCallbackAction mockAction(ReplyCapture capture) {
    ReplyCallbackAction[] holder = new ReplyCallbackAction[1];
    holder[0] =
        mock(
            ReplyCallbackAction.class,
            invocation -> {
              String method = invocation.getMethod().getName();
              switch (method) {
                case "applyData" -> {
                  capture.delivery = "initial";
                  capture.message = invocation.getArgument(0);
                }
                case "setContent" -> {
                  capture.delivery = "initial";
                  capture.content = invocation.getArgument(0);
                }
                case "addContent" -> {
                  capture.delivery = "initial";
                  capture.content =
                      (capture.content == null ? "" : capture.content)
                          + (String) invocation.getArgument(0);
                }
                case "setComponents" -> {
                  capture.delivery = "initial";
                  capture.components.clear();
                  capture.components.addAll(componentCollection(invocation.getArgument(0)));
                }
                case "addComponents" -> {
                  capture.delivery = "initial";
                  capture.components.addAll(componentCollection(invocation.getArgument(0)));
                }
                case "setEphemeral" -> capture.ephemeral = invocation.getArgument(0);
                case "useComponentsV2" -> capture.componentsV2 = true;
                case "setAllowedMentions" -> {
                  Collection<MentionType> mentions = invocation.getArgument(0);
                  capture.allowedMentions = new ArrayList<>(mentions);
                }
                case "addFiles" -> captureFiles(capture, invocation.getArgument(0));
                case "queue" -> {
                  capture.queued = true;
                  if (invocation.getArguments().length == 2) {
                    capture.replySuccess = invocation.getArgument(0);
                    capture.replyFailure = invocation.getArgument(1);
                  }
                }
                default -> {
                  // Other fluent settings do not affect the response contract we snapshot.
                }
              }
              if (invocation.getMethod().getReturnType().isInstance(holder[0])) {
                return holder[0];
              }
              return Answers.RETURNS_DEFAULTS.answer(invocation);
            });
    return holder[0];
  }

  private LocalizableInteractionHook hook() {
    LocalizableInteractionHook hook =
        mock(LocalizableInteractionHook.class, Answers.CALLS_REAL_METHODS);
    capture.interactionHook = hook;
    WebhookMessageCreateAction<Message> createAction = mockWebhookCreateAction(capture);
    WebhookMessageEditAction<Message> editAction = mockWebhookEditAction(capture);
    doAnswer(
            invocation -> {
              capture.delivery = "followup";
              capture.components.clear();
              capture.components.addAll(componentCollection(invocation.getArgument(0)));
              return createAction;
            })
        .when(hook)
        .sendMessageComponents(anyCollection());
    doAnswer(
            invocation -> {
              capture.delivery = "edit-original";
              capture.content = invocation.getArgument(0);
              return editAction;
            })
        .when(hook)
        .editOriginal(any(String.class));
    doAnswer(
            invocation -> {
              resetMessage("edit-original");
              capture.components.addAll(componentCollection(invocation.getArgument(0)));
              return editAction;
            })
        .when(hook)
        .editOriginalComponents(anyCollection());
    doAnswer(
            invocation -> {
              resetMessage("edit-original");
              capture.components.addAll(componentCollection(invocation.getArgument(0)));
              return editAction;
            })
        .when(hook)
        .editOriginalComponents(any(MessageTopLevelComponent[].class));
    return hook;
  }

  @SuppressWarnings("unchecked")
  private static WebhookMessageCreateAction<Message> mockWebhookCreateAction(ReplyCapture capture) {
    Object[] holder = new Object[1];
    holder[0] =
        mock(
            WebhookMessageCreateAction.class,
            invocation -> captureAction(capture, holder[0], invocation));
    return (WebhookMessageCreateAction<Message>) holder[0];
  }

  @SuppressWarnings("unchecked")
  private static WebhookMessageEditAction<Message> mockWebhookEditAction(ReplyCapture capture) {
    Object[] holder = new Object[1];
    holder[0] =
        mock(
            WebhookMessageEditAction.class,
            invocation -> captureAction(capture, holder[0], invocation));
    return (WebhookMessageEditAction<Message>) holder[0];
  }

  private static Object captureAction(
      ReplyCapture capture, Object action, org.mockito.invocation.InvocationOnMock invocation)
      throws Throwable {
    String method = invocation.getMethod().getName();
    switch (method) {
      case "setContent" -> capture.content = invocation.getArgument(0);
      case "setComponents" -> {
        capture.message = null;
        capture.components.clear();
        capture.components.addAll(componentCollection(invocation.getArgument(0)));
      }
      case "addComponents" -> {
        capture.message = null;
        capture.components.addAll(componentCollection(invocation.getArgument(0)));
      }
      case "setEphemeral" -> capture.ephemeral = invocation.getArgument(0);
      case "useComponentsV2" -> capture.componentsV2 = true;
      case "setAllowedMentions" -> {
        Collection<MentionType> mentions = invocation.getArgument(0);
        capture.allowedMentions = new ArrayList<>(mentions);
      }
      case "addFiles" -> captureFiles(capture, invocation.getArgument(0));
      case "queue" -> capture.queued = true;
      default -> {
        // Other fluent settings do not affect the response contract we snapshot.
      }
    }
    if (invocation.getMethod().getReturnType().isInstance(action)) {
      return action;
    }
    return Answers.RETURNS_DEFAULTS.answer(invocation);
  }

  private static void captureFiles(ReplyCapture capture, Object argument) {
    if (argument instanceof FileUpload file) {
      capture.attachments.add(file.getName());
    } else if (argument instanceof Collection<?> files) {
      files.stream()
          .filter(FileUpload.class::isInstance)
          .map(FileUpload.class::cast)
          .map(FileUpload::getName)
          .forEach(capture.attachments::add);
    } else if (argument instanceof FileUpload[] files) {
      for (FileUpload file : files) {
        capture.attachments.add(file.getName());
      }
    }
  }

  private static Collection<? extends MessageTopLevelComponent> componentCollection(
      Object argument) {
    if (argument instanceof MessageTopLevelComponent component) {
      return List.of(component);
    }
    if (argument instanceof Collection<?> components) {
      return components.stream()
          .filter(MessageTopLevelComponent.class::isInstance)
          .map(MessageTopLevelComponent.class::cast)
          .toList();
    }
    if (argument instanceof MessageTopLevelComponent[] components) {
      return List.of(components);
    }
    return List.of();
  }

  private static final class ReplyCapture {
    private String delivery;
    private MessageCreateData message;
    private Modal modal;
    private String content;
    private final List<MessageTopLevelComponent> components = new ArrayList<>();
    private boolean ephemeral;
    private boolean componentsV2;
    private boolean queued;
    private List<MentionType> allowedMentions;
    private final List<String> attachments = new ArrayList<>();
    private LocalizableInteractionHook interactionHook;
    private Consumer<InteractionHook> replySuccess;
    private Consumer<Throwable> replyFailure;
  }
}
