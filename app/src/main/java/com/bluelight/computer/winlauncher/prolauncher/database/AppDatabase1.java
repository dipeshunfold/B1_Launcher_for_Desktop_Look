package com.bluelight.computer.winlauncher.prolauncher.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {GroupedApp.class}, version = 1)
public abstract class AppDatabase1 extends RoomDatabase {

    private static volatile AppDatabase1 INSTANCE;

    public static AppDatabase1 getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase1.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase1.class, "app_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract GroupedAppDao groupedAppDao();
}