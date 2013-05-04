package net.aemservers.pvptag;

import java.util.Calendar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;

public class PvPTagListener implements Listener {
	
	PvPTag plugin;
	
	public PvPTagListener(PvPTag plugin) {
		this.plugin = plugin;
	}
	public void cm(Player player, String string) {
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', string));
	}
	private void listmessage(Player player, String path) {
		for(String message : plugin.getConfig().getStringList(path)) {
			cm(player, message);
		}
	}
	private void deleteFromMemory(String playername) {
		if(plugin.playertime.containsKey(playername)) {
			plugin.playertime.remove(playername);
		}
		if(plugin.flyingplayers.contains(playername)) {
			plugin.flyingplayers.remove(playername);
		}
	}
	private void tagPlayer(Player player) {
		Integer multiple = plugin.pluginsettings.get("timevalue");
		Calendar c = Calendar.getInstance();
		if(player.isFlying()) {
			plugin.flyingplayers.add(player.getName());
		}
		plugin.playertime.put(player.getName(), c.getTimeInMillis()+multiple);
		listmessage(player, "messages.entered-combat");
		player.setAllowFlight(false);
		player.removePotionEffect(PotionEffectType.INVISIBILITY);
		timeCheckDamager(player.getName(), player, player.getTicksLived());
	}
	private void tagAddTime(Player player) {
		Integer multiple = plugin.pluginsettings.get("timevalue");
		Calendar c = Calendar.getInstance();
		player.removePotionEffect(PotionEffectType.INVISIBILITY);
		plugin.playertime.put(player.getName(), c.getTimeInMillis()+multiple);
	}
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPlayercombat(EntityDamageByEntityEvent event) {
		if(!event.isCancelled()) {
			if(event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
				Player damager = (Player) event.getDamager();
				Player defender = (Player) event.getEntity();
				if(damager != null && !plugin.playertime.containsKey(damager.getName())) {
					tagPlayer(damager);
				}
				if(defender != null && !plugin.playertime.containsKey(defender.getName())) {
					tagPlayer(defender);
				}
			}
			else if(event.getDamager() instanceof Projectile) {
				LivingEntity damager = ((Projectile)event.getDamager()).getShooter();
				if(damager instanceof Player && event.getEntity() instanceof Player) {
					Player defender = (Player) event.getEntity();
					Player damagee = (Player) damager;
					if(damager != null && !plugin.playertime.containsKey(damagee.getName()))
					tagPlayer(damagee);
					if(defender != null && !plugin.playertime.containsKey(defender.getName())) {
						tagPlayer(defender);
					}
				}
			}
		}
	}
	@EventHandler
	public void onCombatTime(EntityDamageByEntityEvent event) {
		if(event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
			Player damager = (Player) event.getDamager();
			Player defender = (Player) event.getEntity();
			if(plugin.playertime.containsKey(damager.getName())) {
				tagAddTime(damager);
			}
			if(plugin.playertime.containsKey(defender.getName())) {
				tagAddTime(defender);
			}
		}
		else if(event.getDamager() instanceof Projectile) {
			LivingEntity damager = ((Projectile)event.getDamager()).getShooter();
			if(damager instanceof Player && event.getEntity() instanceof Player) {
				Player defender = (Player) event.getEntity();
				Player damagee = (Player) damager;
				if(plugin.playertime.containsKey(damagee.getName())) {
					tagAddTime(damagee);
				}
				if(plugin.playertime.containsKey(defender.getName())) {
					tagAddTime(defender);
				}
			}
		}
	}
	@EventHandler
	public void onFlyingFall(EntityDamageEvent event) {
		if(event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			if(plugin.flyingplayers.contains(player.getName())) {
				if(event.getCause() == DamageCause.FALL) {
					event.setCancelled(true);
					plugin.flyingplayers.remove(player.getName());
				}
			}
		}
	}
	@EventHandler
	public void onEnderPearlUse(PlayerInteractEvent event) {
		if(plugin.playertime.containsKey(event.getPlayer().getName())) {
			if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if(event.getMaterial() == Material.ENDER_PEARL) {
					if(!event.getPlayer().hasPermission("pvptag.enderpearl.bypass")) {
						event.setCancelled(true);
						listmessage(event.getPlayer(), "messages.enderpearl-combat");
					}
				}
			}
		}
	}
	@EventHandler
	public void onCombatDeath(PlayerDeathEvent event) {
		String name = event.getEntity().getName();
		if(plugin.playertime.containsKey(name)) {
			deleteFromMemory(name);
		}
	}
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
		if(plugin.playertime.containsKey(event.getPlayer().getName())) {
			event.setCancelled(true);
			listmessage(event.getPlayer(), "messages.command-combat");
		}
	}
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		String name = event.getPlayer().getName();
		if(plugin.playertime.containsKey(name)) {
			plugin.players.add(name);
			plugin.playertime.remove(name);
		}
	}
	@EventHandler
	public void onPlayerLogin(final PlayerJoinEvent event) {
		final String name = event.getPlayer().getName();
		if(plugin.players.contains(name)) {
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
				@Override
				public void run() {
					if(event.getPlayer() != null) {
						event.getPlayer().setHealth(0);
						plugin.players.remove(name);
					}
				}
			}, 40L);
		}
		if(plugin.playertime.containsKey(name)) {
			plugin.playertime.remove(name);
		}
	}
	public void timeCheckDamager(final String damagername, final Player damagerplayer, Integer intname) {
		final Integer integername = intname;
		intname = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, new Runnable() {
			@Override
			public void run() {
				if(plugin.playertime.containsKey(damagername)) {
					Calendar c = Calendar.getInstance();
					if(c.getTimeInMillis() >= plugin.playertime.get(damagername)) {
						listmessage(damagerplayer, "messages.depart-combat");
						deleteFromMemory(damagername);
						Bukkit.getScheduler().cancelTask(integername);
					}
				} else {
					deleteFromMemory(damagername);
					Bukkit.getScheduler().cancelTask(integername);
				}
			}
		}, 0L, 40L);
	}
	public void timeCheckDefender(final String defendername, final Player defenderplayer, Integer intname) {
		final Integer integername = intname;
		intname = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, new Runnable() {
			@Override
			public void run() {
				if(plugin.playertime.containsKey(defendername)) {
					Calendar c = Calendar.getInstance();
					if(c.getTimeInMillis() >= plugin.playertime.get(defendername)) {
						listmessage(defenderplayer, "messages.depart-combat");
						deleteFromMemory(defendername);
						Bukkit.getScheduler().cancelTask(integername);
					}
				} else {
					deleteFromMemory(defendername);
					Bukkit.getScheduler().cancelTask(integername);
				}
			}
		}, 0L, 40L);
	}
}
