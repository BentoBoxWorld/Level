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
public class UtilsTest {

    private User user;

    @BeforeEach
    public void setUp() {
        user = mock(User.class);
        when(user.isPlayer()).thenReturn(true);
    }

    // --- getNextValue ---

    @Test
    public void testGetNextValueMidArray() {
        String[] values = {"A", "B", "C"};
        assertEquals("B", Utils.getNextValue(values, "A"));
    }

    @Test
    public void testGetNextValueLastWrapsToFirst() {
        String[] values = {"A", "B", "C"};
        assertEquals("A", Utils.getNextValue(values, "C"));
    }

    @Test
    public void testGetNextValueMiddleElement() {
        String[] values = {"A", "B", "C"};
        assertEquals("C", Utils.getNextValue(values, "B"));
    }

    @Test
    public void testGetNextValueNotFoundReturnsCurrent() {
        String[] values = {"A", "B", "C"};
        assertEquals("Z", Utils.getNextValue(values, "Z"));
    }

    @Test
    public void testGetNextValueSingleElement() {
        String[] values = {"A"};
        assertEquals("A", Utils.getNextValue(values, "A"));
    }

    @Test
    public void testGetNextValueWithEnums() {
        TestEnum[] values = TestEnum.values();
        assertEquals(TestEnum.TWO, Utils.getNextValue(values, TestEnum.ONE));
        assertEquals(TestEnum.ONE, Utils.getNextValue(values, TestEnum.THREE));
    }

    // --- getPreviousValue ---

    @Test
    public void testGetPreviousValueMidArray() {
        String[] values = {"A", "B", "C"};
        assertEquals("A", Utils.getPreviousValue(values, "B"));
    }

    @Test
    public void testGetPreviousValueFirstWrapsToLast() {
        String[] values = {"A", "B", "C"};
        assertEquals("C", Utils.getPreviousValue(values, "A"));
    }

    @Test
    public void testGetPreviousValueLastElement() {
        String[] values = {"A", "B", "C"};
        assertEquals("B", Utils.getPreviousValue(values, "C"));
    }

    @Test
    public void testGetPreviousValueNotFoundReturnsCurrent() {
        String[] values = {"A", "B", "C"};
        assertEquals("Z", Utils.getPreviousValue(values, "Z"));
    }

    @Test
    public void testGetPreviousValueSingleElement() {
        String[] values = {"A"};
        assertEquals("A", Utils.getPreviousValue(values, "A"));
    }

    @Test
    public void testGetPreviousValueWithEnums() {
        TestEnum[] values = TestEnum.values();
        assertEquals(TestEnum.THREE, Utils.getPreviousValue(values, TestEnum.ONE));
        assertEquals(TestEnum.TWO, Utils.getPreviousValue(values, TestEnum.THREE));
    }

    // --- getPermissionValue ---

    @Test
    public void testGetPermissionValueReturnsMatchingSuffix() {
        PermissionAttachmentInfo perm = mock(PermissionAttachmentInfo.class);
        when(perm.getPermission()).thenReturn("island.level.5");
        when(user.getEffectivePermissions()).thenReturn(Set.of(perm));

        String result = Utils.getPermissionValue(user, "island.level", "0");
        assertEquals("5", result);
    }

    @Test
    public void testGetPermissionValueSkipsWildcard() {
        PermissionAttachmentInfo wildcard = mock(PermissionAttachmentInfo.class);
        when(wildcard.getPermission()).thenReturn("island.level.*");
        when(user.getEffectivePermissions()).thenReturn(Set.of(wildcard));

        String result = Utils.getPermissionValue(user, "island.level", "default");
        assertEquals("default", result);
    }

    @Test
    public void testGetPermissionValueNoMatchReturnsDefault() {
        when(user.getEffectivePermissions()).thenReturn(Set.of());

        String result = Utils.getPermissionValue(user, "island.level", "default");
        assertEquals("default", result);
    }

    @Test
    public void testGetPermissionValueNotPlayerReturnsDefault() {
        when(user.isPlayer()).thenReturn(false);

        String result = Utils.getPermissionValue(user, "island.level", "default");
        assertEquals("default", result);
    }

    @Test
    public void testGetPermissionValueStripsTrailingDot() {
        PermissionAttachmentInfo perm = mock(PermissionAttachmentInfo.class);
        when(perm.getPermission()).thenReturn("island.level.10");
        // Pass with trailing dot - method should strip it
        when(user.getEffectivePermissions()).thenReturn(Set.of(perm));

        String result = Utils.getPermissionValue(user, "island.level.", "0");
        assertEquals("10", result);
    }

    @Test
    public void testGetPermissionValuePrefixNotMatchingOtherPerms() {
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
