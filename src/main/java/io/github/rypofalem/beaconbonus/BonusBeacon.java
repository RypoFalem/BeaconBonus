package io.github.rypofalem.beaconbonus;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;


public class BonusBeacon {
	Block block;
	private Location minCorner;
	private Location maxCorner;
	int tier=0;
	final int MINUPDATETIME = 20 * 5; //minimum time before beacon can be recalculated
	Updater updater = new Updater();

	/*
	 * Create BonusBeacon
	 */
	BonusBeacon(Block block){
		this.block = block;
		this.tier = calculateTier();
		calculateCorners();
	}

	/*
	 * Create BonusBeacon using a cached tier
	 */
	BonusBeacon(Block loc, int tier){
		this.block = loc;
		this.tier = tier;
		calculateCorners();
	}

	/*
	 * returns true if location is within the beacons standard range
	 * based on the tier of the beacon
	 */
	public boolean isInRange(Location loc){
		if(getRange() == 0) return false;
		if(minCorner == null || maxCorner == null){
			if(!calculateCorners()) return false;
		}
		return isInRange(loc, getRange());
	}

	/*
	 * returns true if location is with beacon's range
	 * based on provided radius
	 */
	public boolean isInRange(Location loc, int radius){
		if(loc.getWorld() != block.getWorld()) return false;
		int minX = block.getX() - radius;
		int maxX = block.getX() + radius;
		int minZ = block.getZ() - radius;
		int maxZ = block.getZ() + radius;
		if(loc.getBlockX() <= minX) return false;
		if(loc.getBlockZ() <= minZ) return false;
		if(loc.getBlockX() >= maxX) return false;
		if(loc.getBlockZ() >= maxZ) return false;
		return true;
	}

	/* 
	 * Assigns a range based on the tier of the beacon.
	 * Returns true if successful, false if the range
	 * for the tier is not defined.
	 */
	boolean calculateCorners(){
		int range = getRange();
		if(range <= 0) return false;
		minCorner = new Location(block.getWorld(), block.getX() - range, 0, block.getZ() - range);
		maxCorner = new Location(block.getWorld(), block.getX() + range, 0, block.getZ() + range);
		return true;
	}

	int calculateTier(){
		int layer = 1;
		while(block.getY() - layer >= 0){
			for(int x = block.getX() - layer; x<= block.getX() + layer; x++){
				for(int z = block.getZ() - layer; z<= block.getZ() + layer; z++){
					if( !isLightBlock(block.getWorld().getBlockAt(x, block.getY() - layer, z))) return layer -1;
				}
			}
			layer++;
		}
		return layer-1; //only reachable when it would otherwise check y= -1
	}

	/*
	 * Vanilla range formula based on tier, max range 50 at tier 4.
	 */
	public int getRange(){
		if(tier < 1) return 0;
		return Math.min(50, (tier+1)*10);
	}

	/*
	 * Returns false if the block is no longer a beacon.
	 * Otherwise it recalculates the beacon tier and returns true
	 */
	private boolean updateBeacon(){
		if(block.getType() != Material.BEACON){
			BeaconBonusPlugin.getInstance().removeBeacon(this);
			return false;
		}
		int oldTier = tier;
		tier = calculateTier();
		if(calculateTier() != oldTier){
			calculateCorners();
			BeaconBonusPlugin.getInstance().addBeacon(this);
		}
		return true;
	}

	public void scheduleUpdate(){
		scheduleUpdate(false);
	}

	public void scheduleUpdate(boolean now){
		if(now){
			updateBeacon();
			return;
		}
		if(updater != null && updater.isScheduled) return;
		updater = new Updater();
		updater.runTaskLater(BeaconBonusPlugin.getInstance(), 5*20);
		updater.isScheduled = true;
	}

	static boolean isLightBlock(Location oreLoc){
		return isLightBlock(oreLoc.getBlock());
	}

	static boolean isLightBlock(Block oreBlock){
		for(Material mat : BeaconBonusPlugin.LIGHTBLOCKS){
			if(oreBlock.getType() == mat) return true;
		}
		return false;
	}

	/*
	 * Unique ID for saving
	 */
	public String getID(){
		return String.format("%s,%d,%d,%d", block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	public Location getMinCorner() {
		return minCorner.clone();
	}

	public Location getMaxCorner(){
		return maxCorner.clone();
	}

	private class Updater extends BukkitRunnable{
		boolean isScheduled = false;

		@Override
		public void run() {
			updateBeacon();
			isScheduled = false;
			updater = null;
		}
	}
}
