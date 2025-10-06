package com.bluelight.computer.winlauncher.prolauncher.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.HomeScreenFragment;

import java.util.List;

@Dao
public interface AppItemDao {
    @Insert
    long insert(AppItem item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AppItem> items);

    @Update
    void update(AppItem item);

    @Delete
    void delete(AppItem item);

    @Query("SELECT * FROM grid_items")
    LiveData<List<AppItem>> getAllItems();

    @Query("SELECT * FROM grid_items WHERE page_number = :pageIndex")
    LiveData<List<AppItem>> getItemsForPage(int pageIndex);

    @Query("SELECT * FROM grid_items")
    List<AppItem> getAllItemsNow();

    @Query("SELECT * FROM grid_items WHERE page_number = :pageId")
    List<AppItem> getItemsForPageNow(int pageId);

    @Query("SELECT * FROM grid_items WHERE package_name = :packageName")
    List<AppItem> getItemsByPackageNameNow(String packageName);

    @Query("DELETE FROM grid_items")
    void deleteAllItems();

    @Query("DELETE FROM grid_items WHERE package_name = :packageName")
    void deleteByPackageName(String packageName);

    @Query("DELETE FROM grid_items WHERE page_number = :pageId")
    void deletePage(int pageId);

    @Query("UPDATE grid_items SET page_number = page_number - 1 WHERE page_number > :pageId")
    void shiftPagesDown(int pageId);

    // Find the first free cell on a page with bounds and data validation
    // Implements column-first search (top to bottom, left to right)
    @Transaction
    default int[] findFirstFreeCellOnPageNow(int pageId, int requiredRowSpan, int requiredColSpan, int totalRows, int totalCols) {
        // Validate grid dimensions
        if (totalRows <= 0 || totalCols <= 0) return null;

        // Normalize requested spans
        if (requiredRowSpan <= 0) requiredRowSpan = 1;
        if (requiredColSpan <= 0) requiredColSpan = 1;
        if (requiredRowSpan > totalRows || requiredColSpan > totalCols) return null;

        List<AppItem> itemsOnPage = getItemsForPageNow(pageId);
        boolean[][] occupied = new boolean[totalRows][totalCols];
        int[] columnHeights = new int[totalCols];

        // Populate the occupied grid and calculate column heights
        for (AppItem item : itemsOnPage) {
            int baseRow = item.getRow();
            int baseCol = item.getCol();
            int rSpan = Math.max(1, item.rowSpan);
            int cSpan = Math.max(1, item.colSpan);

            // Skip items with invalid/unplaced coordinates
            if (baseRow < 0 || baseCol < 0) continue;

            for (int r = baseRow; r < baseRow + rSpan && r < totalRows; r++) {
                if (r < 0) continue;
                for (int c = baseCol; c < baseCol + cSpan && c < totalCols; c++) {
                    if (c < 0) continue;
                    occupied[r][c] = true;
                    // Update column height
                    columnHeights[c] = Math.max(columnHeights[c], r + 1);
                }
            }
        }

        // Find the column with minimum height
        int minColumn = 0;
        for (int c = 1; c < totalCols; c++) {
            if (columnHeights[c] < columnHeights[minColumn]) {
                minColumn = c;
            }
        }

        // Try to place in the column with minimum height first
        for (int c = minColumn; c < minColumn + totalCols; c++) {
            int currentCol = c % totalCols;
            if (currentCol + requiredColSpan > totalCols) continue;

            // Check each position in the column
            for (int row = 0; row <= totalRows - requiredRowSpan; row++) {
                boolean blockIsFree = true;

                // Check if the block is free
                for (int r = row; r < row + requiredRowSpan; r++) {
                    for (int cc = currentCol; cc < currentCol + requiredColSpan; cc++) {
                        if (r < 0 || r >= totalRows || cc < 0 || cc >= totalCols || occupied[r][cc]) {
                            blockIsFree = false;
                            break;
                        }
                    }
                    if (!blockIsFree) break;
                }

                if (blockIsFree) {
                    return new int[]{row, currentCol, pageId};
                }
            }
        }

        // Fallback: If no space found in the column with minimum height, do a full grid search
        // Column-first: iterate through each column, then each row within that column
        for (int c = 0; c < totalCols; c++) {
            if (c + requiredColSpan > totalCols) continue;

            for (int r = 0; r <= totalRows - requiredRowSpan; r++) {
                boolean blockIsFree = true;

                // Check if the block is free
                for (int rr = r; rr < r + requiredRowSpan; rr++) {
                    for (int cc = c; cc < c + requiredColSpan; cc++) {
                        if (rr < 0 || cc < 0 || rr >= totalRows || cc >= totalCols || occupied[rr][cc]) {
                            blockIsFree = false;
                            break;
                        }
                    }
                    if (!blockIsFree) break;
                }

                if (blockIsFree) {
                    return new int[]{r, c, pageId};
                }
            }
        }

        return null;
    }

    @Transaction
    default void deletePageAndShiftRemaining(int pageId) {
        // Only delete the page and shift remaining page indices down.
        deletePage(pageId);
        shiftPagesDown(pageId);
    }

    default int[] findFirstFreeCell(boolean[][] occupied, int rows, int cols) {
        for (int r = 0; r < rows; r++) { // Iterate rows then columns for a more natural flow
            for (int c = 0; c < cols; c++) {
                if (!occupied[r][c]) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    default int[] findLastFreeCellOnPageNow(int pageId, int totalRows, int totalCols) {
        List<AppItem> itemsOnPage = getItemsForPageNow(pageId);
        boolean[][] occupied = new boolean[totalRows][totalCols];

        for (AppItem item : itemsOnPage) {
            int baseRow = item.getRow();
            int baseCol = item.getCol();
            int rSpan = Math.max(1, item.rowSpan);
            int cSpan = Math.max(1, item.colSpan);

            if (baseRow < 0 || baseCol < 0) continue;

            for (int r = baseRow; r < baseRow + rSpan && r < totalRows; r++) {
                for (int c = baseCol; c < baseCol + cSpan && c < totalCols; c++) {
                    occupied[r][c] = true;
                }
            }
        }

        // Find the last occupied cell in column-first order (left to right, top to bottom)
        int lastOccupiedRow = -1;
        int lastOccupiedCol = -1;
        
        for (int c = 0; c < totalCols; c++) {
            for (int r = 0; r < totalRows; r++) {
                if (occupied[r][c]) {
                    lastOccupiedCol = c;
                    lastOccupiedRow = r;
                }
            }
        }

        // If no items on page, return first cell
        if (lastOccupiedRow == -1 && lastOccupiedCol == -1) {
            return new int[]{0, 0, pageId};
        }

        // Find the next free cell after the last occupied cell in column-first order
        // Start from the cell after the last occupied one
        boolean searchStarted = false;
        for (int c = 0; c < totalCols; c++) {
            for (int r = 0; r < totalRows; r++) {
                // Skip until we pass the last occupied cell
                if (!searchStarted) {
                    if (c == lastOccupiedCol && r == lastOccupiedRow) {
                        searchStarted = true;
                    }
                    continue;
                }
                
                // Found a free cell after the last occupied one
                if (!occupied[r][c]) {
                    return new int[]{r, c, pageId};
                }
            }
        }

        // No free cell found after last occupied, search from beginning
        for (int c = 0; c < totalCols; c++) {
            for (int r = 0; r < totalRows; r++) {
                if (!occupied[r][c]) {
                    return new int[]{r, c, pageId};
                }
            }
        }

        return null;
    }
    @Query("SELECT * FROM grid_items WHERE is_pinned = 1")
    LiveData<List<AppItem>> getPinnedItems();


    @Query("SELECT * FROM grid_items WHERE package_name = :packageName LIMIT 1")
    AppItem findItemByPackageNameNow(String packageName);


    @Query("SELECT * FROM grid_items WHERE package_name = :packageName AND activity_name = :className AND item_type = 'WIDGET' LIMIT 1")
    AppItem findWidgetByProvider(String packageName, String className);

    @Query("SELECT COUNT(*) FROM grid_items WHERE page_number = :pageId")
    int getItemCountForPage(int pageId);

    @Query("SELECT DISTINCT page_number FROM grid_items ORDER BY page_number")
    List<Integer> getAllPageIds();
    
    @Query("SELECT * FROM grid_items WHERE item_type = 'WIDGET'")
    List<AppItem> getWidgetsSync();

}