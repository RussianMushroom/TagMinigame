package org.aurora.tag.game;

import org.aurora.tag.TagManager;
import org.aurora.tag.config.ConfigLoader;
import org.aurora.tag.leaderboard.LeaderboardManager;
import org.aurora.tag.util.InventoryManager;
import org.aurora.tag.util.Timer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * 
 * @author RussianMushroom
 *
 */
public class GameCenter {
	
	public static void start() {
		Bukkit.broadcastMessage(ChatColor.GOLD
				+ ConfigLoader.getDefault("Tag.Strings.GameStart"));
		
		// Warp everyone to the arena
		TagManager.getVotedPlayers().forEach(player -> {
			TagManager.legalWarp(ConfigLoader.getDefault("Tag.Arena.Arena"), player);
		});
		
		// Clear all inventories and apply items 
		InventoryManager.clearPlayerInventory();
		
		InventoryManager.setTagBaton();
		InventoryManager.setArmour();
		
		// Start grace period countdown
		Timer.startGraceTimer();
	}
	
	public static void stop() {
		if(TagManager.isActive())
			Bukkit.broadcastMessage(ChatColor.GOLD
					+ ConfigLoader.getDefault("Tag.Strings.GameStop"));
		
		forceStop();
	}
	
	public static void forceStop() {
		// Clear inventories and set game to inactive
				InventoryManager.clearPlayerInventory();
				
				TagManager.deactivate();
				Timer.disableTimers();
	}
	
	public static void registerWinner(Player player) {
		// Warp all the users back to the Lounge and clear their inventories
		// Update leaderboard
		LeaderboardManager.add(player, true);
		TagManager.getVotedPlayers().forEach(p -> {
			TagManager.legalWarp(ConfigLoader.getDefault("Tag.Arena.Lobby"), p);
			LeaderboardManager.add(p, false);
		});
		InventoryManager.clearPlayerInventory();
		
		// Broadcast the player's win to the server
		Bukkit.broadcastMessage(ChatColor.GOLD
				+ String.format(ConfigLoader.getDefault("Tag.Strings.BroadcastWinner"), player.getName()));
		
		// Reopen the game
		stop();
		
		// Give the winner their reward
		InventoryManager.setWinnerReward(player);
		

	}
	
}
