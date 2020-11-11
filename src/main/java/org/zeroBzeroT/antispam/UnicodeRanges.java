package org.zeroBzeroT.antispam;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnicodeRanges {
    final Plugin plugin;

    List<List<Object>> unicodeRanges;

    public UnicodeRanges(Plugin plugin) throws IOException, InvalidConfigurationException {
        this.plugin = plugin;

        plugin.saveResource("unicode_ranges.yml", false);

        YamlConfiguration config = new YamlConfiguration();

        config.load(plugin.getDataFolder() + "/unicode_ranges.yml");

        unicodeRanges = (List<List<Object>>) config.getList("ranges");
    }

    public String sanitizeText(String text, int minimalPurgeLength) {
        Map<List<Object>, Integer> count = new HashMap<>();

        // count the chars for each unicode range
        for (int i = 0; i < text.length(); i++) {
            for (List<Object> range : unicodeRanges) {
                int unicode = text.codePointAt(i);

                if ((int) range.get(0) <= unicode && unicode <= (int) range.get(1)) {
                    if (count.containsKey(range)) {
                        count.put(range, count.get(range) + 1);
                    } else {
                        count.put(range, 1);
                    }

                    break;
                }

                // All chars from undefined ranges are discarded - That may not be the best idea ;) - With <3 0bOp
            }
        }

        // get the range with the most chars
        Map.Entry<List<Object>, Integer> maxRange = null;

        for (Map.Entry<List<Object>, Integer> entry : count.entrySet()) {
            if (maxRange == null || entry.getValue().compareTo(maxRange.getValue()) > 0) {
                maxRange = entry;
            }
        }

        // only remove the chars from other ranges if the text is long enough afterwards
        if (maxRange.getValue() > minimalPurgeLength) {
            StringBuilder newText = new StringBuilder();

            for (int i = 0; i < text.length(); i++) {
                int unicode = text.codePointAt(i);

                if ((int) maxRange.getKey().get(0) <= unicode && unicode <= (int) maxRange.getKey().get(1)) {
                    newText.append(text.charAt(i));
                }
            }

            // return the text without chars from other ranges
            return newText.toString();
        }

        // return the old text if its not possible to remove chars
        return text;
    }
}
