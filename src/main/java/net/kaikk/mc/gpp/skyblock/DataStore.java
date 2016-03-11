package net.kaikk.mc.gpp.skyblock;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import net.kaikk.mc.gpp.Claim;
import net.kaikk.mc.gpp.ClaimResult;
import net.kaikk.mc.gpp.ClaimResult.Result;
import net.kaikk.mc.gpp.GriefPreventionPlus;
import net.kaikk.mc.gpp.PlayerData;

class DataStore {
	private GPPSkyBlock instance;
	private String dbUrl, username, password;
	private Connection db = null;
	private Map<UUID,Island> islands = new HashMap<UUID,Island>();
	
	ExecutorService executor = Executors.newSingleThreadExecutor();

	DataStore(GPPSkyBlock instance) throws Exception {
		this.instance=instance;
		this.dbUrl = "jdbc:mysql://"+instance.config().dbHostname+"/"+instance.config().dbDatabase;
		this.username = instance.config().dbUsername;
		this.password = instance.config().dbPassword;
		
		try {
			//load the java driver for mySQL
			Class.forName("com.mysql.jdbc.Driver");
		} catch(Exception e) {
			this.instance.getLogger().severe("Unable to load Java's mySQL database driver.  Check to make sure you've installed it properly.");
			throw e;
		}
		
		try {
			this.dbCheck();
		} catch(Exception e) {
			this.instance.getLogger().severe("Unable to connect to database.  Check your config file settings. Details: \n"+e.getMessage());
			throw e;
		}
		
		Statement statement = db.createStatement();

		try {
			// Creates tables on the database
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS gppskyblock_islands (player binary(16) NOT NULL, claimid int(11) NOT NULL, sx int(11) NOT NULL, sy int(11) NOT NULL, sz int(11) NOT NULL, PRIMARY KEY (player));");
		} catch(Exception e) {
			this.instance.getLogger().severe("Unable to create the necessary database table. Details: \n"+e.getMessage());
			throw e;
		}
		
		ResultSet rs = this.statement().executeQuery("SELECT * FROM gppskyblock_islands");
		islands.clear();

		while (rs.next()) {
			UUID uuid = Utils.toUUID(rs.getBytes(1));
			Claim claim = GriefPreventionPlus.getInstance().getDataStore().getClaim(rs.getInt(2));
			if (claim!=null) {
				islands.put(uuid, new Island(uuid, claim, new Location(claim.getWorld(), rs.getInt(3)+0.5, rs.getInt(4), rs.getInt(5)+0.5)));
			}
		}
	}
	
	public Island createIsland(UUID uuid) throws Exception {
		int x, z;
		if (instance.config().nextRegionX>58500) {
			x = 1;
			z = instance.config().nextRegionZ+3;
		} else {
			x = instance.config().nextRegionX+3;
			z = instance.config().nextRegionZ;
		}
		
		int bx = x << 9;
		int bz = z << 9;
		
		World world = Bukkit.getWorld(instance.config().worldName);
		PlayerData playerData = GriefPreventionPlus.getInstance().getDataStore().getPlayerData(uuid);
		playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks()+(((instance.config().radius*2)+1)*2));
		ClaimResult result = GriefPreventionPlus.getInstance().getDataStore().newClaim(world.getUID(), bx+255-instance.config().radius, bz+255-instance.config().radius, bx+255+instance.config().radius, bz+255+instance.config().radius, uuid, null, null, null);
		if (result.getResult()!=Result.SUCCESS) {
			throw new Exception(result.getReason());
		}
		
		instance.config().nextRegionX = x;
		instance.config().nextRegionZ = z;
		instance.config().saveData();
		
		Island island = new Island(uuid, result.getClaim());
		try {
			instance.dataStore().addIsland(island);
		} catch (SQLException e) {
			e.printStackTrace();
			GriefPreventionPlus.getInstance().getDataStore().deleteClaim(result.getClaim());
			throw new Exception("data store issue.");
		}
		
		island.reset();
		
		return island;
	}

	void asyncUpdate(List<String> sql) {
		String[] arr = new String[(sql.size())];
		asyncUpdate(sql.toArray(arr));
	}

	void asyncUpdate(String... sql) {
		executor.execute(new DatabaseUpdate(sql));
	}
	
	Future<ResultSet> asyncQuery(String sql) {
		return executor.submit(new DatabaseQuery(sql));
	}
	
	Future<ResultSet> asyncUpdateGenKeys(String sql) {
		return executor.submit(new DatabaseUpdateGenKeys(sql));
	}
	
	synchronized void update(String sql) throws SQLException {
		this.update(this.statement(), sql);
	}
	
	synchronized void update(Statement statement, String sql) throws SQLException {
		statement.executeUpdate(sql);
	}
	
	synchronized void update(String... sql) throws SQLException {
		this.update(this.statement(), sql);
	}
	
	synchronized void update(Statement statement, String... sql) throws SQLException {
		for (String sqlRow : sql) {
			statement.executeUpdate(sqlRow);
		}
	}
	
	synchronized ResultSet query(String sql) throws SQLException {
		return this.query(this.statement(), sql);
	}
	
	synchronized ResultSet query(Statement statement, String sql) throws SQLException {
		return statement.executeQuery(sql);
	}
	
	synchronized ResultSet updateGenKeys(String sql) throws SQLException {
		return this.updateGenKeys(this.statement(), sql);
	}
	
	synchronized ResultSet updateGenKeys(Statement statement, String sql) throws SQLException {
		statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
		return statement.getGeneratedKeys();
	}
	
	synchronized Statement statement() throws SQLException {
		this.dbCheck();
		return this.db.createStatement();
	}
	
	synchronized void dbCheck() throws SQLException {
		if(this.db == null || this.db.isClosed()) {
			Properties connectionProps = new Properties();
			connectionProps.put("user", this.username);
			connectionProps.put("password", this.password);
			
			this.db = DriverManager.getConnection(this.dbUrl, connectionProps); 
		}
	}
	
	synchronized void dbClose()  {
		try {
			if (!this.db.isClosed()) {
				this.db.close();
				this.db=null;
			}
		} catch (SQLException e) {
			
		}
	}
	
	Island getIsland(UUID playerId) {
		return this.islands.get(playerId);
	}
	
	void addIsland(Island island) throws SQLException {
		this.statement().executeUpdate("INSERT INTO gppskyblock_islands VALUES("+Utils.UUIDtoHexString(island.getOwnerId())+", "+island.getClaim().getID()+", "+island.getSpawn().getBlockX()+", "+island.getSpawn().getBlockY()+", "+island.getSpawn().getBlockZ()+");");
		this.islands.put(island.getOwnerId(), island);
	}
	
	void removeIsland(Island island) throws SQLException {
		this.statement().executeUpdate("DELETE FROM gppskyblock_islands WHERE player = "+Utils.UUIDtoHexString(island.getOwnerId())+" LIMIT 1");
		this.islands.remove(island.getOwnerId());
	}
	
	void updateIsland(Island island) throws SQLException {
		this.statement().executeUpdate("UPDATE gppskyblock_islands SET sx = "+island.getSpawn().getBlockX()+", sy = "+island.getSpawn().getBlockY()+", sz = "+island.getSpawn().getBlockZ()+" WHERE player = "+Utils.UUIDtoHexString(island.getOwnerId())+" LIMIT 1");
	}
	
	private class DatabaseUpdate implements Runnable {
		private String[] sql;
		
		public DatabaseUpdate(String... sql) {
			this.sql = sql;
		}

		@Override
		public void run() {
			try {
				for (String sql : this.sql) {
					if (sql==null) {
						break;
					}
					update(sql);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class DatabaseUpdateGenKeys implements Callable<ResultSet> {
		private String sql;
		
		public DatabaseUpdateGenKeys(String sql) {
			this.sql = sql;
		}
		
		@Override
		public ResultSet call() throws Exception {
			return updateGenKeys(sql);
		}
		
	}
	
	private class DatabaseQuery implements Callable<ResultSet> {
		private String sql;
		
		public DatabaseQuery(String sql) {
			this.sql = sql;
		}
		
		@Override
		public ResultSet call() throws Exception {
			return query(sql);
		}
		
	}
}
