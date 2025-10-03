package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.AudioItem;
import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class AudioPlayerPagerAdapter extends RecyclerView.Adapter<AudioPlayerPagerAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<AudioItem> audioItems;
    private final AudioControlsListener controlsListener;
    private final Handler seekBarHandler = new Handler(Looper.getMainLooper());
    private int currentPlayingPosition = -1;
    private MediaPlayer mediaPlayer;
    private Runnable updateSeekBarRunnable;

    public AudioPlayerPagerAdapter(Context context, ArrayList<AudioItem> audioItems, AudioControlsListener listener) {
        this.context = context;
        this.audioItems = audioItems;
        this.controlsListener = listener;
        initMediaPlayer();
    }

    public static String formatDuration(long duration) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%d:%02d", minutes, seconds);
    }

    private void initMediaPlayer() {
        if (mediaPlayer != null) { // Ensure existing player is released before new one is created
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build());

        mediaPlayer.setOnCompletionListener(mp -> {
            if (controlsListener != null) {
                controlsListener.onNextClicked();
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_audioplayer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AudioItem item = audioItems.get(position);
        holder.bind(item);
        if (position == currentPlayingPosition && mediaPlayer != null) { // Added null check for mediaPlayer
            holder.updatePlayPauseIcon(mediaPlayer.isPlaying());
            startSeekBarUpdates(holder);
        } else {
            holder.resetPlayerUI();
        }
    }

    @Override
    public int getItemCount() {
        return audioItems.size();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        seekBarHandler.removeCallbacksAndMessages(null);
        holder.resetPlayerUI();
    }

    public void playSong(int position) {
        if (position < 0 || position >= audioItems.size()) return;
        if (mediaPlayer == null) { // Re-initialize if null
            initMediaPlayer();
        } else {
            mediaPlayer.reset();
        }
        currentPlayingPosition = position;

        try {
            AudioItem item = audioItems.get(position);
            mediaPlayer.setDataSource(context, item.contentUri);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                notifyItemChanged(position);
                for (int i = 0; i < getItemCount(); i++) {
                    if (i != position) notifyItemChanged(i);
                }
            });

        } catch (IOException e) {
            Toast.makeText(context, "Error playing file", Toast.LENGTH_SHORT).show();
            // In case of error, reset currentPlayingPosition and clear player
            currentPlayingPosition = -1;
            releasePlayer(); // Release player if it fails to prepare
            notifyDataSetChanged(); // Refresh UI to reflect no song playing
        } catch (IllegalStateException e) {
            // This can happen if setDataSource is called in an invalid state.
            // Reset and try again, or just log and notify.
            Toast.makeText(context, "Player error. Trying again.", Toast.LENGTH_SHORT).show();
            releasePlayer();
            playSong(position); // Attempt to re-play
        }
    }

    public void pauseSong() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            notifyItemChanged(currentPlayingPosition);
        }
    }

    public void resumeSong() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            notifyItemChanged(currentPlayingPosition);
        }
    }

    public void releasePlayer() {
        seekBarHandler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            currentPlayingPosition = -1; // Reset current playing position
            notifyDataSetChanged(); // Update all view holders
        }
    }

    private void startSeekBarUpdates(ViewHolder holder) {
        seekBarHandler.removeCallbacksAndMessages(null);

        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                // Added null check for mediaPlayer
                if (mediaPlayer != null && holder.getAdapterPosition() == currentPlayingPosition) {
                    try {
                        int currentPos = mediaPlayer.getCurrentPosition();
                        int duration = mediaPlayer.getDuration();

                        holder.seekBar.setMax(duration);
                        holder.seekBar.setProgress(currentPos);
                        holder.tvCurrentTime.setText(formatDuration(currentPos));
                        holder.tvTotalTime.setText(formatDuration(duration));

                        seekBarHandler.postDelayed(this, 1000);
                    } catch (IllegalStateException e) {
                        // MediaPlayer might be in an invalid state (e.g., released or not prepared)
                        // Log the error and stop updates
                        e.printStackTrace();
                        seekBarHandler.removeCallbacks(this);
                    }
                }
            }
        };
        seekBarHandler.post(updateSeekBarRunnable);
    }

    public interface AudioControlsListener {
        void onNextClicked();

        void onPreviousClicked();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlbumArt, ivPlayPause;
        TextView tvSongTitle, tvSongArtist, tvCurrentTime, tvTotalTime;
        SeekBar seekBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlbumArt = itemView.findViewById(R.id.ivAlbumArt);
            ivPlayPause = itemView.findViewById(R.id.ivPlayPause);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvSongArtist = itemView.findViewById(R.id.tvSongArtist);
            tvCurrentTime = itemView.findViewById(R.id.tvCurrentTime);
            tvTotalTime = itemView.findViewById(R.id.tvTotalTime);
            seekBar = itemView.findViewById(R.id.seekBar);
            ivPlayPause.setOnClickListener(v -> togglePlayState());
            itemView.findViewById(R.id.ivNext).setOnClickListener(v -> {
                if (controlsListener != null) controlsListener.onNextClicked();
            });
            itemView.findViewById(R.id.ivPrevious).setOnClickListener(v -> {
                if (controlsListener != null) controlsListener.onPreviousClicked();
            });

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && mediaPlayer != null) { // Added null check for mediaPlayer
                        try {
                            mediaPlayer.seekTo(progress);
                        } catch (IllegalStateException e) {
                            // MediaPlayer might be in an invalid state
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }

        void bind(AudioItem item) {
            tvSongTitle.setText(item.file.getName().replaceFirst("[.][^.]+$", ""));

            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                // Ensure contentUri is valid before setting data source
                if (item.contentUri != null && Uri.EMPTY != item.contentUri) {
                    mmr.setDataSource(context, item.contentUri);
                } else {
                    throw new IllegalArgumentException("Content URI is null or empty");
                }

                String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                tvSongArtist.setText(artist != null ? artist : "Unknown Artist");

                byte[] art = mmr.getEmbeddedPicture();
                if (art != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                    Glide.with(context).load(bitmap).error(R.drawable.ic_audio).into(ivAlbumArt);
                } else {
                    ivAlbumArt.setImageResource(R.drawable.ic_audio);
                }
            } catch (Exception e) {
                tvSongArtist.setText("Unknown Artist");
                ivAlbumArt.setImageResource(R.drawable.ic_audio);
                e.printStackTrace(); // Log the exception for debugging
            } finally {
                try {
                    mmr.release();
                } catch (Exception e) {
                    e.printStackTrace(); // Log the exception for debugging
                }
            }
            seekBar.setProgress(0);
            tvCurrentTime.setText("0:00");
            tvTotalTime.setText("0:00");
            // Added null check for mediaPlayer
            if (getAdapterPosition() == currentPlayingPosition && mediaPlayer != null) {
                updatePlayPauseIcon(mediaPlayer.isPlaying());
            } else {
                ivPlayPause.setImageResource(R.drawable.ic_play);
            }
        }

        void togglePlayState() {
            // Crucial null check here
            if (mediaPlayer == null) {
                Toast.makeText(context, "Player not ready. Please try again.", Toast.LENGTH_SHORT).show();
                // Optionally re-initialize and try to play
                initMediaPlayer();
                playSong(getAdapterPosition());
                return;
            }

            int position = getAdapterPosition();
            if (position == currentPlayingPosition) {
                if (mediaPlayer.isPlaying()) {
                    pauseSong();
                } else {
                    resumeSong();
                }
            } else {
                playSong(position);
            }
        }

        void updatePlayPauseIcon(boolean isPlaying) {
            ivPlayPause.setImageResource(
                    isPlaying ? R.drawable.ic_pause : R.drawable.ic_play
            );
        }

        void resetPlayerUI() {
            seekBar.setProgress(0);
            tvCurrentTime.setText("0:00");
            tvTotalTime.setText("0:00");
            ivPlayPause.setImageResource(R.drawable.ic_play);
        }
    }
}