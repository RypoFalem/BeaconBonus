package io.github.rypofalem.beaconbonus;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Variant;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class BeaconBonusPlugin extends JavaPlugin implements Listener{
	public static final EntityType[] HOSTILEMOBS = {EntityType.BLAZE, EntityType.CAVE_SPIDER, EntityType.CREEPER, EntityType.ENDERMAN, EntityType.ENDERMITE,
			EntityType.GHAST, EntityType.GIANT, EntityType.GUARDIAN, EntityType.MAGMA_CUBE, EntityType.PIG_ZOMBIE, EntityType.SLIME,
			EntityType.SILVERFISH, EntityType.SKELETON, EntityType.SPIDER, EntityType.WITCH, EntityType.ZOMBIE, EntityType.SKELETON_HORSE};
	public static final Material[] LIGHTBLOCKS = {Material.GLOWSTONE, Material.SEA_LANTERN};
	boolean debug = false;
	HashMap<Block, BonusBeacon> beacons;
	BeaconCommand executor;
	static BeaconBonusPlugin plugin;

	public void onEnable(){
		saveDefaultConfig();
		reloadConfig();
		loadBeaconsFromConfig();
		Bukkit.getPluginManager().registerEvents(this, this);
		executor = new BeaconCommand();
		getCommand("beacon").setExecutor(executor);
		plugin = this;
	}

	void loadBeaconsFromConfig(){
		beacons = new HashMap<Block, BonusBeacon>();
		if(!getConfig().contains("beacons")) return;
		Set<String> savedBeacons = getConfig().getConfigurationSection("beacons").getKeys(false);
		for(String beacon : savedBeacons){
			int x,y,z;
			String  world;
			String[]  beaconInfo = beacon.split(",");
			try{
				world = beaconInfo[0];
				x = Integer.parseInt(beaconInfo[1]);
				y = Integer.parseInt(beaconInfo[2]);
				z = Integer.parseInt(beaconInfo[3]);
			} catch (Exception e) {
				if(beacon != null) getLogger().info(String.format("whoops, beaconID %s is invalid", beacon));
				e.printStackTrace();
				continue;
			}
			
			if(Bukkit.getWorld(world) == null){
				getLogger().info(String.format("Invalid world '%s' in %s", world, beacon));
				continue;
			}
			Block beaconBlock = Bukkit.getWorld(world).getBlockAt(x, y, z);
			BonusBeacon bonusBeacon = new BonusBeacon(beaconBlock);
			beacons.put(beaconBlock, bonusBeacon);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled= true)
	void onPlayerRightClickBeacon(PlayerInteractEvent e){
		if(e.getClickedBlock().getType().equals(Material.BEACON)){
			if(beacons.containsKey(e.getClickedBlock())){
				beacons.get(e.getClickedBlock()).scheduleUpdate();
			}else{
				if(addBeacon(e.getClickedBlock())){
					e.getPlayer().sendMessage(String.format("%sBonus beacon created!", ChatColor.GREEN));
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled= true)
	void onPlayerPlaceBeaconRelated(BlockPlaceEvent e){
		if(e.getBlock().getType().equals(Material.BEACON)){
			if(addBeacon(e.getBlock())){
				e.getPlayer().sendMessage(String.format("%sBonus beacon created!", ChatColor.GREEN));
			}
		}
		if(isLightBlock(e.getBlock().getType())){
			updateNearbyBeacons(e.getBlock().getLocation(), 9);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled= true)
	public void onPlayerBreakBeaconRelated(BlockBreakEvent e){
		Block block = e.getBlock();
		if(block.getType().equals(Material.BEACON)){
			if(beacons.containsKey(e.getBlock())){
				beacons.get(e.getBlock()).scheduleUpdate(true);
			}
		}
		if(isLightBlock(block.getType())){
			updateNearbyBeacons(e.getBlock().getLocation(), 9);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled= true)
	void onMonsterSpawn(CreatureSpawnEvent e){
		if(e.getSpawnReason() != SpawnReason.NATURAL && e.getSpawnReason() != SpawnReason.LIGHTNING) return;
		for(EntityType type : HOSTILEMOBS){
			if(e.getEntityType() == type){
				if(isInBeaconRange(e.getLocation())){
					e.setCancelled(true);
				}
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled= true)
	void onPistonAction(BlockPistonRetractEvent event){
		for(Block block : event.getBlocks()){
			if(isLightBlock(block.getType())){
				updateNearbyBeacons(block.getLocation(), 1 + 12 + 9); // piston + 12 blocks + 9 range
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled= true)
	void onPistonAction(BlockPistonExtendEvent event){
		for(Block block : event.getBlocks()){
			if(isLightBlock(block.getType())){
				updateNearbyBeacons(block.getLocation(), 1 + 12 + 9); // piston + 12 blocks + 9 range
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onExplode(EntityExplodeEvent event){
		for(Block block :event.blockList()){
			if(isLightBlock(block.getType())){
				updateNearbyBeacons(block.getLocation(), 9);
				return;
			}
		}
	}

	boolean isLightBlock(Material material){
		for(Material lightBlock : LIGHTBLOCKS){
			if(material == lightBlock) return true;
		}
		return false;
	}

	boolean addBeacon(BonusBeacon beacon){
		if(beacon.tier < 1 || beacon.block.getWorld().getEnvironment() != World.Environment.NORMAL){
			removeBeacon(beacon);
			return false;
		}
		beacons.put(beacon.block, beacon);
		getConfig().set("beacons." + beacon.getID() + ".tier", beacon.tier);
		saveConfig();
		return true;
	}

	boolean addBeacon(Block block){
		return addBeacon(new BonusBeacon(block));
	}

	void removeBeacon(BonusBeacon beacon){
		if(beacons.containsKey(beacon.block)){
			beacons.remove(beacon.block);
		}
		if(!getConfig().contains("beacons")) return;
		getConfig().getConfigurationSection("beacons").set(beacon.getID(), null);
		saveConfig();
	}
	
	void updateNearbyBeacons(Location location, int range){
		for(BonusBeacon beacon : getNearbyBeacons(location, range)){
			beacon.scheduleUpdate();
		}
	}

	boolean isInBeaconRange(Location loc){
		for(Block key : beacons.keySet()){
			if(beacons.get(key).isInRange(loc)){
				return true;
			}
		}
		return false;
	}
	
	/*
	 * Get all beacons that effect the given location
	 */
	List<BonusBeacon> getBeaconsEffectingLocation(Location loc){
		ArrayList<BonusBeacon> beaconsInRange = new ArrayList<BonusBeacon>();
		for(Block key : beacons.keySet()){
			if(beacons.get(key).isInRange(loc)) beaconsInRange.add(beacons.get(key));
		}
		return beaconsInRange;
	}
	
	/*
	 * Get all beacons that exist in a given range
	 */
	List<BonusBeacon> getNearbyBeacons(Location loc, int range){
		ArrayList<BonusBeacon> beaconsInRange = new ArrayList<BonusBeacon>();
		for(Block key : beacons.keySet()){
			if(beacons.get(key).isInRange(loc, range)) beaconsInRange.add(beacons.get(key));
		}
		return beaconsInRange;
	}
	
	public static BeaconBonusPlugin getInstance(){
		return plugin;
	}

}
