/* Copyright 2013 Kevin Seiden. All rights reserved.

 This works is licensed under the Creative Commons Attribution-NonCommercial 3.0

 You are Free to:
    to Share: to copy, distribute and transmit the work
    to Remix: to adapt the work

 Under the following conditions:
    Attribution: You must attribute the work in the manner specified by the author (but not in any way that suggests that they endorse you or your use of the work).
    Non-commercial: You may not use this work for commercial purposes.

 With the understanding that:
    Waiver: Any of the above conditions can be waived if you get permission from the copyright holder.
    Public Domain: Where the work or any of its elements is in the public domain under applicable law, that status is in no way affected by the license.
    Other Rights: In no way are any of the following rights affected by the license:
        Your fair dealing or fair use rights, or other applicable copyright exceptions and limitations;
        The author's moral rights;
        Rights other persons may have either in the work itself or in how the work is used, such as publicity or privacy rights.

 Notice: For any reuse or distribution, you must make clear to others the license terms of this work. The best way to do this is with a link to this web page.
 http://creativecommons.org/licenses/by-nc/3.0/
 */
package io.github.alshain01.flagsplayer;

import io.github.alshain01.flags.Flag;
import io.github.alshain01.flags.Flags;
import io.github.alshain01.flags.System;
import io.github.alshain01.flags.ModuleYML;
import io.github.alshain01.flags.area.Area;

import org.bukkit.Bukkit;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Flags Damage - Module that adds damage flags to the plug-in Flags.
 */
public class FlagsPlayer extends JavaPlugin {
	/**
	 * Called when this module is enabled
	 */
	@Override
	public void onEnable() {
		final PluginManager pm = Bukkit.getServer().getPluginManager();

		if (!pm.isPluginEnabled("Flags")) {
			getLogger().severe("Flags was not found. Shutting down.");
			pm.disablePlugin(this);
		}

		// Connect to the data file and register the flags
        Set<Flag> flags = Flags.getRegistrar().register(new ModuleYML(this, "flags.yml"), "Player");
        Map<String, Flag> flagMap = new HashMap<String, Flag>();
        for(Flag f : flags) {
            flagMap.put(f.getName(), f);
        }

		// Load plug-in events and data
		Bukkit.getServer().getPluginManager().registerEvents(new PlayerListener(flagMap), this);
	}
	
	/*
	 * Handler for Eating Kept in isolated class due to version support.
	 */
	private class PlayerConsumeListener implements Listener {
        Flag flag;
        final System system = System.getActive();

        private PlayerConsumeListener (Flag flag) {
            this.flag = flag;
        }

		@EventHandler(ignoreCancelled = true)
		private void onPlayerItemConsume(PlayerItemConsumeEvent e) {
			final Player player = e.getPlayer();
			final Area area = system.getAreaAt(e.getPlayer().getLocation());

			if (player.hasPermission(flag.getBypassPermission()) || area.hasTrust(flag, player)) {
				return;
			}

			if (!area.getValue(flag, false)) {
				player.sendMessage(area.getMessage(flag, player.getName()));
				e.setCancelled(true);
			}
		}
	}

	/*
	 * The event handlers for the flags we created earlier
	 */
	private class PlayerListener implements Listener {
        Map<String, Flag> flags;
        final System system = System.getActive();

        private PlayerListener(Map<String, Flag> flags) {
            this.flags = flags;
        }

		private boolean isDenied(Player player, Flag flag, Area area) {
			if (player.hasPermission(flag.getBypassPermission())
					|| area.hasTrust(flag, player)) {
				return false;
			}

			if (!area.getValue(flag, false)) {
				sendMessage(player, flag, area);
				return true;
			}
			return false;
		}

        /*
         * Handler for Eat
         */
        @EventHandler(ignoreCancelled = true)
        private void onPlayerItemConsume(PlayerItemConsumeEvent e) {
            Flag flag = flags.get("Eat");

            if(flag != null) {
                e.setCancelled(isDenied(e.getPlayer(), flag, system.getAreaAt(e.getPlayer().getLocation())));
            }
        }

		/*
		 * Handler for Commands
		 */
		@EventHandler(ignoreCancelled = true)
		private void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
            Flag flag = flags.get("Commands");
            if(flag != null) {
			    e.setCancelled(isDenied(e.getPlayer(), flag, system.getAreaAt(e.getPlayer().getLocation())));
            }
		}

		/*
		 * Handler for Dropping Items
		 */
		@EventHandler(ignoreCancelled = true)
		private void onPlayerDropItem(PlayerDropItemEvent e) {
            Flag flag = flags.get("ItemDrop");
            if(flag != null) {
			    e.setCancelled(isDenied(e.getPlayer(), flag, system.getAreaAt(e.getPlayer().getLocation())));
            }
		}

		/*
		 * Handler for gaining Experience
		 */
		@EventHandler(priority = EventPriority.MONITOR)
		private void onPlayerExpChange(PlayerExpChangeEvent e) {
			final Player player = e.getPlayer();
			final Area area = system.getAreaAt(player.getLocation());
			final Flag flag = flags.get("Experience");

			if (player.hasPermission(flag.getBypassPermission()) || area.hasTrust(flag, player)) {
				return;
			}

			if (flag != null && !area.getValue(flag, false) && e.getAmount() > 0) {
			    e.setAmount(0);
			}
		}

		/*
		 * Handler for Fishing
		 */
		@EventHandler(ignoreCancelled = true)
		private void onPlayerFish(PlayerFishEvent e) {
            Flag flag = flags.get("Fishsing");
            if(flag != null) {
			    e.setCancelled(isDenied(e.getPlayer(), flag, system.getAreaAt(e.getPlayer().getLocation())));
            }
		}

		/*
		 * Handler for Right Clicking Objects
		 */
		@EventHandler(ignoreCancelled = true)
		private void onPlayerInteract(PlayerInteractEvent e) {
			if (e.getAction() != Action.RIGHT_CLICK_BLOCK) { return; }
            Flag flag = null;
            switch (e.getMaterial()) {
                case TRAP_DOOR:
                    flag = flags.get("TrapDoor");
                    break;
                case ANVIL:
                    flag = flags.get("Anvil");
                    break;
			}

            if(flag != null) {
                e.setCancelled(isDenied(e.getPlayer(), flag, system.getAreaAt(e.getPlayer().getLocation())));
            }
		}

		/*
		 * Handler for Right Clicking Entities, (Trading, Breeding)
		 */
		@EventHandler(ignoreCancelled = true)
		private void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
			final Player player = e.getPlayer();
			final Entity entity = e.getRightClicked();
			final Area area = system.getAreaAt(player.getLocation());
			Flag flag;

			if (entity instanceof Villager) {
				flag = flags.get("Trading");
                if(flag == null) { return; }

				// Villager trading
				if (player.hasPermission(flag.getBypassPermission()) || area.hasTrust(flag, player)) {
					return;
				}

				if (!area.getValue(flag, false)) {
					e.setCancelled(true);
					sendMessage(player, flag, area);
				}
			} else if (entity instanceof Animals) {
				// 1. This is not a "taming" flag, so let it be tamed.
				// 2. This is not a "feeding" flag, so let it be fed.
				if (entity instanceof Tameable && (!((Tameable) entity).isTamed()
							|| ((LivingEntity) entity).getHealth() != ((LivingEntity) entity).getMaxHealth())) {
					return;
				}

				flag = flags.get("Breeding");
                if(flag == null) { return; }

                if (player.hasPermission(flag.getBypassPermission()) || area.hasTrust(flag, player)) {
					return;
				}

				if (!area.getValue(flag, false)) {
					e.setCancelled(true);
					sendMessage(player, flag, area);
				}
			}
		}

		/*
		 * Handler for Leveling Up
		 */
		@EventHandler(priority = EventPriority.MONITOR)
		private void onPlayerLevelChange(PlayerLevelChangeEvent e) {
			final Player player = e.getPlayer();
			final Area area = system.getAreaAt(player.getLocation());
			final Flag flag = flags.get("Level");
            if(flag == null) { return; }

            if (player.hasPermission(flag.getBypassPermission()) || area.hasTrust(flag, player)) {
				return;
			}

			if (!area.getValue(flag, false)) {
				if (e.getNewLevel() > e.getOldLevel()) {
					// You can't actually stop this event
					// but you can make it ineffective by reducing a players
					// level before they gain it back
					player.setLevel(player.getLevel() - (e.getNewLevel() - e.getOldLevel()));
					player.setExp(0.9999f);
				}
			}
		}

		/*
		 * Handler for picking up items
		 */
		@EventHandler(ignoreCancelled = true)
		private void onPlayerPickupItem(PlayerPickupItemEvent e) {
			final Player player = e.getPlayer();
			final Area area = system.getAreaAt(player.getLocation());
			final Flag flag = flags.get("ItemPickup");
            if(flag == null) { return; }

            if (player.hasPermission(flag.getBypassPermission()) || area.hasTrust(flag, player)) {
				return;
			}

			if (!area.getValue(flag, false)) {
				e.setCancelled(true);
			}
		}

		/*
		 * Handler for entering a portal
		 */
		@EventHandler(ignoreCancelled = true)
		private void onPlayerPortal(PlayerPortalEvent e) {
            Flag flag = flags.get("Portal");
            if(flag != null) {
			    e.setCancelled(isDenied(e.getPlayer(), flag, system.getAreaAt(e.getPlayer().getLocation())));
            }
		}

		/*
		 * Handler for Teleportation
		 */
		@EventHandler(ignoreCancelled = true)
		private void onPlayerTeleport(PlayerTeleportEvent e) {
			// We don't want to cancel plugin teleporting
			// Because that would mess with flight, allow leave, and allow exit.
			// Portals are handled in another event.
			if (e.getCause() == TeleportCause.PLUGIN
					|| e.getCause() == TeleportCause.END_PORTAL
					|| e.getCause() == TeleportCause.NETHER_PORTAL) {
				return;
			}

			final Player player = e.getPlayer();
			final Area tpFrom = system.getAreaAt(e.getFrom());
			final Area tpTo = system.getAreaAt(e.getTo());

			// Teleport out of area
			Flag flag = flags.get("AllowTpOut");
			if (!player.hasPermission(flag.getBypassPermission()) && !tpFrom.hasTrust(flag, player)) {
				if (!tpFrom.getValue(flag, false)) {
					e.setCancelled(true);
					sendMessage(player, flag, tpFrom);
					return;
				}
			}

			// Teleport into area
			flag = flags.get("AllowTpIn");
			if (!player.hasPermission(flag.getBypassPermission()) && !tpTo.hasTrust(flag, player)) {
				if (!tpTo.getValue(flag, false)) {
					e.setCancelled(true);
					sendMessage(player, flag, tpTo);
				}
			}
		}

		private void sendMessage(Player player, Flag flag, Area area) {
			player.sendMessage(area.getMessage(flag, player.getName()));
		}
	}
}
