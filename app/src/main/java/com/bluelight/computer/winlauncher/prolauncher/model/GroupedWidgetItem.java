package com.bluelight.computer.winlauncher.prolauncher.model;

import android.appwidget.AppWidgetProviderInfo;

import java.util.List;

public class GroupedWidgetItem {
    public ItemType type;
    public String headerTitle;
    public List<AppWidgetProviderInfo> widgets; // Only for WIDGET_GROUP type
    // Constructor for a header item
    public GroupedWidgetItem(String headerTitle) {
        this.type = ItemType.HEADER;
        this.headerTitle = headerTitle;
    }

    // Constructor for a widget group item
    public GroupedWidgetItem(List<AppWidgetProviderInfo> widgets) {
        this.type = ItemType.WIDGET_GROUP;
        this.widgets = widgets;
    }

    public enum ItemType {
        HEADER,
        WIDGET_GROUP
    }
}