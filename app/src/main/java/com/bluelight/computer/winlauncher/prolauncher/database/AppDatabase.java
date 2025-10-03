package com.bluelight.computer.winlauncher.prolauncher.database;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telecom.TelecomManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {AppItem.class}, version = 10, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {


    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grid_items ADD COLUMN page_number INTEGER NOT NULL DEFAULT 0");
        }
    };
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grid_items ADD COLUMN original_name TEXT");
        }
    };
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grid_items ADD COLUMN custom_icon_path TEXT");
        }
    };
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grid_items ADD COLUMN app_widget_id INTEGER DEFAULT -1");
            database.execSQL("ALTER TABLE grid_items ADD COLUMN item_type TEXT DEFAULT 'APP'");
        }
    };
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grid_items ADD COLUMN is_special_item INTEGER DEFAULT 0");
        }
    };
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE grid_items_temp (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "item_type TEXT, " +
                    "row_position INTEGER NOT NULL, " +
                    "col_position INTEGER NOT NULL, " +
                    "page_number INTEGER NOT NULL, " +
                    "package_name TEXT, " +
                    "activity_name TEXT, " +
                    "app_widget_id INTEGER NOT NULL DEFAULT -1, " +
                    "original_name TEXT, " +
                    "custom_icon_path TEXT, " +
                    "is_special_item INTEGER NOT NULL DEFAULT 0)");
            database.execSQL("INSERT INTO grid_items_temp SELECT id, " +
                    "CASE WHEN item_type = 0 THEN 'APP' WHEN item_type = 1 THEN 'WIDGET' ELSE 'APP' END, " +
                    "row_position, col_position, page_number, " +
                    "package_name, activity_name, app_widget_id, " +
                    "original_name, custom_icon_path, is_special_item " +
                    "FROM grid_items");
            database.execSQL("DROP TABLE grid_items");
            database.execSQL("ALTER TABLE grid_items_temp RENAME TO grid_items");
        }
    };
    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grid_items ADD COLUMN category TEXT");
            database.execSQL("ALTER TABLE grid_items ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0");
        }
    };
    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grid_items ADD COLUMN category TEXT");
            database.execSQL("ALTER TABLE grid_items ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0");
        }
    };
    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grid_items ADD COLUMN row_span INTEGER NOT NULL DEFAULT 1");
            database.execSQL("ALTER TABLE grid_items ADD COLUMN col_span INTEGER NOT NULL DEFAULT 1");
        }
    };
    private static final String TAG = "AppDatabase";
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "launcher_database")
                            .addMigrations(
                                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                                    MIGRATION_9_10
                            )
                            .fallbackToDestructiveMigration()

                            .addCallback(roomDatabaseCallback(context.getApplicationContext()))
                            .build();
                }
            }
        }
        return INSTANCE;
    }


    private static RoomDatabase.Callback roomDatabaseCallback(final Context context) {
        return new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);
                Log.d(TAG, "Database onCreate, scheduling default apps to be pinned.");

                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {

                    AppItemDao dao = getDatabase(context).appItemDao();

                    pinDefaultApps(context, dao);
                });
            }
        };
    }


    private static void pinDefaultApps(Context context, AppItemDao dao) {
        List<String> defaultPackages = new ArrayList<>();


        defaultPackages.add("com.android.chrome");


        String smsPackage = Telephony.Sms.getDefaultSmsPackage(context);
        if (smsPackage != null && !smsPackage.isEmpty()) {
            defaultPackages.add(smsPackage);
        }


        String dialerPackage = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null) {
                dialerPackage = telecomManager.getDefaultDialerPackage();
            }
        } else {

            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            if (context.getPackageManager().resolveActivity(dialIntent, 0) != null) {
                dialerPackage = context.getPackageManager().resolveActivity(dialIntent, 0).activityInfo.packageName;
            }
        }
        if (dialerPackage != null && !dialerPackage.isEmpty()) {
            defaultPackages.add(dialerPackage);
        }

        Log.d(TAG, "Attempting to pin default packages: " + defaultPackages);


        for (String pkgName : defaultPackages) {
            if (pkgName == null) continue;


            AppItem itemToPin = dao.findItemByPackageNameNow(pkgName);

            if (itemToPin != null) {
                itemToPin.isPinned = true;
                dao.update(itemToPin);
                Log.i(TAG, "Successfully pinned default app: " + pkgName);
            } else {
                Log.w(TAG, "Could not find default app to pin: " + pkgName + ". It might not be installed or scanned into the DB yet.");
            }
        }
    }

    public abstract AppItemDao appItemDao();
}
