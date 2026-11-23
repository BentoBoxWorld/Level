//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.level.util;


import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.api.prompts.Prompts;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;


/**
 * Asks a player for typed input. This keeps the shape it had when it was built on Bukkit's conversation API,
 * which Paper is removing, and now hands the work to BentoBox's {@link Prompts}: the player gets a dialog with
 * a text box where the server supports dialogs, and is asked in chat otherwise.
 */
public class ConversationUtils
{
    /**
     * The heading of every prompt. The question itself is shown under it.
     */
    private static final Component TITLE = Component.text("Level");


    private ConversationUtils() {} // Private constructor as this is a utility class only with static methods


    /**
     * Closes the open gui and asks the player for one line of text. Asking again while the player still has
     * an unanswered question replaces that question; nothing is queued (#451).
     *
     * @param consumer Receives the text, or null if the player cancelled or did not answer.
     * @param user User who is asked.
     * @param question What the player is asked, already translated.
     * @param successMessage Message sent after the text is accepted, or null for none.
     */
    public static void createStringInput(Consumer<String> consumer,
            User user,
            @NonNull String question,
            @Nullable String successMessage)
    {
        user.closeInventory();
        Prompts.text(user).
        title(TITLE).
        message(Util.parseMiniMessageOrLegacy(question)).
        onSubmit(text ->
        {
            consumer.accept(text);

            if (successMessage != null && !successMessage.isBlank())
            {
                user.sendRawMessage(successMessage);
            }
        }).
        onCancel(() -> consumer.accept(null)).
        open();
    }
}
