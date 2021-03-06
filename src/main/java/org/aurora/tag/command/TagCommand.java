package org.aurora.tag.command;

import java.util.List;
import java.util.stream.Collectors;

import org.aurora.tag.Tag;
import org.aurora.tag.config.ArenaConfig;
import org.aurora.tag.config.ConfigLoader;
import org.aurora.tag.game.GameCenter;
import org.aurora.tag.game.TagArena;
import org.aurora.tag.manager.LeaderboardManager;
import org.aurora.tag.util.GeneralMethods;
import org.aurora.tag.util.MethodBypass;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import mkremins.fanciful.FancyMessage;

/**
 * Deals with the use of the tag command.
 * @author RussianMushroom
 *
 */
public class TagCommand {
	
	private static final int DEFAULT_TOP = 10;
	
	public static void handle(CommandSender sender, String[] args, Tag plugin) {
		
		// Check if the user has the necessary permissions
		if(args.length == 0 || args.length > 3) {
			if(sender.hasPermission("tag.help.advanced"))
				displayMenu(sender, true);
			else
				displayMenu(sender, false);
		} else {
			for(int i = 0; i < args.length; i++) {
				args[i] = args[i].toLowerCase();
			}
			Player player = null;
			if(!(sender instanceof ConsoleCommandSender))
				player = (Player) sender;
			
				switch (args[0].toLowerCase()) {
				// /tag join & /tag join confirm
				case "join":
				case "j":
					if(sender instanceof ConsoleCommandSender)
						sender.sendMessage(ChatColor.GOLD
								+ ConfigLoader.getDefault("Tag.Strings.ConsoleUser"));
					else {
						if(sender.hasPermission("tag.join")) {
							TagArena arena;
							if(args.length != 2)
								sender.sendMessage(ChatColor.GOLD
									+ ConfigLoader.getDefault("Tag.Strings.JoinSyntax"));

							else {
								if(GameCenter.availableArenas().contains(args[1])) {
									if(GameCenter.getArena(args[1]) == null) {
										arena = new TagArena(args[1]);
										GameCenter.addGame(arena);
									} else 
										arena = GameCenter.getArena(args[1]);
									
									if(GameCenter.arenaContainsPlayerAsType("joined", player))
											sender.sendMessage(ChatColor.GOLD
													+ ConfigLoader.getDefault("Tag.Strings.AlreadyInActiveGame"));
									else if(arena.getJoinedPlayers().contains(player)
											&& !arena.getVotedPlayers().contains(player)
											&& !arena.isActive())
										sender.sendMessage(ChatColor.GOLD
												+ ConfigLoader.getDefault("Tag.Strings.AlreadyInLobby"));
									else if(arena.getVotedPlayers().contains(player)
											&& !arena.isActive())
										sender.sendMessage(ChatColor.GOLD
												+ ConfigLoader.getDefault("Tag.Strings.PlayerAlreadyVote"));
									else if((arena.getVotedPlayers().contains(player) && arena.isActive()))
										sender.sendMessage(ChatColor.GOLD
												+ ConfigLoader.getDefault("Tag.Strings.AlreadyInActiveGame"));
									else if(arena.isActive())
										sender.sendMessage(ChatColor.GOLD
												+ ConfigLoader.getDefault("Tag.Strings.AlreadyActiveGameWait"));
									else {  
										if(GameCenter.arenaHasAllArenasSet(arena)) {
											handleJoin(player, arena);
											sender.sendMessage(ChatColor.GOLD
													+ ConfigLoader.getDefault("Tag.Strings.InventorySaved"));
										} 
										else
											sender.sendMessage(ChatColor.GOLD
													+ ConfigLoader.getDefault("Tag.Strings.ArenaNotSet"));
									}
									
								} else {
									sender.sendMessage(ChatColor.GOLD
											+ ConfigLoader.getDefault("Tag.Strings.ArenaDoesNotExist"));
								}		
							}
							
						} else
							notEnoughPermission(sender);	
					}	
					break;
					// /tag start
				case "start":	
					if(sender.hasPermission("tag.start")) {
						if(args.length == 2) {
							if(GameCenter.getArena(args[1]) != null) {
								if(GameCenter.getArena(args[1]).isActive())
									sender.sendMessage(ChatColor.GOLD
											+ ConfigLoader.getDefault("Tag.Strings.AlreadyActive"));
								else {
									GameCenter.getArena(args[1]).migrate();
									GameCenter.getArena(args[1]).delayStart();
								}
							}
						} else {
							sender.sendMessage(ChatColor.GOLD
									+ ConfigLoader.getDefault("Tag.Strings.InventorySaved"));
						}
					} else
						notEnoughPermission(sender);
					break;
					// /tag help
				case "help":
					if(sender.hasPermission("tag.help") || sender instanceof ConsoleCommandSender)
							displayHelpMenu(sender);
					else
						notEnoughPermission(sender);
					break;	
				// /tag leave
				case "leave":
					if(sender instanceof ConsoleCommandSender)
						sender.sendMessage(ChatColor.GOLD
								+ ConfigLoader.getDefault("Tag.Strings.ConsoleUser"));
					else {
						if(sender.hasPermission("tag.leave")) {
							if(!GameCenter.arenaContainsPlayerAsType("joined", player))
								sender.sendMessage(ChatColor.GOLD
										+ ConfigLoader.getDefault("Tag.Strings.PlayerNotInGame"));
							else {
								sender.sendMessage(ChatColor.GOLD
										+ ConfigLoader.getDefault("Tag.Strings.PlayerHasLeft"));
								GeneralMethods.displayMessage(GameCenter.getArena(player), 
										String.format(ConfigLoader.getDefault("Tag.Strings.PlayerLeaves"),
												player.getName()));
								GameCenter.getArena(player).removePlayer(player);
							}
						} else
							notEnoughPermission(sender);
					}
					break;
				// /tag stop
				case "stop":
					if(sender.hasPermission("tag.stop")) {
						if(args.length == 2) {
							if(GameCenter.getArena(args[1]) != null) {
								TagArena arena = GameCenter.getArena(args[1]);
								
								if(!arena.isActive())
									sender.sendMessage(ChatColor.GOLD
											+ ConfigLoader.getDefault("Tag.Strings.NotActive"));
								else
									GameCenter.stop(arena, false);
							}
							
						} else if(args.length == 1) {
							if(GameCenter.availableArenas().isEmpty())
								sender.sendMessage(ChatColor.GOLD
										+ ConfigLoader.getDefault("Tag.Strings.NotActive"));
							else
								GameCenter.getActiveGames().forEach(arena -> {
									GameCenter.stop(arena, false);
								});
						}
					} else
						notEnoughPermission(sender);
					
					break;
				// /tag set [arena|rip|lobby]
				case "set":
					if(sender instanceof ConsoleCommandSender)
						sender.sendMessage(ChatColor.GOLD
								+ ConfigLoader.getDefault("Tag.Strings.ConsoleUser"));
					else if(!sender.hasPermission("tag.set")) 
						notEnoughPermission(sender);
					else {
						if(args.length != 3) {
							sender.sendMessage(ChatColor.GOLD
									+ ConfigLoader.getDefault("Tag.Strings.SetSyntax"));
						} else {
							if(!ArenaConfig.getYamlConfig().contains("Arena." + args[2]))
								sender.sendMessage(ChatColor.GOLD
										+ ConfigLoader.getDefault("Tag.Strings.ArenaDoesNotExist"));
							else {
								if (args[1].equalsIgnoreCase("arena")) 
									setArena("Arena." + args[2] + ".Warps.Arena", sender);					
								else if(args[1].equalsIgnoreCase("lobby")) 
									setArena("Arena." + args[2] + ".Warps.Lobby", sender);
								else if(args[1].equalsIgnoreCase("rip")) 
									setArena("Arena." + args[2] + ".Warps.Rip", sender);
							}
						}
					}
					break;
				// /tag leaderboard
				case "leaderboard":
				case "lb":
					if(!sender.hasPermission("tag.leaderboard") && !(sender instanceof ConsoleCommandSender)) 
						notEnoughPermission(sender);
					else {
						displayLeaderboard(sender, (args.length == 2) ? args[1] : DEFAULT_TOP + "");
					}
					break;
				case "createarena":
				case "ca":
					if(!sender.hasPermission("tag.createarena") && !(sender instanceof ConsoleCommandSender))
						notEnoughPermission(sender);
					else if(args.length != 2)
						sender.sendMessage(ChatColor.GOLD
								+ ConfigLoader.getDefault("Tag.Strings.CreateSyntax"));
					else {
						if(!GameCenter.availableArenas().contains(args[1])) {
							ArenaConfig.set("Arena." + args[1], "");
							ArenaConfig.set("Arena." + args[1] + ".MaxPlayers", "10");
							sender.sendMessage(ChatColor.GOLD
									+ ConfigLoader.getDefault("Tag.Strings.ArenaCreated"));
						} else {
							sender.sendMessage(ChatColor.GOLD
									+ ConfigLoader.getDefault("Tag.Strings.ArenaAlreadyExists"));
						}
					}
					break;
				case "listarena":
				case "la":
					if(!sender.hasPermission("tag.createarena") && !(sender instanceof ConsoleCommandSender))
						notEnoughPermission(sender);
					else {
						if(GameCenter.availableArenas().isEmpty()) {
							String listedArenas = ConfigLoader.getDefault("Tag.Strings.AreNoArenas");
							sender.sendMessage(ChatColor.GOLD
									+ listedArenas);
						} else {
							String listedArenas = ConfigLoader.getDefault("Tag.Strings.ArenasList");
							sender.sendMessage(ChatColor.GOLD
									+ String.format(listedArenas, GameCenter.availableArenas()
											.stream()
											.map(arena -> ChatColor.YELLOW + GeneralMethods.toProperCase(arena))
											.collect(Collectors.joining(", "))));
						}	
					}
					break;
				case "status":
					if(!sender.hasPermission("tag.status") && !(sender instanceof ConsoleCommandSender))
						notEnoughPermission(sender);
					else {
						if(args.length == 2 && GameCenter.availableArenas().contains(args[1])) {
							if(GameCenter.getArena(args[1]) != null) {
								TagArena arena = GameCenter.getArena(args[1]);
								
								// import FancifulText and use the methods to display a json popup of the joined players.
								new FancyMessage(String.format(
										"%s%s%s%s%s%s",
										"===============================\n",
										"  Tag-Minigame " + GeneralMethods.toProperCase(args[1]) + ": \n",
										"===============================\n",
										"  Players joined: " + arena.getJoinedPlayers().size() + "/" + arena.getMaxPlayers() + "\n",
										"  Status: " + (arena.isActive() ? ChatColor.RED + "ACTIVE\n" : ChatColor.GREEN + "OPEN\n"),
										"===============================\n"
										))
									.color(ChatColor.AQUA)
									.tooltip(getJoinedPlayers(arena))
									.send(sender);
								
							} else {
								String maxPlayers = ArenaConfig.getDefault("Arena." + args[1] + ".MaxPlayers");
								
								sender.sendMessage(ChatColor.AQUA
										+ "===============================\n"
										+ "  Tag-Minigame " + GeneralMethods.toProperCase(args[1]) + ": \n"
										+ "===============================\n"
										+ "  Players joined: 0/" + maxPlayers + "\n"
										+ "  Status: " + ChatColor.GREEN + "OPEN\n" 
										+ ChatColor.AQUA + "===============================\n"
										);
								
							}
							
						}
						else 
							sender.sendMessage(ChatColor.GOLD
									+ ConfigLoader.getDefault("Tag.Strings.StatusSyntax"));
					}
					break;
			}
		}	
	}
	
	private static String getJoinedPlayers(TagArena arena) {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("Joined players: \n");
		
		arena.getJoinedPlayers()
			.stream()
			.map(player -> player.getName())
			.sorted()
			.forEach(player -> {
				sBuilder.append(ChatColor.YELLOW + "- " + player + "\n");
			});
		
		return sBuilder.toString();
	}
	
	
	private static void setArena(String path, CommandSender sender) {
		Player player = (Player) sender;
		Location location = (player).getLocation();
		
		ArenaConfig.set(
				path,
				String.format("%s_%s,%s,%s",
						(player).getWorld().getName(),
						location.getBlockX(),
						location.getBlockY(),
						location.getBlockZ())
				);
		sender.sendMessage(ChatColor.GOLD
				+ ConfigLoader.getDefault("Tag.Strings.ArenaAdded"));
	}
	
	private static void handleJoin(Player player, TagArena arena) {
		if(arena.addPlayer(player)) {
			if(player.getGameMode() != GameMode.SURVIVAL) {
				player.sendMessage(ChatColor.GOLD
						+ ConfigLoader.getDefault("Tag.Strings.PlayerChangeGameMode"));
				player.setGameMode(GameMode.SURVIVAL);
			}
			
			MethodBypass.legalWarp(ArenaConfig.getDefault(
					"Arena." + arena.getArena() + ".Warps.Lobby"),
					player,
					GameCenter.getArena(player));
			player.sendMessage(ChatColor.GOLD + ConfigLoader.getDefault("Tag.Strings.PlayerWarpLobby"));
		} else
			player.sendMessage(ChatColor.GOLD 
					+ ConfigLoader.getDefault("Tag.Strings.GameIsFull"));
	}
	
	private static void notEnoughPermission(CommandSender sender) {
		sender.sendMessage(ChatColor.GOLD 
				+ ConfigLoader.getDefault("Tag.Strings.NoPerm"));
	} 
	
	private static void displayMenu(CommandSender sender, boolean specialPerms) {
		ChatColor aqua = ChatColor.AQUA;
		ChatColor dAqua = ChatColor.DARK_AQUA;
		ChatColor red = ChatColor.RED;
		ChatColor dRed = ChatColor.DARK_RED;
		
		String baseMenu = String.format(ChatColor.AQUA + 
				"%s%s%s%s%s%s%s%s%s%s%s%s%s",
				"===============================\n",
				"  Tag-Minigame Menu: \n",
				"===============================\n",
				aqua + " - /tag join [arena name]\n",
				dAqua + "  > Join the Tag lobby.\n",
				aqua + " - /tag leave\n",
				dAqua + "  > Leave a running game of Tag.\n",
				aqua + " - /tag help\n",
				dAqua + "  > Tutorial on how to play Tag.\n",
				aqua + " - /tag status [arena name]\n",
				dAqua + "  > Display the status of the specified Tag arena.\n",
				aqua + " - /tag leaderboard [amount]\n",
				dAqua + "  > Display a leaderboard showing everyone's wins and losses.\n");

		if(specialPerms) {
			sender.sendMessage(baseMenu
					+ String.format("%s%s%s%s%s%s%s%s%s%s",
							red + "\n \n  Advanced Commands: \n",
							red + " - /tag start [arena name]\n",
							dRed + "  > Force starts a game of Tag in the specified arena.\n",
							red + " - /tag stop [arena name]\n",
							dRed + "  > Force stops a game of Tag in the specified arena.\n"
									+ "    If no arena is specified, all games are stopped.\n",
							red + " - /tag createarena [arena name]\n",
							dRed + "  > Creates a Tag arena with the specified name.\n",
							red + " - /tag set [arena | rip | lobby] [arena name]\n",
							dRed + "  > Set the necessary spawn points for each arena. \n"
									+ "    These are needed for an arena to be usable! \n",
							aqua + "===============================\n"
									));
		} else 
			sender.sendMessage(baseMenu
					+ aqua + "===============================\n");
		
	}
	
	private static void displayHelpMenu(CommandSender sender) {
		sender.sendMessage(ChatColor.AQUA 
				+ String.format(
				"%s%s%s%s%s%s%s%s",
				"===============================\n",
				"  Tag-Minigame Help: \n",
				"===============================\n",
				"  Tag is a combination of Hide-and-Seek as well as Tag.\n",
				"  In this game you are teleported to an arena along with other players in which you \n",
				"  have to try not to get tagged by others while trying to hit others with your stick.\n",
				"  The last player standing wins the game of Tag and receives currency.\n",
				"===============================\n"
				));
	}
	
	private static void displayLeaderboard(CommandSender sender, String top) {
		int defaultSize = DEFAULT_TOP;
		
		if(!GeneralMethods.isInteger(top))
			sender.sendMessage(ChatColor.GOLD
					+ "Invalid number! Using default: " + DEFAULT_TOP);
		else 
			defaultSize = Integer.parseInt(top);

		if(!LeaderboardManager.getLeaderboardTop(defaultSize).isPresent()) {
			Bukkit.getServer().getLogger().warning("[Tag] leaderboard.yml was not detected!");
			return;
		}
		
		List<List<String[]>> leaderboard = LeaderboardManager.getLeaderboardTop(defaultSize).get();
		StringBuilder sBuilder = new StringBuilder();
		int count = 1;
		
		sBuilder.append(ChatColor.AQUA + "===============================\n");
		sBuilder.append(ChatColor.AQUA + "  Tag-Minigame Leaderboard: \n");
		sBuilder.append(ChatColor.AQUA + "===============================\n");
		for(List<String[]> stats : leaderboard) {
			if(count > defaultSize)
				break;
			
			for(String[] currentStat : stats) {
				String player = currentStat[0];
				int[] wins = LeaderboardManager.stringToIntArray(currentStat[1].split("_"));
				
				sBuilder.append(String.format(ChatColor.AQUA + "  [%d]: %s - %s %s\n", 
						count,
						player, 
						ChatColor.GREEN + "Wins: " + wins[0],
						ChatColor.RED + "Losses: " + wins[1]));
				count++;
			}
		}
		sBuilder.append(ChatColor.AQUA + "===============================\n");
		
		sender.sendMessage(sBuilder.toString());
	}
}
