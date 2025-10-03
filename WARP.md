# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is an Android launcher application written in Java that provides a Windows-like desktop experience on Android devices. The launcher supports:

- Multi-page home screen with app icons and widgets
- Drag-and-drop functionality for apps and widgets across pages  
- Taskbar with pinned apps and system controls
- File management integration
- Widget management and configuration
- Customizable themes and wallpapers

## Build Commands

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build  
```bash
./gradlew assembleRelease
```

### Install Debug APK
```bash
./gradlew installDebug
```

### Clean Build
```bash
./gradlew clean
./gradlew build
```

### Run Tests
```bash
./gradlew test
```

### Run Connected Tests (on device/emulator)
```bash
./gradlew connectedAndroidTest
```

## Architecture Overview

### Core Components

**LauncherActivity**: Main activity that hosts the ViewPager2 with home screen pages, manages the taskbar, handles system UI, and coordinates cross-page drag operations.

**HomeScreenFragment**: Individual pages in the ViewPager2. Each fragment manages a 6x4 grid layout for apps and widgets, handles touch interactions, and supports item positioning with Room database persistence.

**Database Layer (Room)**:
- `AppDatabase`: Main database with migration support (currently at version 10)
- `AppItem`: Entity representing apps, widgets, and special items with position, type, and metadata
- `AppItemDao`: Data access with LiveData support for reactive UI updates

**ViewModels**:
- `AppItemViewModel`: Manages app data and database operations
- `TaskbarViewModel`: Handles taskbar items and pinned apps  
- `SettingsViewModel`: Manages app settings and preferences

### Drag and Drop System

The launcher implements a sophisticated cross-page drag system:

1. **Touch Detection**: Custom touch listeners (`AppIconTouchListener`, `WidgetTouchListener`) detect long-press gestures
2. **Cross-Page Dragging**: `LauncherActivity` manages drag overlay and page transitions
3. **Item Placement**: `placeItemOnGridWithDisplacement` handles complex placement logic with automatic displacement of existing items

### Widget Management

- Uses `SafeAppWidgetHost` for widget lifecycle management
- Supports dynamic widget sizing with span calculations
- Handles widget configuration activities securely
- Automatic cleanup of orphaned widgets

## Key File Locations

- **Main Activity**: `app/src/main/java/com/bluelight/computer/winlauncher/prolauncher/ui/activity/LauncherActivity.java`
- **Home Screen**: `app/src/main/java/com/bluelight/computer/winlauncher/prolauncher/ui/fragment/HomeScreenFragment.java`  
- **Database**: `app/src/main/java/com/bluelight/computer/winlauncher/prolauncher/database/`
- **Adapters**: `app/src/main/java/com/bluelight/computer/winlauncher/prolauncher/ui/adapter/`
- **Models**: `app/src/main/java/com/bluelight/computer/winlauncher/prolauncher/model/`

## Development Notes

### Database Migrations
The app uses Room with careful migration handling. When adding new fields to `AppItem`, create a new migration in `AppDatabase.java` and increment the version number.

### Performance Optimizations
- RecyclerView item view caching is enabled for taskbar
- Glide image loading with disk caching for wallpapers
- Database operations run on background threads with ExecutorService
- Fragment lifecycle management prevents memory leaks

### Touch Event Handling
Complex touch event coordination between:
- Widget internal touch handling (scrolling, clicking)
- Launcher drag-and-drop operations  
- Long-press detection with haptic feedback
- Cross-page drag overlay management

### Dependencies Management
- Uses Gradle Version Catalogs (`gradle/libs.versions.toml`)
- Room database with annotation processing
- Glide for image loading and caching
- Material Design Components
- ViewPager2 with DotsIndicator
- Retrofit for network operations (wallpaper downloads)

## Debugging

### Database Inspection
Use Room's database export functionality and Android Studio's database inspector to examine the `launcher_database`.

### Touch Event Debugging  
Enable touch logging by setting log level to DEBUG for tags:
- `HomeScreenFragment`
- `LauncherActivity` 
- `AppIconTouchListener`
- `WidgetTouchListener`

### Widget Issues
Check AppWidgetHost status and widget IDs in logs. Orphaned widgets should be cleaned up automatically on app restart.

## Build Configuration

- **compileSdk**: 36
- **minSdk**: 25  
- **targetSdk**: 36
- **Java Version**: 11
- **Build Tools**: AGP 8.12.2
- **ProGuard**: Enabled for release builds with custom rules in `proguard-rules.pro`

## Permissions

Key permissions required:
- `QUERY_ALL_PACKAGES`: For app discovery
- `BIND_APPWIDGET`: For widget management
- `MANAGE_EXTERNAL_STORAGE`: For file management
- `PACKAGE_USAGE_STATS`: For recent apps
- Various system permissions for launcher functionality