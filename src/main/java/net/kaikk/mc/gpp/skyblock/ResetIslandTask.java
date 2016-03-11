package net.kaikk.mc.gpp.skyblock;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.LegacyWorldData;
import com.sk89q.worldedit.world.registry.WorldData;

public class ResetIslandTask extends BukkitRunnable {
	private File schematic;
	private Island island;
	private String ownerName;
	private int x, z, lz, gx, gz;

	public ResetIslandTask(Island island, File schematic) {
		this.island = island;
		this.schematic = schematic;

		this.lz = island.getClaim().getLesserBoundaryCorner().getBlockZ()>>4;
		this.x = island.getClaim().getLesserBoundaryCorner().getBlockX()>>4;
		this.z = lz;
		this.gx = island.getClaim().getGreaterBoundaryCorner().getBlockX()>>4;
		this.gz = island.getClaim().getGreaterBoundaryCorner().getBlockZ()>>4;

		ownerName = island.getOwnerName();
		
		if (island.isOwnerOnline()) {
			island.getPlayer().sendMessage(ChatColor.GREEN+"Please wait while your island is generating!");
		}
		Bukkit.getLogger().info("Generating "+ownerName+" island at "+island.getClaim().locationToString());
	}

	@Override
	public void run() {
		for (int i = 0; i<8; i++) {
			if (x <= gx) {
				if (z <= gz) {
					try {
						island.getClaim().getWorld().loadChunk(x, z, false);
						island.getClaim().getWorld().regenerateChunk(x, z);
					} catch (Exception e) {
						if (island.isOwnerOnline()) {
							island.getPlayer().sendMessage(ChatColor.RED+"An error occurred while generating a new island: regen error.");
						}
						Bukkit.getLogger().info("An error occurred while generating "+ownerName+" island");
						e.printStackTrace();
						this.cancel();
						return;
					}
					z++;
				} else {
					this.z = lz;
					x++;
				}
			} else {
				this.cancel();
				Bukkit.getLogger().info(ownerName+" island regeneration complete.");
				try {
					// read schematic file
					FileInputStream fis = new FileInputStream(schematic);
					BufferedInputStream bis = new BufferedInputStream(fis);
					ClipboardReader reader = ClipboardFormat.SCHEMATIC.getReader(bis);

					// create clipboard
					WorldData worldData = LegacyWorldData.getInstance();
					Clipboard clipboard = reader.read(worldData);
					fis.close();

					ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard, worldData);
					EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession((World) new BukkitWorld(island.getClaim().getWorld()), 1000000);

					try {
						island.setSpawn(island.getCenter());
					} catch (SQLException e) {
						e.printStackTrace();
					}

					island.getSpawn().getChunk().load();

					Vector vector = BukkitUtil.toVector(island.getSpawn());
					Operation operation = clipboardHolder.createPaste(editSession, LegacyWorldData.getInstance()).to(vector).ignoreAirBlocks(true).build();
					Operations.completeLegacy(operation);
					
					Bukkit.getLogger().info(ownerName+" island schematic load complete.");

					if (GPPSkyBlock.getInstance().config().defaultBiome != null) {
						island.setIslandBiome(GPPSkyBlock.getInstance().config().defaultBiome);
						Bukkit.getLogger().info(ownerName+" island biome set to default biome ("+GPPSkyBlock.getInstance().config().defaultBiome.toString()+")");
					}

					island.ready = true;
					if (island.isOwnerOnline()) {
						island.getPlayer().sendMessage(ChatColor.GREEN+"Island generation complete.");
						island.getPlayer().sendMessage(ChatColor.RED+"WARNING: Be sure to use a full block for your island spawn. Do not use slabs!");
						island.getPlayer().teleport(island.getSpawn());
					}

					Bukkit.getLogger().info(ownerName+" island reset completed.");
				} catch (MaxChangedBlocksException | IOException e) {
					if (island.isOwnerOnline()) {
						island.getPlayer().sendMessage(ChatColor.RED+"An error occurred while generating a new island: schematic load error.");
					}
					e.printStackTrace();
				}
				return;
			}
		}
	}
}
