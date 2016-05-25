package net.kaikk.mc.gpp.skyblock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import net.kaikk.mc.gpp.Claim;
import net.kaikk.mc.gpp.GriefPreventionPlus;
import net.kaikk.mc.gpp.events.ClaimDeleteEvent;
import net.kaikk.mc.gpp.events.ClaimExitEvent;
import net.kaikk.mc.gpp.events.ClaimResizeEvent;

class EventListener implements Listener {
	private GPPSkyBlock instance;
	
	EventListener(GPPSkyBlock instance) {
		this.instance = instance;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void onPlayerLogin(PlayerLoginEvent event) {
		if (instance.config().autoSpawn && !event.getPlayer().hasPlayedBefore()) {
			Island island = instance.dataStore().getIsland(event.getPlayer().getUniqueId());
			if (island==null) {
				try {
					island = instance.dataStore().createIsland(event.getPlayer().getUniqueId());
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
			
			event.getPlayer().teleport(island.getSpawn());
		}
	}
	
	@EventHandler	
	void onClaimExit(ClaimExitEvent event) {
		if (event.getPlayer().hasPermission("gppskyblock.override") || event.getPlayer().hasPermission("gppskylobck.leaveisland")) {
			return;
		}
		
		if (isIslandWorld(event.getFrom().getWorld()) && isIslandWorld(event.getTo().getWorld())) {
			event.getPlayer().sendMessage(ChatColor.RED+"You can't walk/fly out of your island!");
			Island island = getIsland(event.getClaim());
			if (island!=null) {
				event.getPlayer().teleport(island.getSpawn());
			} else {
				event.setCancelled(true);
			}
			return;
		}
	}
	
	@EventHandler
	void onClaimDelete(ClaimDeleteEvent event) {
		if (isIsland(event.getClaim())) {
			event.setCancelled(true);
			if (event.getPlayer()!=null) {
				event.getPlayer().sendMessage(ChatColor.RED+"You can't delete this claim. It's an island!");
			}
		}
	}
	
	@EventHandler
	void onClaimResize(ClaimResizeEvent event) {
		if (event.getPlayer()!=null && isIsland(event.getClaim())) {
			event.setCancelled(true);
			if (event.getPlayer()!=null) {
				event.getPlayer().sendMessage(ChatColor.RED+"You can't resize this claim. It's an island!");
			}
		}
	}
	
	@EventHandler
	void onPlayerTeleport(PlayerTeleportEvent event) {
		if (!event.getPlayer().hasPermission("gppskyblock.override") && isIslandWorld(event.getTo().getWorld()) && !isIslandWorld(event.getFrom().getWorld()) && !event.getTo().equals(Bukkit.getWorld(instance.config().worldName).getSpawnLocation())) {
			Claim claim = GriefPreventionPlus.getInstance().getDataStore().getClaimAt(event.getTo());
			if (claim==null) {
				event.getPlayer().teleport(Bukkit.getWorld(instance.config().worldName).getSpawnLocation()); // TODO
			}
		}
	}
	
	@EventHandler
	void onPlayerTeleport(PlayerPortalEvent event) {
		if (event.getCause()==TeleportCause.END_PORTAL && isIslandWorld(event.getFrom().getWorld())) {
			Location loc = event.getPortalTravelAgent().findPortal(new Location(event.getTo().getWorld(), 0, 64, 0));
			if (loc!=null) {
				event.setTo(loc);
				event.useTravelAgent(false);
			}
		}
	}
	
	@EventHandler
	void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction()==Action.RIGHT_CLICK_BLOCK && event.getPlayer().getItemInHand().getType()==Material.BUCKET && event.getClickedBlock().getType()==Material.OBSIDIAN && event.getPlayer().hasPermission("gppskyblock.lava")) {
			event.getClickedBlock().setType(Material.AIR);
			event.getPlayer().getItemInHand().setType(Material.LAVA_BUCKET);
		}
	}
	
	boolean isIsland(Claim claim) {
		Island island = getIsland(claim);
		if (island == null) {
			return false;
		}
		
		return island.getClaim() == claim;
	}
	
	Island getIsland(Claim claim) {
		return instance.dataStore().getIsland(claim.getOwnerID());
	}
	
	boolean isIslandWorld(World world) {
		return world.getName().equals(instance.config().worldName);
	}
}
