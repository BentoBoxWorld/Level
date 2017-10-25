package bskyblock.addin.level.commands;

import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import bskyblock.addin.level.CalculateLevel;
import bskyblock.addin.level.Level;
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
        // island level command
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

        // island top command
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
                plugin.getTopTen().getGUI((Player)sender);
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

        // admin top command
        bSkyBlock.addSubCommand(new ArgumentHandler("bsadmin") {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (sender instanceof Player) {
                    VaultHelper.hasPerm((Player)sender, Settings.PERMPREFIX + "admin.topten");
                    return new CanUseResp(true);
                }
                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                int rank = 0;
                for (Entry<UUID, Long> topTen : plugin.getTopTen().getTopTenList().getTopTen().entrySet()) {
                    UUID player = topTen.getKey();
                    rank++;
                    String item = String.valueOf(rank) + ":" + BSkyBlock.getPlugin().getIslands().getIslandName(player) + " "
                            + plugin.getLocale(sender).get("topten.islandLevel").replace("[level]", String.valueOf(topTen.getValue()));
                    Util.sendMessage(sender, item);
                }
                return;
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"", "List top ten"};
            }
        }.alias("top"));
    }


}
