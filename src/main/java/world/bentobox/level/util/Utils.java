//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.level.util;


import java.util.List;

import org.bukkit.Material;
import org.bukkit.permissions.PermissionAttachmentInfo;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.hooks.LangUtilsHook;


public class Utils
{
    private static final String LEVEL_MATERIALS = "level.materials.";

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
     * Prettify Material object for user.
     * @param object Object that must be pretty.
     * @param user User who will see the object.
     * @return Prettified string for Material.
     */
    public static String prettifyObject(Material object, User user)
    {
        // Nothing to translate
        if (object == null)
        {
            return "";
        }

        // Find addon structure with:
        // [addon]:
        //   materials:
        //     [material]:
        //       name: [name]
        String translation = user.getTranslationOrNothing(LEVEL_MATERIALS + object.name().toLowerCase() + ".name");

        if (!translation.isEmpty())
        {
            // We found our translation.
            return translation;
        }

        // Find addon structure with:
        // [addon]:
        //   materials:
        //     [material]: [name]

        translation = user.getTranslationOrNothing(LEVEL_MATERIALS + object.name().toLowerCase());

        if (!translation.isEmpty())
        {
            // We found our translation.
            return translation;
        }

        // Find general structure with:
        // materials:
        //   [material]: [name]

        translation = user.getTranslationOrNothing("materials." + object.name().toLowerCase());

        if (!translation.isEmpty())
        {
            // We found our translation.
            return translation;
        }

        // Use Lang Utils Hook to translate material
        return LangUtilsHook.getMaterialName(object, user);
    }


    /**
     * Prettify Material object description for user.
     * @param object Object that must be pretty.
     * @param user User who will see the object.
     * @return Prettified description string for Material.
     */
    public static String prettifyDescription(Material object, User user)
    {
        // Nothing to translate
        if (object == null)
        {
            return "";
        }

        // Find addon structure with:
        // [addon]:
        //   materials:
        //     [material]:
        //       description: [text]
        String translation = user.getTranslationOrNothing(LEVEL_MATERIALS + object.name().toLowerCase() + ".description");

        if (!translation.isEmpty())
        {
            // We found our translation.
            return translation;
        }

        // No text to return.
        return "";
    }
}
