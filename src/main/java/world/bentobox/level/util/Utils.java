//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.level.util;


import org.bukkit.permissions.PermissionAttachmentInfo;
import java.util.List;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.user.User;


public class Utils
{
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
                collect(Collectors.toList());

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
}
