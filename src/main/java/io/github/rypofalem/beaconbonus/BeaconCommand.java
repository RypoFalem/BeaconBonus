package io.github.rypofalem.beaconbonus;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class BeaconCommand implements CommandExecutor{
	HashMap<UUID, BeaconRangeView> rangeViews = new HashMap<>();
	static BeaconCommand instance;

	BeaconCommand(){
		instance = this;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!(sender instanceof Player)){
			sender.sendMessage("This command is not usable via console.");
			return true;
		}
		Player player = (Player)sender;
		UUID uid = player.getUniqueId();
		if(rangeViews.containsKey(uid)){
			rangeViews.get(uid).hide();
			rangeViews.get(uid).cancelTask();
		}

		String info= null;
		for(BonusBeacon beacon : BeaconBonusPlugin.getInstance().getBeaconsEffectingLocation(player.getLocation())){
			rangeViews.put(player.getUniqueId(), new BeaconRangeView(player, beacon));
			if(info == null){
				info = getBeaconInfo(beacon);
			}else{
				info = info + "\n" + getBeaconInfo(beacon);
			}
		}
		if(info == null) info = String.format("%sYou are not in range of any bonus beacons.", ChatColor.GREEN.toString());
		player.sendMessage(info);
		return true;
	}

	String getBeaconInfo(BonusBeacon beacon){
		String info;
		Location loc = beacon.block.getLocation();
		info = String.format("%sBonus beacon location: %s%d, %d, %d\n", ChatColor.DARK_GREEN.toString(),
				ChatColor.GREEN.toString(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		loc = beacon.getMinCorner();
		info = String.format("%s%sCorner 1 location: %d, %d\n", info, ChatColor.GREEN.toString(),
				loc.getBlockX(), loc.getBlockZ());
		loc = beacon.getMaxCorner();
		info = String.format("%s%sCorner 2 location: %d, %d", info, ChatColor.GREEN.toString(),
				loc.getBlockX(), loc.getBlockZ());
		return info;
	}

	HashMap<UUID, BeaconRangeView> getRangeViews(){
		return rangeViews;
	}
}
