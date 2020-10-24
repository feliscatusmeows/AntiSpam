package org.zeroBzeroT.antispam;

import org.bukkit.plugin.Plugin;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class UnicodeRanges {
    Plugin plugin;

    public UnicodeRanges(Plugin plugin) {
        this.plugin = plugin;

        plugin.saveResource("unicode_ranges.yml", false);
    }

    public int getUnicodeRangeCount(String sentence) {
        throw new NotImplementedException();
    }

    private String getUnicodeRangeName(char character) {
        throw new NotImplementedException();
    }
}
