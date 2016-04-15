package net.kaikk.mc.gpp.skyblock;

import java.io.File;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import net.kaikk.mc.gpp.Claim;
import net.kaikk.mc.gpp.GriefPreventionPlus;
import net.kaikk.mc.gpp.PlayerData;

public class Island {
	private UUID ownerId;
	private Claim claim;
	private Location spawn;
	boolean ready = true;
	
	public Island(UUID ownerId, Claim claim) {
		this.ownerId = ownerId;
		this.claim = claim;
		this.spawn = this.getCenter().add(0.5, 1, 0.5);
	}
	
	public Island(UUID ownerId, Claim claim, Location spawn) {
		this.ownerId = ownerId;
		this.claim = claim;
		this.spawn = spawn;
	}
	
	public Claim getClaim() {
		return claim;
	}
	
	public Location getSpawn() {
		return spawn;
	}
	
	public UUID getOwnerId() {
		return ownerId;
	}
	
	public Player getPlayer() {
		return Bukkit.getPlayer(ownerId);
	}
	
	public String getOwnerName() {
		return Bukkit.getOfflinePlayer(ownerId).getName();
	}
	
	public boolean isOwnerOnline() {
		return Bukkit.getPlayer(ownerId) != null;
	}
	
	public void reset() {
		File schematicFile = new File(GPPSkyBlock.getInstance().getDataFolder(), GPPSkyBlock.getInstance().config().schematic+".schematic");
		if (!schematicFile.exists()) {
			throw new IllegalStateException("Schematic file \""+GPPSkyBlock.getInstance().config().schematic+".schematic\" doesn't exist");
		}
		
		this.teleportEveryoneToSpawn();
		
		this.ready = false;
		new ResetIslandTask(this, schematicFile).runTaskTimer(GPPSkyBlock.getInstance(), 1L, 1L);
	}
	
	public int getRadius() {
		int lx = claim.getLesserBoundaryCorner().getBlockX();
		int gx = claim.getGreaterBoundaryCorner().getBlockX();
		
		return (gx-lx)/2;
	}
	
	public void setRadius(int radius) {
		if (radius>254 || radius<1) {
			throw new IllegalArgumentException("Invalid radius (max 254)");
		}
		Location center = this.getCenter();
		int size = this.claim.getArea();		
		GriefPreventionPlus.getInstance().getDataStore().resizeClaim(this.claim, center.getBlockX()-radius, center.getBlockZ()-radius, center.getBlockX()+radius, center.getBlockZ()+radius, null);
		PlayerData playerData = GriefPreventionPlus.getInstance().getDataStore().getPlayerData(ownerId);
		playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks()+(this.claim.getArea()-size));
		GriefPreventionPlus.getInstance().getDataStore().savePlayerData(ownerId, playerData);
	}
	
	public Location getCenter() {
		int radius = this.getRadius();
		return new Location(claim.getWorld(), claim.getLesserBoundaryCorner().getBlockX()+radius, GPPSkyBlock.getInstance().config().yLevel, claim.getLesserBoundaryCorner().getBlockZ()+radius);
	}
	
	public void teleportEveryoneToSpawn() {
		Location spawnLocation = Bukkit.getWorld(GPPSkyBlock.getInstance().config().worldName).getSpawnLocation();
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (this.getClaim().contains(player.getLocation(), true, false)) {
				player.teleport(spawnLocation);
			}
		}
	}
	
	public void setSpawn(Location location) throws SQLException {
		this.spawn = location;
		GPPSkyBlock.getInstance().dataStore().updateIsland(this);
	}
	
	public void setIslandBiome(Biome biome) {
		int x = this.getClaim().getLesserBoundaryCorner().getBlockX();
		int sz = this.getClaim().getLesserBoundaryCorner().getBlockZ();
		int ux = this.getClaim().getGreaterBoundaryCorner().getBlockX();
		int uz = this.getClaim().getGreaterBoundaryCorner().getBlockZ();
		
		for (; x <= ux; x++) {
			for (int z = sz; z <= uz; z++) {
				this.getClaim().getWorld().setBiome(x, z, biome);
			}
		}
	}
	
	public void setChunkBiome(Biome biome, int chunkX, int chunkZ) {
		int x = chunkX<<4;
		int sz = chunkZ<<4;
		int ux = x+16;
		int uz = sz+16;
		
		for (; x < ux; x++) {
			for (int z = sz; z < uz; z++) {
				this.getClaim().getWorld().setBiome(x, z, biome);
			}
		}
	}
	
	public void setBlockBiome(Biome biome, int blockX, int blockZ) {
		this.getClaim().getWorld().setBiome(blockX, blockZ, biome);
	}
}
