package de.outlook.therealspeedy.besocial.commands;

import de.outlook.therealspeedy.besocial.util.Cooldown;
import de.outlook.therealspeedy.besocial.util.Database;
import de.outlook.therealspeedy.besocial.util.Messages;
import de.outlook.therealspeedy.besocial.util.Players;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;

public class SimpleSocialCommand implements CommandExecutor {

    private static final double maxDistance = 20.0; // Maximum interaction distance in blocks

    @Override
    public boolean onCommand(CommandSender sender, Command cmdRaw, String label, String[] args) {
        String cmd = cmdRaw.getName().toLowerCase();

        /*
        tests to pass:
            sender is member
            target is set
            cooldown is not active
            target is online
            target is sender >> selfsocial message and particles
            target is member
            target is not ignoring sender
            sender is not ignoring target
            target is in range of sender
            >> all passed >> social messages and particles
         */

        if (Players.notMember((Player) sender)) {
            sender.sendMessage(Messages.getPrefix() + Messages.getInfoMessage("messages.sender.error.senderNotMember"));
            return false;
        }
        else {
            if (args.length == 0) {
                sender.sendMessage(Messages.getPrefix() + ChatColor.RESET + "" + ChatColor.RED + "/"+cmd+" <playername>");
                return false;
            }
            else {

                if (Cooldown.cooldownActive((Player) sender, cmd)){
                    sender.sendMessage(Messages.getCooldownErrorMessage());
                    return false;
                }

                Player target = Bukkit.getPlayerExact(args[0]);
                
                if (target == null) {
                    sender.sendMessage(Messages.getPrefix() + Messages.getInfoMessage("messages.sender.error.targetOffline"));
                    return false;
                }
                else if (Players.samePlayer(sender, target)) {
                    sender.sendMessage(Messages.getPrefix() + Messages.getInfoMessage("messages.sender.error.selfSocial."+cmd));
                    Players.spawnParticles((Player) sender, target, cmd);
                    return true;
                }
                else {
                    if (Players.notMember(target)) {
                        sender.sendMessage(Messages.getPrefix() + Messages.getInfoMessage("messages.sender.error.targetNotMember"));
                        return true;
                    }
                    else if (Players.targetIsIgnoringSender((Player) sender, target)) {
                        sender.sendMessage(Messages.getPrefix() + Messages.getInfoMessage("messages.sender.error.targetIgnoringSender"));
                        return true;
                    }
                    else if (Players.targetIsIgnoringSender(target, (Player) sender)) {
                        sender.sendMessage(Messages.getPrefix() + Messages.getInfoMessage("messages.sender.error.senderIgnoringTarget"));
                        return true;
                    }
                    else {
                        // test for range
                        Player senderPlayer = (Player) sender;
                        Location senderLocation = senderPlayer.getLocation();
                        Location targetLocation = target.getLocation();
   
                        World senderWorld = senderLocation.getWorld();
                        World targetWorld = targetLocation.getWorld();
                        
                        // Check if players are in different worlds
                        if (!senderWorld.equals(targetWorld)) {
                          sender.sendMessage(Messages.getPrefix() + Messages.getInfoMessage("messages.sender.error.targetOutOfRange"));
                          return true;
                        }
                        
                        double dx = senderLocation.getX() - targetLocation.getX();
                        double dy = senderLocation.getY() - targetLocation.getY();
                        double dz = senderLocation.getZ() - targetLocation.getZ();
                        double distance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2) + Math.pow(dz, 2));

                        if (distance >= maxDistance) {
                          sender.sendMessage(Messages.getPrefix() + Messages.getInfoMessage("messages.sender.error.targetOutOfRange"));
                          return true;
                        }

                        sender.sendMessage(Messages.getPrefix() + Messages.getSocialMessage("messages.sender.success."+cmd, (Player) sender, target));
                        target.sendMessage(Messages.getPrefix() + Messages.getSocialMessage("messages.target.success."+cmd, (Player) sender, target));
                        Players.spawnParticles((Player) sender, target, cmd);
                        databaseLogBridge(cmd, (Player) sender, target);
                        return true;
                    }
                }
            }
        }
    }


    private static void databaseLogBridge(String command, Player sender, Player target) {
        Database.logAction(sender, "send"+command);
        Database.logAction(target, "receive"+command);
    }


}
