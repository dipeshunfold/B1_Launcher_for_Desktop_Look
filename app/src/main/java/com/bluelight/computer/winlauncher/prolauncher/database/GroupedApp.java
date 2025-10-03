package com.bluelight.computer.winlauncher.prolauncher.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "grouped_apps")
public class GroupedApp {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String packageName;
    public String appName;
    public String groupName;
}