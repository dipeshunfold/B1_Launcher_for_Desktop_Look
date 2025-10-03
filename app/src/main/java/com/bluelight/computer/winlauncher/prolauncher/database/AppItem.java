
package com.bluelight.computer.winlauncher.prolauncher.database;

import android.content.pm.ActivityInfo;
import android.view.View;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "grid_items")
public class AppItem {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "item_type")
    public Type type = Type.APP;
    @ColumnInfo(name = "row_position")
    public int row;
    @ColumnInfo(name = "col_position")
    public int col;
    @ColumnInfo(name = "row_span", defaultValue = "1")
    public int rowSpan = 1; // New field for widget row span
    @ColumnInfo(name = "col_span", defaultValue = "1")
    public int colSpan = 1; // New field for widget column span
    @ColumnInfo(name = "page_number")
    public int page = 0;
    @ColumnInfo(name = "package_name")
    public String packageName;
    @ColumnInfo(name = "activity_name")
    public String activityName;
    @ColumnInfo(name = "app_widget_id")
    public int appWidgetId = -1;
    @ColumnInfo(name = "original_name")
    public String originalName;
    @ColumnInfo(name = "custom_icon_path")
    public String customIconPath;
    @ColumnInfo(name = "is_special_item")
    public boolean isSpecialItem = false;
    @ColumnInfo(name = "category")
    public String category;
    @ColumnInfo(name = "is_pinned", defaultValue = "0")
    public boolean isPinned = false;

    @Ignore
    private int originalWidth = 0;
    @Ignore
    private int originalHeight = 0;
    @Ignore
    public String name;
    @Ignore
    public View view;
    @Ignore
    public ActivityInfo activityInfo;

    public AppItem() {
    }

    @Ignore
    public AppItem(String name, Type type, int row, int col, ActivityInfo activityInfo) {
        this.originalName = name;
        this.name = name;
        this.type = type;
        this.row = row;
        this.col = col;
        this.rowSpan = (type == Type.WIDGET) ? 2 : 1; // Default for widgets, will be updated later
        this.colSpan = (type == Type.WIDGET) ? 2 : 1; // Default for widgets, will be updated later
        this.activityInfo = activityInfo;
        if (activityInfo != null) {
            this.packageName = activityInfo.packageName;
            this.activityName = activityInfo.name;
        }
        this.isPinned = false;
    }

    public String getName() {
        return name != null ? name : originalName;
    }

    public void setName(String name) {
        this.name = name;
        this.originalName = name;
    }

    public Type getType() {
        return type;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int getRowSpan() {
        return rowSpan;
    }

    public void setRowSpan(int rowSpan) {
        this.rowSpan = rowSpan;
    }

    public int getColSpan() {
        return colSpan;
    }

    public void setColSpan(int colSpan) {
        this.colSpan = colSpan;
    }

    public int getOriginalWidth() {
        return originalWidth > 0 ? originalWidth : colSpan;
    }

    public int getOriginalHeight() {
        return originalHeight > 0 ? originalHeight : rowSpan;
    }

    public void setOriginalDimensions(int width, int height) {
        this.originalWidth = width;
        this.originalHeight = height;
    }

    public ActivityInfo getActivityInfo() {
        return activityInfo;
    }

    public void setActivityInfo(ActivityInfo activityInfo) {
        this.activityInfo = activityInfo;
    }

    public int getAppWidgetId() {
        return appWidgetId;
    }

    public void setAppWidgetId(int id) {
        this.appWidgetId = id;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }

    public void setPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public enum Type {
        APP,
        WIDGET,
        WALLPAPER_ACTION,
        SETTING,
        FILE_MANAGER_ACTION,
        SOCIAL_MEDIA_FOLDER,
        USER,
        GOOGLE_FOLDER
    }

    public int getPage() {
        return Math.max(0, page); // Replace 'page' with your actual field name
    }
}