package com.bluelight.computer.winlauncher.prolauncher.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GroupedAppDao {
    @Insert
    void insert(GroupedApp app);

    @Query("SELECT * FROM grouped_apps WHERE groupName = :groupName")
    List<GroupedApp> getAppsForGroup(String groupName);


    @Query("SELECT COUNT(*) FROM grouped_apps WHERE packageName = :packageName AND groupName = :groupName")
    int isAppInGroup(String packageName, String groupName);


    @Query("DELETE FROM grouped_apps WHERE packageName = :packageName AND groupName = :groupName")
    void deleteAppFromGroup(String packageName, String groupName);

    @Query("UPDATE grouped_apps SET packageName = :newPackageName WHERE packageName = :oldPackageName AND groupName = :groupName")
    void updateAppInGroup(String oldPackageName, String newPackageName, String groupName);
}