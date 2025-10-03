package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.BaseFileFragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    private static final Map<String, Integer> iconMap = new HashMap<>();

    private static final int VIEW_TYPE_LIST = 1;
    private static final int VIEW_TYPE_GRID = 2;

    static {
        iconMap.put("pdf", R.drawable.ic_pdf);
        iconMap.put("txt", R.drawable.ic_txt);
        iconMap.put("doc", R.drawable.ic_docx);
        iconMap.put("docx", R.drawable.ic_docx);
        iconMap.put("xls", R.drawable.ic_xls);
        iconMap.put("xlsx", R.drawable.ic_xls);
        iconMap.put("zip", R.drawable.ic_file_zip);
        iconMap.put("rar", R.drawable.ic_file_rar);
        iconMap.put("apk", R.drawable.ic_apk);
    }

    private final Context context;
    private final ArrayList<File> displayList;
    private final ArrayList<File> originalList;

    private final Set<Integer> selectedPositions = new HashSet<>();
    private OnItemClickListener itemClickListener;
    private OnItemLongClickListener itemLongClickListener;
    private OnSelectionChangedListener selectionChangedListener;
    private BaseFileFragment.LayoutMode layoutMode = BaseFileFragment.LayoutMode.LIST;

    public FileAdapter(Context context, ArrayList<File> fileList) {
        this.context = context;
        this.displayList = fileList != null ? fileList : new ArrayList<>();
        this.originalList = new ArrayList<>(this.displayList);
    }

    public void sort(BaseFileFragment.SortType sortType) {
        Comparator<File> comparator = (file1, file2) -> {
            boolean isDir1 = file1.isDirectory();
            boolean isDir2 = file2.isDirectory();

            if (isDir1 && !isDir2) {
                return -1;
            }
            if (!isDir1 && isDir2) {
                return 1;
            }

            switch (sortType) {
                case DATE:
                    return Long.compare(file2.lastModified(), file1.lastModified());
                case SIZE:
                    return Long.compare(file2.length(), file1.length());
                case NAME:
                default:
                    return file1.getName().compareToIgnoreCase(file2.getName());
            }
        };

        Collections.sort(originalList, comparator);
        Collections.sort(displayList, comparator);
        notifyDataSetChanged();
    }


    public void filter(String query) {
        displayList.clear();
        if (query == null || query.isEmpty()) {
            displayList.addAll(originalList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (File file : originalList) {
                if (file.getName().toLowerCase().contains(lowerCaseQuery)) {
                    displayList.add(file);
                }
            }
        }
        notifyDataSetChanged();
    }

    public List<File> getDisplayList() {
        return displayList;
    }


    public List<File> getOriginalList() {
        return originalList;
    }

    @Override
    public int getItemCount() {
        return displayList != null ? displayList.size() : 0;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = (layoutMode == BaseFileFragment.LayoutMode.LIST)
                ? R.layout.item_file_list
                : R.layout.item_file_grid;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= displayList.size()) return;
        File file = displayList.get(position);
        holder.fileName.setText(file.getName());

        if (file.isDirectory()) {
            holder.fileIcon.setImageResource(R.drawable.ic_folder);
            if (holder.fileSize != null) holder.fileSize.setVisibility(View.GONE);
            holder.checkBox.setVisibility(View.GONE);
            holder.itemView.setAlpha(1.0f);
        } else {
            if (holder.fileSize != null) {
                holder.fileSize.setVisibility(View.VISIBLE);
                holder.fileSize.setText(String.format("%.2f MB", (double) file.length() / (1024 * 1024)));
            }
            setFileIcon(holder.fileIcon, file);
            boolean selectionModeActive = !selectedPositions.isEmpty();
            holder.checkBox.setVisibility(selectionModeActive ? View.VISIBLE : View.GONE);
            boolean isSelected = selectedPositions.contains(position);
            holder.checkBox.setChecked(isSelected);
            holder.itemView.setAlpha(isSelected ? 0.7f : 1.0f);
        }
    }

    public List<File> getSelectedFiles() {
        List<File> selectedFiles = new ArrayList<>();
        for (int position : selectedPositions) {
            if (position >= 0 && position < displayList.size()) {
                selectedFiles.add(displayList.get(position));
            }
        }
        return selectedFiles;
    }

    public void toggleSelection(int position) {
        int selectionCountBefore = selectedPositions.size();
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        int selectionCountAfter = selectedPositions.size();
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectionCountAfter);
        }
        if ((selectionCountBefore == 0 && selectionCountAfter > 0) || (selectionCountBefore > 0 && selectionCountAfter == 0)) {
            notifyDataSetChanged();
        } else {
            notifyItemChanged(position);
        }
    }

    public void clearSelections() {
        if (selectedPositions.isEmpty()) return;
        selectedPositions.clear();
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(0);
        }
        notifyDataSetChanged();
    }

    private void setFileIcon(ImageView imageView, File file) {
        String extension = getFileExtension(file.getName());
        Integer iconRes = iconMap.get(extension);
        if (iconRes != null) {
            imageView.setImageResource(iconRes);
        } else {
            Glide.with(context).load(file).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(R.drawable.ic_image).error(R.drawable.ic_doccuement).into(imageView);
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    public void setLayoutMode(BaseFileFragment.LayoutMode layoutMode) {
        this.layoutMode = layoutMode;

        notifyDataSetChanged();
    }


    @Override
    public int getItemViewType(int position) {
        return (layoutMode == BaseFileFragment.LayoutMode.GRID) ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.itemLongClickListener = listener;
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView fileIcon;
        TextView fileName, fileSize;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            fileName = itemView.findViewById(R.id.fileName);
            fileSize = itemView.findViewById(R.id.fileSize);
            checkBox = itemView.findViewById(R.id.checkBox);
            fileName.setSelected(true);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                if (!selectedPositions.isEmpty()) {
                    toggleSelection(position);
                } else {
                    if (itemClickListener != null) {
                        itemClickListener.onItemClick(position);
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return false;
                toggleSelection(position);
                if (itemLongClickListener != null) {
                    itemLongClickListener.onItemLongClick(position);
                }
                return true;
            });
        }
    }
}