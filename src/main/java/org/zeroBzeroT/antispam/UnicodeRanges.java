package org.zeroBzeroT.antispam;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;

public class UnicodeRanges {
    Plugin plugin;

    YamlConfiguration config = new YamlConfiguration();

    public UnicodeRanges(Plugin plugin) throws IOException, InvalidConfigurationException {
        this.plugin = plugin;

        plugin.saveResource("unicode_ranges.yml", false);

        config.load(plugin.getDataFolder() + "/unicode_ranges.yml");
    }

    public int getUnicodeRangeCount(String sentence) {
        int count = 0;

        for(String key : config.getConfigurationSection("kills").getKeys(false)) {
            System.out.println(config.getString(key));
            count ++;
        }

        return count;
    }

    private String getUnicodeRangeName(char character) {
        throw new NotImplementedException();
    }
}
