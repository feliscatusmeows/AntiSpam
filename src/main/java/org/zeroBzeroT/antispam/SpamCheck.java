package org.zeroBzeroT.antispam;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

public class SpamCheck {
    static final int maxBadSentencesSaved = 64;
    static int minMessageLength = 8;
    // factor of the message difference - TODO: Config vars
    static double msgDiffFactor = 1d / 5d;
    static int maxDuplicates = 2;
    static int maxSentencesSaved = 128;
    static int perPlayerQueueSizeFactor = 5;

    // the last [maxSentencesSaved] chat messages for comparison
    final LimitedSizeQueue<String> lastMessages = new LimitedSizeQueue<>(maxSentencesSaved);

    // the last [maxBadSentencesSaved] spam chat messages for comparison
    final LimitedSizeQueue<String> lastSpamMessages = new LimitedSizeQueue<>(maxBadSentencesSaved);

    // sanitizing message from chars that are from unicode ranges that are only used a few times in that message
    private UnicodeRanges unicodeRanges;

    public SpamCheck(Plugin plugin) {
        try {
            unicodeRanges = new UnicodeRanges(plugin);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    // distance between two words - the minimum number of single-character edits
    static int calculateLevenshtein(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(dp[i - 1][j - 1] + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
                            dp[i - 1][j] + 1, dp[i][j - 1] + 1);
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    public static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    public static int min(int... numbers) {
        return Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
    }

    // Checks message for spam
    public boolean isSpam(Player p, String message) {
        // Bots - TODO: UUID - use contains without case
        for (String bot : AntiSpam.bots) {
            if (bot.toLowerCase().contentEquals(p.getName().toLowerCase())) {
                return false;
            }
        }

        // use unicode ranges to sanitize text
        String saniMsg = unicodeRanges.sanitizeText(message, minMessageLength);

        // from [minMessageLength] character length
        if (saniMsg.length() < minMessageLength)
            return false;

        // remove hashcodes
        saniMsg = saniMsg.replaceAll("[^a-zA-Z0-9](?=([a-zA-Z]*\\d))\\S{4,}[^a-zA-Z0-9]", "");

        // remove camelcase - why?!?
        saniMsg = saniMsg.replaceAll("[^a-zA-Z0-9](?=([a-z]+[A-Z]+|[A-Z]+[a-z]+){2})\\S{3,}[^a-zA-Z0-9]", "");

        // remove non printable chars and spaces
        saniMsg = saniMsg.replaceAll("[\\p{C} ]", "");

        saniMsg = saniMsg.toLowerCase();

        int cntDuplicates = 0;

        // has the same already been written?
        for (String oldMsg : new LinkedList<>(lastMessages)) { // copy contents to new object to avoid concurrent modification by async chat event handling
            // difference in length of the messages is already greater than the factor
            if (Math.abs(oldMsg.length() - saniMsg.length()) > Math.max(oldMsg.length(), saniMsg.length()) * msgDiffFactor)
                continue;

            // Levenshtein distance - strings are similar
            if (calculateLevenshtein(oldMsg, saniMsg) < saniMsg.length() * msgDiffFactor) {
                cntDuplicates++;

                if (cntDuplicates >= maxDuplicates)
                    break;
            }
        }

        // we dont need to add the message if its already in the list (really?
        // drawbacks?)
        //if (cntDuplicates < maxDuplicates) {
        lastMessages.add(saniMsg);
        //}

        if (cntDuplicates >= maxDuplicates) {
            // is Spam
            if (!lastSpamMessages.contains(saniMsg))
                lastSpamMessages.add(saniMsg);

            return true;
        } else {
            // Messages seems to be ok - so check the last spam messages
            for (String oldSpam : new LinkedList<>(lastSpamMessages)) {
                // difference in length of the messages is already greater than the factor
                if (Math.abs(oldSpam.length() - saniMsg.length()) > Math.max(oldSpam.length(), saniMsg.length()) * msgDiffFactor)
                    continue;

                // Levenshtein distance - strings are similar
                if (calculateLevenshtein(oldSpam, saniMsg) < saniMsg.length() * msgDiffFactor) {
                    return true;
                }
            }
        }

        return false;
    }

    public void setPlayerCount(int count) {
        lastMessages.setSize(Math.max(maxSentencesSaved, count * perPlayerQueueSizeFactor));
    }
}
