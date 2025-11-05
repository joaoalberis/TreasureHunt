package com.github.joaoalberis.treasurehunt.commands;

import org.bukkit.command.CommandSender;

public interface TreasureBaseCommand {

    String name();
    String usage();
    String permission();
    boolean execute(CommandSender sender, String label, String[] args);

}
