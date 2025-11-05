package com.github.joaoalberis.treasurehunt.models;

public class PendingCreation {
    private final String id;
    private final String command;

    public PendingCreation(String id, String command) {
        this.id = id;
        this.command = command;
    }

    public String getId() {
        return id;
    }

    public String getCommand() {
        return command;
    }
}
