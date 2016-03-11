package net.kaikk.mc.gpp.skyblock;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpawnTeleportTask extends BukkitRunnable {
	private int countdown;
	private Player player;
	private Island island;
	private Location location;
	
	private SpawnTeleportTask(Player player, Island island, int countdown) {
		this.player = player;
		this.island = island;
		this.location = player.getLocation();
		this.countdown = countdown;
	}

	@Override
	public void run() {
		island.getSpawn().getChunk().load();
		if (countdown>0) {
			try {
				if (this.location.distanceSquared(location)>0) {
					player.sendMessage(ChatColor.RED+"Teleport cancelled");
					this.cancel();
					return;
				}
			} catch (IllegalStateException e) {
				player.sendMessage(ChatColor.RED+"Teleport cancelled");
				this.cancel();
				return;
			}
		} else {
			player.teleport(island.getSpawn());
			if (countdown<-4) {
				this.cancel();
				return;
			}
			
		}
		
		countdown--;
	}
	
	public static void TeleportTask(Player player, Island island, int countdown) {
		new SpawnTeleportTask(player, island, countdown*4).runTaskTimer(GPPSkyBlock.getInstance(), 0L, 5L);
	}
}
