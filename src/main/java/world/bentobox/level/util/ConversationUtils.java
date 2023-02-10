//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.level.util;


import java.util.function.Consumer;

import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;


public class ConversationUtils
{
    // ---------------------------------------------------------------------
    // Section: Conversation API implementation
    // ---------------------------------------------------------------------

    private ConversationUtils() {} // Private constructor as this is a utility class only with static methods

    /**
     * This method will close opened gui and writes question in chat. After players answers on question in chat, message
     * will trigger consumer and gui will reopen.
     *
     * @param consumer Consumer that accepts player output text.
     * @param question Message that will be displayed in chat when player triggers conversion.
     * @param user User who is targeted with current confirmation.
     */
    public static void createStringInput(Consumer<String> consumer,
            User user,
            @NonNull String question,
            @Nullable String successMessage)
    {
        // Text input message.
        StringPrompt stringPrompt = new StringPrompt()
        {
            @Override
            public @NonNull String getPromptText(@NonNull ConversationContext context)
            {
                user.closeInventory();
                return question;
            }


            @Override
            public @NonNull Prompt acceptInput(@NonNull ConversationContext context, @Nullable String input)
            {
                consumer.accept(input);
                return ConversationUtils.endMessagePrompt(successMessage);
            }
        };

        new ConversationFactory(BentoBox.getInstance()).
        withPrefix(context -> user.getTranslation("level.conversations.prefix")).
        withFirstPrompt(stringPrompt).
        // On cancel conversation will be closed.
        withLocalEcho(false).
        withTimeout(90).
        withEscapeSequence(user.getTranslation("level.conversations.cancel-string")).
        // Use null value in consumer to detect if user has abandoned conversation.
        addConversationAbandonedListener(ConversationUtils.getAbandonListener(consumer, user)).
        buildConversation(user.getPlayer()).
        begin();
    }


    /**
     * This is just a simple end message prompt that displays requested message.
     *
     * @param message Message that will be displayed.
     * @return MessagePrompt that displays given message and exists from conversation.
     */
    private static MessagePrompt endMessagePrompt(@Nullable String message)
    {
        return new MessagePrompt()
        {
            @Override
            public @NonNull String getPromptText(@NonNull ConversationContext context)
            {
                return message == null ? "" : message;
            }


            @Override
            protected @Nullable Prompt getNextPrompt(@NonNull ConversationContext context)
            {
                return Prompt.END_OF_CONVERSATION;
            }
        };
    }


    /**
     * This method creates and returns abandon listener for every conversation.
     *
     * @param consumer Consumer which must return null value.
     * @param user User who was using conversation.
     * @return ConversationAbandonedListener instance.
     */
    private static ConversationAbandonedListener getAbandonListener(Consumer<?> consumer, User user)
    {
        return abandonedEvent ->
        {
            if (!abandonedEvent.gracefulExit())
            {
                consumer.accept(null);
                // send cancell message
                abandonedEvent.getContext().getForWhom().sendRawMessage(
                        user.getTranslation("level.conversations.prefix") +
                        user.getTranslation("level.conversations.cancelled"));
            }
        };
    }
}
