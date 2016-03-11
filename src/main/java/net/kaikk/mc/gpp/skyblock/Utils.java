package net.kaikk.mc.gpp.skyblock;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.biome.BiomeReplace;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.biome.Biomes;
import com.sk89q.worldedit.world.registry.LegacyWorldData;
import com.sk89q.worldedit.world.registry.WorldData;

public class Utils {
	public static UUID toUUID(byte[] bytes) {
	    if (bytes.length != 16) {
	        throw new IllegalArgumentException();
	    }
	    int i = 0;
	    long msl = 0;
	    for (; i < 8; i++) {
	        msl = (msl << 8) | (bytes[i] & 0xFF);
	    }
	    long lsl = 0;
	    for (; i < 16; i++) {
	        lsl = (lsl << 8) | (bytes[i] & 0xFF);
	    }
	    return new UUID(msl, lsl);
	}
	
	public static String UUIDtoHexString(UUID uuid) {
		if (uuid==null) return "0x0";
		return "0x"+org.apache.commons.lang.StringUtils.leftPad(Long.toHexString(uuid.getMostSignificantBits()), 16, "0")+org.apache.commons.lang.StringUtils.leftPad(Long.toHexString(uuid.getLeastSignificantBits()), 16, "0");
	}
	
	public static boolean isFakePlayer(Player player) {
		for (Player p : Bukkit.getOnlinePlayers()) {
			if(player==p) {
				return false;
			}
		}
		return true;
	}
	
	public static String sanitizeSql(String string) {
		return string.replace("\\", "\\\\").replace("\"", "\\\"");
	}
	
	public static void loadSchematic(File schematic, Location location) {
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
			BukkitWorld bukkitWorld = new BukkitWorld(location.getWorld());
			EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession((com.sk89q.worldedit.world.World) bukkitWorld, 1000000);
			
			Vector vector = BukkitUtil.toVector(location);
			Operation operation = clipboardHolder.createPaste(editSession, LegacyWorldData.getInstance()).to(vector).ignoreAirBlocks(true).build();
			Operations.completeLegacy(operation);
		} catch (MaxChangedBlocksException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean regen(Location center, int blockRadius) {
		BukkitWorld bukkitWorld = new BukkitWorld(center.getWorld());
		EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession((com.sk89q.worldedit.world.World) bukkitWorld, 1000000);
		Region region = new CuboidRegion(new Vector(center.getBlockX()-blockRadius,0,center.getBlockZ()-blockRadius), new Vector(center.getBlockX()+blockRadius,255,center.getBlockZ()+blockRadius));
		
		
		return bukkitWorld.regenerate(region, editSession);
	}
	
	public static void setBiome(Location location, int radius, String biomeName) {
		BukkitWorld bukkitWorld = new BukkitWorld(location.getWorld());
		EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession((com.sk89q.worldedit.world.World) bukkitWorld, 1000000);
		bukkitWorld.getWorldData().getBiomeRegistry().getBiomes();
		BaseBiome biome = Biomes.findBiomeByName(bukkitWorld.getWorldData().getBiomeRegistry().getBiomes(), biomeName, bukkitWorld.getWorldData().getBiomeRegistry());
		if (biome == null) {
			throw new IllegalStateException("Biome not found");
		}
		
		BiomeReplace biomeReplace = new BiomeReplace(editSession, biome);
		try {
			for (int x = location.getBlockX()-radius; x<=location.getBlockX()+radius; x++) {
				for (int z = location.getBlockZ()-radius; z<=location.getBlockZ()+radius; z++) {
					biomeReplace.apply(new Vector2D(x, z));
				}
			}
		} catch (WorldEditException e) {
			e.printStackTrace();
		}
	}
	
	public static Biome matchAllowedBiome(String biomeName) {
		biomeName = biomeName.toLowerCase();
		for (Biome biome : GPPSkyBlock.getInstance().config().allowedBiomes) {
			if (biome.toString().replace("_", "").toLowerCase().equals(biomeName)) {
				return biome;
			}
		}
		return null;
	}

	public static String fromSnakeToCamelCase(String string) {
		StringBuilder sb = new StringBuilder();
		
		for (String s : string.split("_")) {
			sb.append((""+s.charAt(0)).toUpperCase());
			sb.append(s.substring(1).toLowerCase());
		}
		
		return sb.toString();
	}
}
