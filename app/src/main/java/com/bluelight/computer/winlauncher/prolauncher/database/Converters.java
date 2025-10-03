package com.bluelight.computer.winlauncher.prolauncher.database;

import androidx.room.TypeConverter;

public class Converters {
    @TypeConverter
    public static AppItem.Type toType(String value) {
        if (value == null) {
            return AppItem.Type.APP;
        }
        try {
            return AppItem.Type.valueOf(value);
        } catch (IllegalArgumentException e) {
            return AppItem.Type.APP;
        }
    }

    @TypeConverter
    public static String fromType(AppItem.Type type) {
        return type == null ? AppItem.Type.APP.name() : type.name();
    }

    @TypeConverter
    public static boolean toBoolean(int value) {
        return value == 1;
    }

    @TypeConverter
    public static int fromBoolean(boolean value) {
        return value ? 1 : 0;
    }
}