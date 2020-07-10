package com.zeroBzeroT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bukkit.BanList.Type;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiSpamBot extends JavaPlugin implements Listener {
	ArrayList<Player> notMoved = new ArrayList<Player>();
	FileConfiguration config;
	public static List<String> bots = new ArrayList<String>();
	List<String> whisperCommands = Arrays.asList("tell", "w", "msg", "whisper");
	private SpamCheck spamBotCheck;

	@Override
	public void onEnable() {
		spamBotCheck = new SpamCheck();

		saveDefaultConfig();
		PluginManager pm = Bukkit.getServer().getPluginManager();
		pm.registerEvents(this, this);

		config = this.getConfig();

		bots = config.getStringList("bots");
	}

	@Override
	public void onDisable() {
		try {
			saveConfig();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player player = (Player) sender;

		if (cmd.getName().equalsIgnoreCase("movereload")) {
			if (player.hasPermission("move.reload")) {
				reloadConfig();
				player.sendMessage(
						ChatColor.translateAlternateColorCodes('&', getConfig().getString("reload-message")));
				return true;
			}

			player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("noPermissions")));
			return true;
		}

		return false;
	}

	@EventHandler
	public void onPlayerJoinEvent(PlayerLoginEvent event) {
		Player player = event.getPlayer();

		addNotMoved(player);
	}

	@EventHandler
	public void onPlayerLeaveEvent(PlayerQuitEvent event) {
		int playersOnline = getServer().getOnlinePlayers().size();
		Player player = event.getPlayer();
		delNotMoved(player);
		spamBotCheck.setPlayerCount(playersOnline);
	}

	// chat spam check
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerChatEvent(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		String message = event.getMessage();

		if (this.notMoved.contains(player)) {
			event.setCancelled(true);
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("cannot-talk")));
			return;
		}

		boolean check = spamBotCheck.isSpam(player, message);

		if (check) {
			event.setCancelled(true);

			player.sendMessage("§ePlease §cdo not always write the same§e, it gets boring and leads to a §ckick.");

			log("AsyncPlayerChatEvent", player.getName() + " message has been discarded. " + check);
			return;
		}
	}

	// TODO: try to load plugin before chatco and just block the whisphering
	// whisper spam check
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) throws IOException {
		Player player = event.getPlayer();

		String inputText = event.getMessage();

		if (whisperCommands.stream().anyMatch(cmd -> inputText.toLowerCase().startsWith("/" + cmd + " "))) {
			String[] args = inputText.split("\\ ", 3);

			if (args.length == 3) {
				if (spamBotCheck.isSpam(player, args[2])) {
					event.setCancelled(true);

					TempBanPlayer(player,
							"You have been temp banned for spam. If you like to appeal please msg Leee✓ᵛᵉʳᶦᶠᶦᵉᵈ.");

					log("PlayerCommandPreprocessEvent", player.getName() + " has been temp banned for whisper spam.");

					return;
				}
			}
		}
	}

	@EventHandler
	public void onPlayerMoveEvent(PlayerMoveEvent e) {
		// check if the player has moved over a block border
		if (e.getTo().getBlockX() == e.getFrom().getBlockX() && e.getTo().getBlockZ() == e.getFrom().getBlockZ())
			return;

		Player player = e.getPlayer();

		if (this.notMoved.contains(player)) {
			delNotMoved(player);
		}
	}

	private void addNotMoved(Player player) {
		for (String bot : bots) {
			if (bot.toLowerCase().contentEquals(player.getName().toLowerCase())) {
				log("BOT", player.getName());
				return;
			}
		}

		if (!player.hasPermission("move.bypass")) {
			this.notMoved.add(player);
		}
	}

	private void delNotMoved(Player player) {
		this.notMoved.remove(player);
	}

	public void TempBanPlayer(Player player, String message) {
		Bukkit.getScheduler().runTask(this, new Runnable() {
			public void run() {
				Bukkit.getBanList(Type.NAME).addBan(player.getName(), message,
						new Date(System.currentTimeMillis() + 30 * 1000), null);

				player.kickPlayer(message);
			}
		});
	}

	public void log(String module, String message) {
		getLogger().info("§a[" + module + "] §e" + message + "§r");
	}
}
