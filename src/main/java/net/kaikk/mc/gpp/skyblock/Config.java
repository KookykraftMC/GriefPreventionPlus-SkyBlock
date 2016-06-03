package net.kaikk.mc.gpp.skyblock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Config {
	public String dbHostname, dbUsername, dbPassword, dbDatabase, worldName, schematic;
	public int nextRegionX = 1, nextRegionZ = 1, radius, yLevel, tpCountdown;
	public boolean autoSpawn;
	public Biome defaultBiome;
	public List<Biome> allowedBiomes = new ArrayList<Biome>();
	JavaPlugin instance;
	
	Config(JavaPlugin instance) {
		this.instance = instance;
		instance.saveDefaultConfig();
		instance.reloadConfig();
		
		this.worldName=instance.getConfig().getString("WorldName");
		this.schematic=instance.getConfig().getString("Schematic");
		saveResource("default.schematic");
		File schematicFile = new File(instance.getDataFolder(), schematic+".schematic");
		if (!schematicFile.exists()) {
			instance.getLogger().severe("Island schematic file \""+schematic+".schematic\" doesn't exist!");
		}
		
		this.radius=instance.getConfig().getInt("Radius");
		if (this.radius>255) {
			this.radius=255;
		} else if (this.radius<10) {
			this.radius=10;
		}
		
		this.yLevel=instance.getConfig().getInt("YLevel");
		if (this.yLevel<1 || this.yLevel>255) {
			this.yLevel=64;
		}
		
		String biomeName = instance.getConfig().getString("DefaultBiome", "UNCHANGED");
		if (!biomeName.equals("UNCHANGED")) {
			defaultBiome = Biome.valueOf(biomeName);
			if (defaultBiome == null) {
				instance.getLogger().warning("Unknown default biome \""+biomeName+"\"");
			}
		}
		
		this.dbHostname=instance.getConfig().getString("MySQL.Hostname");
		this.dbUsername=instance.getConfig().getString("MySQL.Username");
		this.dbPassword=instance.getConfig().getString("MySQL.Password");
		this.dbDatabase=instance.getConfig().getString("MySQL.Database");
		
		this.autoSpawn=instance.getConfig().getBoolean("NewbiesAutoSpawn");
		
		allowedBiomes.clear();
		for (String biomeString : instance.getConfig().getStringList("AllowedBiomes")) {
			Biome biome = Biome.valueOf(biomeString);
			if (biome == null) {
				instance.getLogger().warning("Skipping unknown allowed biome \""+biomeString+"\"");
			} else {
				allowedBiomes.add(biome);
			}
		}
		
		this.tpCountdown=instance.getConfig().getInt("TPCountdown", 5);
		
		// Messages
		/*saveResource("messages.yml");
		FileConfiguration messages = YamlConfiguration.loadConfiguration(new File(instance.getDataFolder(), "messages.yml"));
		
		if (messages==null) {
			instance.getLogger().severe("There was an error while loading messages.yml!");
		} else {
			Messages.messages.clear();
			for (String key : messages.getKeys(false)) {
				Messages.messages.put(key, ChatColor.translateAlternateColorCodes('&', messages.getString(key)));
			}
		}*/
		
		// Data
		saveResource("data.yml");
		FileConfiguration data = YamlConfiguration.loadConfiguration(new File(instance.getDataFolder(), "data.yml"));
		if (data==null) {
			instance.getLogger().severe("There was an error while loading data.yml!");
		} else {
			this.nextRegionX=data.getInt("NextRegion.X", 1);
			this.nextRegionZ=data.getInt("NextRegion.Z", 1);
		}
	}
	
	void saveData() {
		File file = new File(instance.getDataFolder(), "data.yml");
		FileConfiguration data = YamlConfiguration.loadConfiguration(file);
		if (data==null) {
			instance.getLogger().severe("There was an error while saving data.yml!");
		} else {
			data.set("NextRegion.X", this.nextRegionX);
			data.set("NextRegion.Z", this.nextRegionZ);
			try {
				data.save(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	void saveResource(String name) {
		if (!new File(instance.getDataFolder(), name).exists()) {
			instance.saveResource(name, false);
		}
	}
}
