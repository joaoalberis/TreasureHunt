package com.github.joaoalberis.treasurehunt.models;

public class TreasureModel {

    private String id;
    private String command;
    private String world;
    private int x;
    private int y;
    private int z;

    public TreasureModel(String id, String command, String world, int x, int y, int z) {
        this.id = id;
        this.command = command;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }
}
