//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.level.util;


import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.permissions.PermissionAttachmentInfo;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.hooks.LangUtilsHook;


public class Utils
{

    private Utils() {} // Private constructor as this is a utility class only with static methods

    /**
     * This method sends a message to the user with appended "prefix" text before message.
     * @param user User who receives message.
     * @param translationText Translation text of the message.
     * @param parameters Parameters for the translation text.
     */
    public static void sendMessage(User user, String translationText, String... parameters)
    {
        user.sendMessage(user.getTranslation( "level.conversations.prefix") +
                user.getTranslation( translationText, parameters));
    }


    /**
     * This method gets string value of given permission prefix. If user does not have given permission or it have all
     * (*), then return default value.
     *
     * @param user User who's permission should be checked.
     * @param permissionPrefix Prefix that need to be found.
     * @param defaultValue Default value that will be returned if permission not found.
     * @return String value that follows permissionPrefix.
     */
    public static String getPermissionValue(User user, String permissionPrefix, String defaultValue)
    {
        if (user.isPlayer())
        {
            if (permissionPrefix.endsWith("."))
            {
                permissionPrefix = permissionPrefix.substring(0, permissionPrefix.length() - 1);
            }

            String permPrefix = permissionPrefix + ".";

            List<String> permissions = user.getEffectivePermissions().stream().
                    map(PermissionAttachmentInfo::getPermission).
                    filter(permission -> permission.startsWith(permPrefix)).
                    toList();

            for (String permission : permissions)
            {
                if (permission.contains(permPrefix + "*"))
                {
                    // * means all. So continue to search more specific.
                    continue;
                }

                String[] parts = permission.split(permPrefix);

                if (parts.length > 1)
                {
                    return parts[1];
                }
            }
        }

        return defaultValue;
    }


    /**
     * This method allows to get next value from array list after given value.
     *
     * @param values Array that should be searched for given value.
     * @param currentValue Value which next element should be found.
     * @param <T> Instance of given object.
     * @return Next value after currentValue in values array.
     */
    public static <T> T getNextValue(T[] values, T currentValue)
    {
        for (int i = 0; i < values.length; i++)
        {
            if (values[i].equals(currentValue))
            {
                if (i + 1 == values.length)
                {
                    return values[0];
                }
                else
                {
                    return values[i + 1];
                }
            }
        }

        return currentValue;
    }


    /**
     * This method allows to get previous value from array list after given value.
     *
     * @param values Array that should be searched for given value.
     * @param currentValue Value which previous element should be found.
     * @param <T> Instance of given object.
     * @return Previous value before currentValue in values array.
     */
    public static <T> T getPreviousValue(T[] values, T currentValue)
    {
        for (int i = 0; i < values.length; i++)
        {
            if (values[i].equals(currentValue))
            {
                if (i > 0)
                {
                    return values[i - 1];
                }
                else
                {
                    return values[values.length - 1];
                }
            }
        }

        return currentValue;
    }

    /**
     * Reference string in translations.
     */
    public static final String ENTITIES = "level.entities.";
    private static final String LEVEL_MATERIALS = "level.materials.";
    private static final String DESCRIPTION = ".description";

    public static String prettifyObject(Object object, User user) {
        if (object == null) {
            return "";
        }
        // All supported objects are enums so we can use name() safely.
        String translation = "";

        if (object instanceof Material || object instanceof String) {
            String key = "";
            if (object instanceof Material) {
                key = ((Enum<?>) object).name().toLowerCase();
            } else {
                key = (String) object;
                // Remove prefix
                if (key.startsWith("oraxen:")) {
                    key = key.substring(7);
                }
            }

            // Try our translations for Material.
            translation = user.getTranslationOrNothing(LEVEL_MATERIALS + key + ".name");
            if (!translation.isEmpty())
                return translation;

            translation = user.getTranslationOrNothing(LEVEL_MATERIALS + key);
            if (!translation.isEmpty())
                return translation;

            translation = user.getTranslationOrNothing("materials." + key);
            if (!translation.isEmpty())
                return translation;

            if (object instanceof Material) {
                // Fallback to our hook for Material.
                return LangUtilsHook.getMaterialName((Material) object, user);
            } else {
                return world.bentobox.bentobox.util.Util.prettifyText(key);
            }
        } else if (object instanceof EntityType) {
            String key = ((Enum<?>) object).name().toLowerCase();
            // Try our translations for EntityType.
            translation = user.getTranslationOrNothing(ENTITIES + key + ".name");
            if (!translation.isEmpty())
                return translation;

            translation = user.getTranslationOrNothing(ENTITIES + key);
            if (!translation.isEmpty())
                return translation;

            translation = user.getTranslationOrNothing("entities." + key);
            if (!translation.isEmpty())
                return translation;

            // Fallback to our hook for EntityType.
            return LangUtilsHook.getEntityName((EntityType) object, user);
        }

        // In case of an unexpected type, return an empty string.
        return "";
    }

    public static String prettifyDescription(Object object, User user) {
        if (object instanceof String key) {
            String translation = user.getTranslationOrNothing(LEVEL_MATERIALS + key + DESCRIPTION);
            return translation != null ? translation : "";
        }
        if (object == null || !(object instanceof Enum<?>)) {
            return "";
        }
        String key = ((Enum<?>) object).name().toLowerCase();

        if (object instanceof Material) {
            String translation = user.getTranslationOrNothing(LEVEL_MATERIALS + key + DESCRIPTION);
            return translation != null ? translation : "";
        } else if (object instanceof EntityType) {
            String translation = user.getTranslationOrNothing(ENTITIES + key + DESCRIPTION);
            return translation != null ? translation : "";
        }

        return "";
    }
}
