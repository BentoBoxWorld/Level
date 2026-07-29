package world.bentobox.level.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.bukkit.conversations.Conversation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.CommonTestSetup;

/**
 * Tests for {@link ConversationUtils}.
 */
class ConversationUtilsTest extends CommonTestSetup {

    private User user;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        // The conversation timeout canceller starts a scheduler timer on build.
        when(plugin.getServer()).thenReturn(server);
        user = User.getInstance(p);
    }

    /**
     * A player with no active conversation should get a new one.
     */
    @Test
    void testCreateStringInputStartsConversation() {
        when(p.isConversing()).thenReturn(false);

        Consumer<String> consumer = value -> {};
        ConversationUtils.createStringInput(consumer, user, "question?", "done");

        verify(p).beginConversation(any(Conversation.class));
    }

    /**
     * Clicking the search button while a conversation is already pending must not
     * queue a second conversation (#451) — it should just repeat the question.
     */
    @Test
    void testCreateStringInputDoesNotQueueSecondConversation() {
        when(p.isConversing()).thenReturn(true);

        Consumer<String> consumer = value -> {};
        ConversationUtils.createStringInput(consumer, user, "question?", "done");

        verify(p, never()).beginConversation(any(Conversation.class));
        verify(p).closeInventory();
        verify(p).sendRawMessage(anyString());
    }
}
