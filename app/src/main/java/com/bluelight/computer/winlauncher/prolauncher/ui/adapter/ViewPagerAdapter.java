package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;

import java.io.File;
import java.util.ArrayList;

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<File> fileList;

    public ViewPagerAdapter(Context context, ArrayList<File> fileList) {
        this.context = context;
        this.fileList = fileList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_viewpager, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = fileList.get(position);
        holder.bind(file, context);
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }


    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.stopPlayback();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        VideoView videoView;
        ProgressBar progressBar;
        MediaController mediaController;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            progressBar = itemView.findViewById(R.id.progressBar);
        }

        void bind(File file, Context context) {
            progressBar.setVisibility(View.VISIBLE);
            if (mediaController == null) {
                mediaController = new MediaController(context);
            }
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);
            videoView.setVideoURI(Uri.fromFile(file));

            videoView.setOnPreparedListener(mp -> {
                progressBar.setVisibility(View.GONE);
                mp.start();
            });

            videoView.setOnCompletionListener(mp -> {
                Toast.makeText(context, "Video finished", Toast.LENGTH_SHORT).show();
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(context, "Error playing video", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        void stopPlayback() {
            if (videoView.isPlaying()) {
                videoView.stopPlayback();
            }
        }
    }
}