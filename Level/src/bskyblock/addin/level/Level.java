package bskyblock.addin.level;

import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.ArgumentHandler;
import us.tastybento.bskyblock.api.commands.CanUseResp;

public class Level extends JavaPlugin {
    
    @Override
    public void onEnable(){
        BSkyBlock plugin = BSkyBlock.getPlugin();
        plugin.getIslandCommand().addSubCommand(new ArgumentHandler("island") {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                sender.sendMessage("Your island level is XXX");
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{null, "Calculate your island's level"};
            }
        }.alias("level"));
        
        
    }
    
    @Override
    public void onDisable(){
        
    }

}
