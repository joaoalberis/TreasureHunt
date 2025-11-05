package com.github.joaoalberis.treasurehunt.commands;

import org.bukkit.command.CommandSender;

public class TreasureHelp implements TreasureBaseCommand{
    @Override
    public String name() {
        return "help";
    }

    @Override
    public String usage() {
        return "/tc help";
    }

    @Override
    public String permission() {
        return "";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        sender.sendMessage("§8§m--------------------------------------------------");
        sender.sendMessage("§6§lTreasure Hunt §7v1.0 §8- §fCommand Guide");
        sender.sendMessage("§7Manage and explore the treasure system.");
        sender.sendMessage(" ");
        sender.sendMessage("§e/treasure create <id> <command>");
        sender.sendMessage("  §7Create a new treasure. After running the command,");
        sender.sendMessage("  §7right-click a block to set it as the treasure location.");
        sender.sendMessage(" ");
        sender.sendMessage("§e/treasure delete <id>");
        sender.sendMessage("  §7Delete an existing treasure by its ID.");
        sender.sendMessage(" ");
        sender.sendMessage("§e/treasure list");
        sender.sendMessage("  §7Show all registered treasures.");
        sender.sendMessage(" ");
        sender.sendMessage("§e/treasure completed <id>");
        sender.sendMessage("  §7List all players who have already claimed that treasure.");
        sender.sendMessage(" ");
        sender.sendMessage("§e/treasure help");
        sender.sendMessage("  §7Display this help menu.");
        sender.sendMessage(" ");
        sender.sendMessage("§8§m--------------------------------------------------");
        sender.sendMessage("§6Plugin created by §fMrJoao (Albis) §7| §eGitHub: §fgithub.com/joaoalberis");
        return true;
    }
}
