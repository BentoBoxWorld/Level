package world.bentobox.level.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.bukkit.permissions.PermissionAttachmentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.api.user.User;

/**
 * Tests for {@link Utils}
 */
class UtilsTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        when(user.isPlayer()).thenReturn(true);
    }

    // --- getNextValue ---

    @Test
    void testGetNextValueMidArray() {
        String[] values = {"A", "B", "C"};
        assertEquals("B", Utils.getNextValue(values, "A"));
    }

    @Test
    void testGetNextValueLastWrapsToFirst() {
        String[] values = {"A", "B", "C"};
        assertEquals("A", Utils.getNextValue(values, "C"));
    }

    @Test
    void testGetNextValueMiddleElement() {
        String[] values = {"A", "B", "C"};
        assertEquals("C", Utils.getNextValue(values, "B"));
    }

    @Test
    void testGetNextValueNotFoundReturnsCurrent() {
        String[] values = {"A", "B", "C"};
        assertEquals("Z", Utils.getNextValue(values, "Z"));
    }

    @Test
    void testGetNextValueSingleElement() {
        String[] values = {"A"};
        assertEquals("A", Utils.getNextValue(values, "A"));
    }

    @Test
    void testGetNextValueWithEnums() {
        TestEnum[] values = TestEnum.values();
        assertEquals(TestEnum.TWO, Utils.getNextValue(values, TestEnum.ONE));
        assertEquals(TestEnum.ONE, Utils.getNextValue(values, TestEnum.THREE));
    }

    // --- getPreviousValue ---

    @Test
    void testGetPreviousValueMidArray() {
        String[] values = {"A", "B", "C"};
        assertEquals("A", Utils.getPreviousValue(values, "B"));
    }

    @Test
    void testGetPreviousValueFirstWrapsToLast() {
        String[] values = {"A", "B", "C"};
        assertEquals("C", Utils.getPreviousValue(values, "A"));
    }

    @Test
    void testGetPreviousValueLastElement() {
        String[] values = {"A", "B", "C"};
        assertEquals("B", Utils.getPreviousValue(values, "C"));
    }

    @Test
    void testGetPreviousValueNotFoundReturnsCurrent() {
        String[] values = {"A", "B", "C"};
        assertEquals("Z", Utils.getPreviousValue(values, "Z"));
    }

    @Test
    void testGetPreviousValueSingleElement() {
        String[] values = {"A"};
        assertEquals("A", Utils.getPreviousValue(values, "A"));
    }

    @Test
    void testGetPreviousValueWithEnums() {
        TestEnum[] values = TestEnum.values();
        assertEquals(TestEnum.THREE, Utils.getPreviousValue(values, TestEnum.ONE));
        assertEquals(TestEnum.TWO, Utils.getPreviousValue(values, TestEnum.THREE));
    }

    // --- getPermissionValue ---

    @Test
    void testGetPermissionValueReturnsMatchingSuffix() {
        PermissionAttachmentInfo perm = mock(PermissionAttachmentInfo.class);
        when(perm.getPermission()).thenReturn("island.level.5");
        when(user.getEffectivePermissions()).thenReturn(Set.of(perm));

        String result = Utils.getPermissionValue(user, "island.level", "0");
        assertEquals("5", result);
    }

    @Test
    void testGetPermissionValueSkipsWildcard() {
        PermissionAttachmentInfo wildcard = mock(PermissionAttachmentInfo.class);
        when(wildcard.getPermission()).thenReturn("island.level.*");
        when(user.getEffectivePermissions()).thenReturn(Set.of(wildcard));

        String result = Utils.getPermissionValue(user, "island.level", "default");
        assertEquals("default", result);
    }

    @Test
    void testGetPermissionValueNoMatchReturnsDefault() {
        when(user.getEffectivePermissions()).thenReturn(Set.of());

        String result = Utils.getPermissionValue(user, "island.level", "default");
        assertEquals("default", result);
    }

    @Test
    void testGetPermissionValueNotPlayerReturnsDefault() {
        when(user.isPlayer()).thenReturn(false);

        String result = Utils.getPermissionValue(user, "island.level", "default");
        assertEquals("default", result);
    }

    @Test
    void testGetPermissionValueStripsTrailingDot() {
        PermissionAttachmentInfo perm = mock(PermissionAttachmentInfo.class);
        when(perm.getPermission()).thenReturn("island.level.10");
        // Pass with trailing dot - method should strip it
        when(user.getEffectivePermissions()).thenReturn(Set.of(perm));

        String result = Utils.getPermissionValue(user, "island.level.", "0");
        assertEquals("10", result);
    }

    @Test
    void testGetPermissionValuePrefixNotMatchingOtherPerms() {
        PermissionAttachmentInfo perm = mock(PermissionAttachmentInfo.class);
        when(perm.getPermission()).thenReturn("island.other.5");
        when(user.getEffectivePermissions()).thenReturn(Set.of(perm));

        String result = Utils.getPermissionValue(user, "island.level", "default");
        assertEquals("default", result);
    }

    // --- Helper enum ---

    private enum TestEnum {
        ONE, TWO, THREE
    }
}
