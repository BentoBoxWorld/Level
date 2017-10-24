package bskyblock.addin.level;

import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.ArgumentHandler;
import us.tastybento.bskyblock.api.commands.CanUseResp;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

public class Commands extends CalculateLevel {

    public Commands(Level plugin) {
        super(plugin);
        setupCommands();
    }

    private void setupCommands() {
        // level command
        bSkyBlock.addSubCommand(new ArgumentHandler("island") {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                //getLogger().info("DEBUG: " + args);
                if (args.length > 0) {
                    // Asking for another player's level?
                    // Convert name to a UUID
                    final UUID playerUUID = bSkyBlock.getPlayers().getUUID(args[0], true);
                    //getLogger().info("DEBUG: console player info UUID = " + playerUUID);
                    if (playerUUID == null) {
                        sendMessage(sender, ChatColor.RED + getLocale(sender).get("error.UnknownPlayer"));
                        return;
                    } else {
                        sendMessage(sender, ChatColor.GREEN + "Level is " + plugin.getIslandLevel(playerUUID));
                        return;
                    }
                }
                if (sender instanceof Player) {
                    Player player = (Player)sender;
                    UUID playerUUID = player.getUniqueId();

                    if (VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.info")) {
                        if (!bSkyBlock.getPlayers().inTeam(playerUUID) && !bSkyBlock.getPlayers().hasIsland(playerUUID)) {
                            Util.sendMessage(player, ChatColor.RED + bSkyBlock.getLocale(sender).get("errors.no-island"));
                            return;
                        } else {
                            calculateIslandLevel(player, playerUUID);
                            return;
                        }
                    } else {
                        Util.sendMessage(player, ChatColor.RED + bSkyBlock.getLocale(sender).get("errors.no-permission"));
                        return;
                    }
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"[player]", "See your island's level or someone else's"};
            }
        }.alias("level"));

        // top command
        bSkyBlock.addSubCommand(new ArgumentHandler("island") {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (sender instanceof Player) {
                    VaultHelper.hasPerm((Player)sender, Settings.PERMPREFIX + "island.topten");
                    return new CanUseResp(true);
                }
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                plugin.getTopTen().topTenShow((Player)sender);
                return;
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"", "View top ten"};
            }
        }.alias("top"));

        // Admin level command
        bSkyBlock.addSubCommand(new ArgumentHandler("bsadmin") {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                if (args.length == 0) {

                } else {
                    // Convert name to a UUID
                    final UUID playerUUID = bSkyBlock.getPlayers().getUUID(args[0], true);
                    //plugin.getLogger().info("DEBUG: console player info UUID = " + playerUUID);
                    if (playerUUID == null) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.getLocale(sender).get("error.UnknownPlayer"));
                        return;
                    } else {
                        if (sender instanceof Player) {
                            calculateIslandLevel(sender, playerUUID, false); 
                        } else {
                            calculateIslandLevel(sender, playerUUID, true);
                        }
                        return;
                    }
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"[player]", "Calculate a player's island's level"};
            }
        }.alias("level"));
    }


}
