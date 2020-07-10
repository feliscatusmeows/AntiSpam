package com.zeroBzeroT;

import org.bukkit.entity.Player;

import java.util.Arrays;

public class SpamCheck {

	// factor of the message difference - TODO: Config vars
	static double msgDiffFac = 1d / 5d;
	static int maxDuplicates = 2;
	static int maxSentencesSaved = 100;
	static int minMessageLength = 8;

	// the last [maxSentencesSaved] chat messages for comparison
	LimitedSizeQueue<String> lastMessages = new LimitedSizeQueue<String>(maxSentencesSaved);

	// Checks message for spam
	public boolean isSpam(Player p, String message) {
		// Bots - TODO: UUID - use contains without case
		for (String bot : AntiSpamBot.bots) {
			if (bot.toLowerCase().contentEquals(p.getName().toLowerCase())) {
				return false;
			}
		}

		// remove hashs
		// TODO

		// remove non printable chars and spaces
		String saniMsg = message.replaceAll("[\\p{C} ]", "").toLowerCase();

		// from [minMessageLength] character length
		if (saniMsg.length() < minMessageLength)
			return false;

		int cntDuplicates = 0;

		// has the same already been written?
		for (String oldMsg : lastMessages) {
			// difference in length of the messages is already greater than the factor
			if (Math.abs(oldMsg.length() - saniMsg.length()) > Math.max(oldMsg.length(), saniMsg.length()) * msgDiffFac)
				continue;

			// Levenshtein distance - strings are similar
			if (calculateLevenshtein(oldMsg, saniMsg) < saniMsg.length() * msgDiffFac) {
				cntDuplicates++;

				if (cntDuplicates > maxDuplicates)
					break;
			}
		}

		// we dont need to add the message if its already in the list (really?
		// drawbacks?)
		if (cntDuplicates <= maxDuplicates) {
			lastMessages.add(saniMsg);
		}

		return cntDuplicates > maxDuplicates ? true : false;
	}

	public void setPlayerCount(int count) {
		lastMessages.setSize(Math.max(maxSentencesSaved, count * 5));
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
}
