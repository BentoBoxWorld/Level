package bskyblock.addin.level.config;

import java.util.UUID;

import bskyblock.addin.level.Level;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.AbstractLocaleManager;
import us.tastybento.bskyblock.config.BSBLocale;
import us.tastybento.bskyblock.config.Settings;

public class LocaleManager extends AbstractLocaleManager {

    public LocaleManager(Level plugin) {
        super(plugin);
    }

    @Override
    public BSBLocale getLocale(UUID player) {
        //getLogger().info("DEBUG: " + player);
        //getLogger().info("DEBUG: " + getPlayers() == null ? "Players is null":"Players in not null");
        //getLogger().info("DEBUG: " + getPlayers().getPlayer(player));
        //getLogger().info("DEBUG: " + getPlayers().getPlayer(player).getLocale());
        String locale = BSkyBlock.getPlugin().getPlayers().getPlayer(player).getLocale();
        if(locale.isEmpty() || !getLocales().containsKey(locale)) return getLocales().get(Settings.defaultLanguage);

        return getLocales().get(locale);
    }
}
