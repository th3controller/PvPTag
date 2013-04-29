package net.aemservers.pvptag;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

public class PvPTag extends JavaPlugin {
	
	Logger log = Logger.getLogger("Minecraft");
	
	public ArrayList<String> players = new ArrayList<String>();
	public ArrayList<String> flyingplayers = new ArrayList<String>();
	public HashMap<String, Long> playertime = new HashMap<String, Long>();
	public HashMap<String, Integer> pluginsettings = new HashMap<String, Integer>();
	
	@Override
	public void onEnable() {
		File file = new File("plugins/PvPTag", "config.yml");
		if(!file.exists()) {
			this.saveResource("config.yml", true);
		}
		pluginsettings.put("timevalue", 1000*getConfig().getInt("tag-settings.duration-in-seconds"));
		getServer().getPluginManager().registerEvents(new PvPTagListener(this), this);
		log.info("[PvPTag] Successfully initiated the plugin!");
	}
	@Override
	public void onDisable() {
		log.info("[PvPTag] Successfully terminated the plugin!");
	}
}
