package net.kaikk.mc.gpp.skyblock;

import com.earth2me.essentials.Essentials;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Created by TimeTheCat on 8/22/2016.
 */
public class EssentialsHelper {

    public static void createHome(UUID playerID, Location spawn) {
        Essentials ess = (Essentials) Bukkit.getServer().getPluginManager().getPlugin("Essentials");
        Player player = Bukkit.getPlayer(playerID);
        ess.getUser(player).setHome("island", spawn);
    }
}
