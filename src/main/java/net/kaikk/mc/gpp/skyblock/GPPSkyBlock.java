package net.kaikk.mc.gpp.skyblock;

import org.bukkit.plugin.java.JavaPlugin;

public class GPPSkyBlock extends JavaPlugin {
	private static GPPSkyBlock instance;
	private Config config;
	private DataStore dataStore;
	
	// TODO LIST
	// obsidian to lava bucket
	// clickable sign that teleports the player to his island
	// custom messages
	
	@Override
	public void onEnable() {
		instance=this;

		config = new Config(instance);
		
		try {
			dataStore = new DataStore(this);
			this.getServer().getPluginManager().registerEvents(new EventListener(this), this);
			this.getCommand(this.getName()).setExecutor(new CommandExec(this));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static GPPSkyBlock getInstance() {
		return instance;
	}

	public Config config() {
		return config;
	}

	public DataStore dataStore() {
		return dataStore;
	}
}
