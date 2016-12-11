package io.github.rypofalem.beaconbonus;


import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

public class BeaconRangeView {
	Player player;
	BonusBeacon beacon;
	ArrayList<Location> ghostBlocks;
	ViewTask viewTask;

	BeaconRangeView(Player player, BonusBeacon beacon){
		this.player = player;
		this.beacon = beacon;
		if(beacon.block.getWorld() != player.getWorld()){
			this.cancelTask();
			return;
		}

		ghostBlocks = new ArrayList<>();
		Location minCorner =beacon.getMinCorner();
		Location maxCorner = beacon.getMaxCorner();
		int x = minCorner.getBlockX()+1;
		int z = minCorner.getBlockZ();

		for(;x<=maxCorner.getBlockX(); x+=6){
			addGhostBlock(x, z);
		}
		x = maxCorner.getBlockX();
		for(;z<=maxCorner.getBlockZ(); z+=6){
			addGhostBlock(x, z);
		}
		z = maxCorner.getBlockZ();
		for(;x>=minCorner.getBlockX(); x-=6 ){
			addGhostBlock(x, z);
		}
		x= minCorner.getBlockX();
		for(;z>=minCorner.getBlockZ(); z-=6){
			addGhostBlock(x, z);
		}

		show();
		viewTask = new ViewTask();
		viewTask.runTaskTimer(BeaconBonusPlugin.getInstance(), 0, 20);
	}

	boolean addGhostBlock( int x, int z){
		int y = player.getWorld().getHighestBlockYAt(x, z)-1;
		if(y<1) y = 1;
		Location playerLoc = player.getLocation();
		Location location = new Location(player.getWorld(),x, y, z);
		final int range = 64;
		if(Math.abs(location.getBlockX() - playerLoc.getBlockX()) > range) return false;
		if(Math.abs(location.getBlockY() - playerLoc.getBlockY()) > range) return false;
		if(Math.abs(location.getBlockZ() - playerLoc.getBlockZ()) > range) return false;
		ghostBlocks.add(location);
		return true;
	}

	void show(){
		for(Location location : ghostBlocks){
			player.sendBlockChange(location, Material.SEA_LANTERN, (byte)0);
		}
	}

	void hide(){
		for(Location location : ghostBlocks){
			player.sendBlockChange(location, location.getBlock().getType(), location.getBlock().getData());
		}
	}

	void doParticles(){
		for(Location location : ghostBlocks){
			World world = location.getWorld();
			for(int height = 1; height <= 3; height++){
				world.spawnParticle(Particle.VILLAGER_HAPPY,
						location.getX() +.5, location.getY() + .5 + height, location.getZ() + .5,
						5, .5, .5, .5);
			}
		}
	}

	void cancelTask(){
		if(viewTask == null) return;
		viewTask.cancel();
		viewTask = null;
		BeaconCommand.instance.getRangeViews().remove(player.getUniqueId());
	}

	class ViewTask extends BukkitRunnable {
			int count = 0;
			@Override
			public void run() {
				count++;
				if(count >= 10){
					hide();
					cancelTask();
				}else{
					doParticles();
				}
			}
	}
}
