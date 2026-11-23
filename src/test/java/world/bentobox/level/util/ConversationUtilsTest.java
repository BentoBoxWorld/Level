package world.bentobox.level.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.api.prompts.ChatInputListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.CommonTestSetup;

/**
 * Tests that {@link ConversationUtils} hands its questions to BentoBox's prompts and passes the answers back
 * the way its callers expect: the value on success, and null (or false for a confirmation) when the player
 * backs out.
 * <p>
 * Dialogs cannot be built without a server, so they are switched off here and every prompt is answered in
 * chat. Translations come back as their locale key, so the cancel, done and confirm words are the keys.
 */
class ConversationUtilsTest extends CommonTestSetup
{
    private static final String CANCEL = "prompts.chat.cancel-word";

    private User user;

    private final ChatInputListener listener = new ChatInputListener();

    private final List<Object> answers = new ArrayList<>();


    @Override
    @BeforeEach
    protected void setUp() throws Exception
    {
        super.setUp();
        this.plugin.getSettings().setDialogPrompts(false);
        this.user = User.getInstance(this.p);
    }


    @Override
    @AfterEach
    protected void tearDown() throws Exception
    {
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(quit.getPlayer()).thenReturn(this.p);
        this.listener.onQuit(quit);
        super.tearDown();
    }


    /**
     * Sends a chat message as the player and runs the task that carries it to the main thread.
     */
    private void say(String text)
    {
        AsyncChatEvent chat = mock(AsyncChatEvent.class);
        when(chat.getPlayer()).thenReturn(this.p);
        when(chat.message()).thenReturn(Component.text(text));
        this.listener.onChat(chat);
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(this.sch, atLeastOnce()).runTask(eq(this.plugin), task.capture());
        task.getValue().run();
    }


    @Test
    void testStringInput()
    {
        ConversationUtils.createStringInput(this.answers::add, this.user, "Search for?", "Updated");
        verify(this.p).closeInventory();
        assertTrue(ChatInputListener.isWaitingFor(this.uuid));

        this.say("diamond");

        assertEquals(List.of("diamond"), this.answers);
        assertFalse(ChatInputListener.isWaitingFor(this.uuid));
    }


    @Test
    void testStringInputCancelGivesNull()
    {
        ConversationUtils.createStringInput(this.answers::add, this.user, "Search for?", null);

        this.say(CANCEL);

        assertEquals(1, this.answers.size());
        assertNull(this.answers.getFirst());
        assertFalse(ChatInputListener.isWaitingFor(this.uuid));
    }


    /**
     * Clicking the search button again while a question is still waiting must not stack questions (#451): the
     * new one replaces the old one, and only the new consumer is answered.
     */
    @Test
    void testSecondQuestionReplacesFirst()
    {
        List<Object> first = new ArrayList<>();
        ConversationUtils.createStringInput(first::add, this.user, "Search for?", null);
        ConversationUtils.createStringInput(this.answers::add, this.user, "Search for?", null);

        this.say("stone");

        assertTrue(first.isEmpty());
        assertEquals(List.of("stone"), this.answers);
        assertFalse(ChatInputListener.isWaitingFor(this.uuid));
    }
}
