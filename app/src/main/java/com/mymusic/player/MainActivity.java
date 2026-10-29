package com.mymusic.player;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(9, 10, 18);
    private final int CARD = Color.rgb(22, 23, 39);
    private final int PURPLE = Color.rgb(139, 92, 246);
    private final int CYAN = Color.rgb(34, 211, 238);
    private final int WHITE = Color.rgb(245, 245, 255);
    private final int MUTED = Color.rgb(160, 163, 185);

    private MediaPlayer player;
    private Equalizer equalizer;
    private BassBoost bassBoost;
    private AudioManager audioManager;
    private final ArrayList<Uri> tracks = new ArrayList<>();
    private final ArrayList<String> trackNames = new ArrayList<>();
    private int currentIndex = -1;
    private boolean shuffle = false;
    private boolean repeat = false;
    private boolean userSeeking = false;
    private SeekBar seekBar;
    private TextView titleText, artistText, timeText, durationText, outputText, playButton;
    private TextView shuffleButton, repeatButton, bassValue;
    private SeekBar bassSlider;
    private LinearLayout eqContainer;
    private final Handler handler = new Handler();
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (player != null) {
                try {
                    if (player.isPlaying() && !userSeeking) seekBar.setProgress(player.getCurrentPosition());
                    timeText.setText(formatTime(player.getCurrentPosition()));
                } catch (IllegalStateException ignored) {}
            }
            refreshOutput();
            handler.postDelayed(this, 400);
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        buildUi();
        handler.post(ticker);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private void pad(View v, int l, int t, int r, int b) {
        v.setPadding(dp(l), dp(t), dp(r), dp(b));
    }

    private TextView button(String label, int size) {
        TextView b = text(label, size, WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(roundBg(CARD, 18));
        pad(b, 10, 10, 10, 10);
        return b;
    }

    private android.graphics.drawable.GradientDrawable roundBg(int color, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }

    private void buildUi() {
        LinearLayout root = column();
        root.setBackgroundColor(BG);
        pad(root, 20, 12, 20, 14);

        LinearLayout header = row();
        TextView logo = text("♫", 30, CYAN, true);
        header.addView(logo, lp(42, 48));
        LinearLayout brand = column();
        brand.addView(text("MyMusic", 23, WHITE, true));
        brand.addView(text("YOUR SOUND. YOUR WORLD.", 9, MUTED, true));
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView add = button("+", 26);
        add.setTextColor(CYAN);
        header.addView(add, lp(48, 48));
        add.setOnClickListener(v -> pickTracks());
        root.addView(header);
        root.addView(space(10));

        TextView section = text("NOW PLAYING", 11, CYAN, true);
        root.addView(section);
        root.addView(space(10));

        TextView art = text("♫", 92, WHITE, true);
        art.setGravity(Gravity.CENTER);
        art.setBackground(roundBg(Color.rgb(44, 26, 83), 28));
        art.setElevation(dp(5));
        LinearLayout.LayoutParams artLp = lp(-1, 220);
        root.addView(art, artLp);
        root.addView(space(18));

        titleText = text("Choose your music", 23, WHITE, true);
        titleText.setGravity(Gravity.CENTER);
        artistText = text("Select audio files from your phone", 13, MUTED, false);
        artistText.setGravity(Gravity.CENTER);
        root.addView(titleText);
        root.addView(artistText);
        root.addView(space(15));

        seekBar = new SeekBar(this);
        seekBar.setMax(1000);
        seekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        seekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(CYAN));
        root.addView(seekBar, lp(-1, 28));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser && player != null && player.getDuration() > 0) {
                    timeText.setText(formatTime((int) ((long) p * player.getDuration() / 1000)));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) { userSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar s) {
                if (player != null && player.getDuration() > 0) player.seekTo((int) ((long) s.getProgress() * player.getDuration() / 1000));
                userSeeking = false;
            }
        });
        LinearLayout times = row();
        timeText = text("00:00", 11, MUTED, false);
        durationText = text("00:00", 11, MUTED, false);
        times.addView(timeText, new LinearLayout.LayoutParams(0, -2, 1));
        durationText.setGravity(Gravity.RIGHT);
        times.addView(durationText);
        root.addView(times);
        root.addView(space(12));

        LinearLayout controls = row();
        controls.setGravity(Gravity.CENTER);
        shuffleButton = button("⤨", 21);
        TextView prev = button("◀|", 19);
        playButton = button("▶", 26);
        playButton.setBackground(roundBg(PURPLE, 50));
        TextView next = button("|▶", 19);
        repeatButton = button("↻", 21);
        addControl(controls, shuffleButton, 48);
        addControl(controls, prev, 50);
        addControl(controls, playButton, 64);
        addControl(controls, next, 50);
        addControl(controls, repeatButton, 48);
        root.addView(controls);
        shuffleButton.setOnClickListener(v -> {
            shuffle = !shuffle;
            shuffleButton.setTextColor(shuffle ? CYAN : WHITE);
            toast(shuffle ? "Shuffle on" : "Shuffle off");
        });
        repeatButton.setOnClickListener(v -> {
            repeat = !repeat;
            repeatButton.setTextColor(repeat ? CYAN : WHITE);
            toast(repeat ? "Repeat current track" : "Repeat off");
        });
        prev.setOnClickListener(v -> previousTrack());
        next.setOnClickListener(v -> nextTrack());
        playButton.setOnClickListener(v -> togglePlayback());
        root.addView(space(18));

        LinearLayout eqCard = column();
        eqCard.setBackground(roundBg(CARD, 20));
        pad(eqCard, 16, 14, 16, 14);
        LinearLayout eqHeader = row();
        eqHeader.addView(text("♫  Equalizer", 17, WHITE, true), new LinearLayout.LayoutParams(0, -2, 1));
        eqHeader.addView(text("5 BAND", 10, CYAN, true));
        eqCard.addView(eqHeader);
        eqCard.addView(space(8));
        eqContainer = row();
        eqContainer.setGravity(Gravity.CENTER);
        eqCard.addView(eqContainer, lp(-1, 94));
        root.addView(eqCard);
        root.addView(space(12));

        LinearLayout bassCard = column();
        bassCard.setBackground(roundBg(CARD, 20));
        pad(bassCard, 16, 12, 16, 12);
        LinearLayout bassHeader = row();
        bassHeader.addView(text("Bass Boost", 16, WHITE, true), new LinearLayout.LayoutParams(0, -2, 1));
        bassValue = text("0%", 12, CYAN, true);
        bassHeader.addView(bassValue);
        bassCard.addView(bassHeader);
        bassSlider = new SeekBar(this);
        bassSlider.setMax(1000);
        bassSlider.setProgressTintList(android.content.res.ColorStateList.valueOf(CYAN));
        bassSlider.setThumbTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        bassCard.addView(bassSlider);
        bassSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                bassValue.setText((p / 10) + "%");
                if (bassBoost != null) {
                    try { bassBoost.setStrength((short) (p * 10)); } catch (Exception ignored) {}
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(bassCard);
        root.addView(space(10));

        outputText = text("◉  Audio output: Phone speaker", 12, MUTED, true);
        outputText.setGravity(Gravity.CENTER);
        root.addView(outputText);
        root.addView(space(8));
        TextView library = button("＋   Add music from device", 14);
        library.setTextColor(CYAN);
        root.addView(library, lp(-1, 46));
        library.setOnClickListener(v -> pickTracks());

        setContentView(root);
        initEqualizerUi();
    }

    private View space(int height) {
        View v = new View(this);
        v.setLayoutParams(lp(1, height));
        return v;
    }

    private void addControl(LinearLayout parent, TextView child, int width) {
        LinearLayout.LayoutParams p = lp(width, 52);
        p.setMargins(dp(3), 0, dp(3), 0);
        parent.addView(child, p);
    }

    private void pickTracks() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, 101);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 101 || resultCode != RESULT_OK || data == null) return;
        tracks.clear();
        trackNames.clear();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                tracks.add(uri);
                trackNames.add(getName(uri));
            }
        } else if (data.getData() != null) {
            tracks.add(data.getData());
            trackNames.add(getName(data.getData()));
        }
        if (!tracks.isEmpty()) playTrack(0);
    }

    private String getName(Uri uri) {
        String name = "Unknown track";
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception ignored) {}
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        return name;
    }

    private void playTrack(int index) {
        if (tracks.isEmpty()) return;
        currentIndex = Math.max(0, Math.min(index, tracks.size() - 1));
        releaseEffects();
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            player.release();
        }
        player = new MediaPlayer();
        try {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setDataSource(this, tracks.get(currentIndex));
            player.setOnPreparedListener(mp -> {
                mp.start();
                titleText.setText(trackNames.get(currentIndex));
                artistText.setText("MyMusic • Local audio");
                durationText.setText(formatTime(mp.getDuration()));
                seekBar.setProgress(0);
                seekBar.setMax(1000);
                playButton.setText("Ⅱ");
                attachEffects(mp.getAudioSessionId());
                refreshOutput();
            });
            player.setOnCompletionListener(mp -> {
                if (repeat) playTrack(currentIndex);
                else if (currentIndex < tracks.size() - 1 || shuffle) nextTrack();
                else {
                    playButton.setText("▶");
                    seekBar.setProgress(1000);
                }
            });
            player.setOnErrorListener((mp, what, extra) -> {
                toast("Unable to play this audio file");
                return true;
            });
            player.prepareAsync();
        } catch (IOException | IllegalArgumentException e) {
            toast("Could not open this audio file");
        }
    }

    private void attachEffects(int sessionId) {
        try {
            equalizer = new Equalizer(0, sessionId);
            equalizer.setEnabled(true);
            buildBands();
        } catch (Exception e) {
            equalizer = null;
            toast("Equalizer not supported by this device");
        }
        try {
            bassBoost = new BassBoost(0, sessionId);
            bassBoost.setEnabled(true);
            if (bassSlider != null) bassBoost.setStrength((short) (bassSlider.getProgress() * 10));
        } catch (Exception ignored) { bassBoost = null; }
    }

    private void initEqualizerUi() {
        if (eqContainer == null) return;
        eqContainer.removeAllViews();
        String[] labels = {"60", "230", "910", "3.6k", "14k"};
        for (int i = 0; i < 5; i++) {
            final int bandIndex = i;
            LinearLayout bandColumn = column();
            bandColumn.setGravity(Gravity.CENTER);
            SeekBar bar = new SeekBar(this);
            bar.setMax(100);
            bar.setProgress(50);
            bar.setRotation(-90f);
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));
            bar.setThumbTintList(android.content.res.ColorStateList.valueOf(CYAN));
            bandColumn.addView(bar, lp(70, 32));
            TextView label = text(labels[i], 9, MUTED, false);
            label.setGravity(Gravity.CENTER);
            bandColumn.addView(label);
            eqContainer.addView(bandColumn, new LinearLayout.LayoutParams(0, -1, 1));
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                    if (equalizer != null && fromUser) {
                        try {
                            short band = (short) Math.min(equalizer.getNumberOfBands() - 1, bandIndex);
                            short min = equalizer.getBandLevelRange()[0];
                            short max = equalizer.getBandLevelRange()[1];
                            equalizer.setBandLevel(band, (short) (min + (max - min) * p / 100));
                        } catch (Exception ignored) {}
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar s) {}
                @Override public void onStopTrackingTouch(SeekBar s) {}
            });
        }
    }

    private void buildBands() {
        // Keep the five-band controls in place; the selected band is applied when adjusted.
    }

    private void togglePlayback() {
        if (player == null) {
            if (tracks.isEmpty()) pickTracks();
            else playTrack(Math.max(currentIndex, 0));
            return;
        }
        try {
            if (player.isPlaying()) {
                player.pause();
                playButton.setText("▶");
            } else {
                player.start();
                playButton.setText("Ⅱ");
            }
        } catch (IllegalStateException e) { pickTracks(); }
    }

    private void previousTrack() {
        if (player != null) {
            try {
                if (player.getCurrentPosition() > 3000) {
                    player.seekTo(0);
                    return;
                }
            } catch (Exception ignored) {}
        }
        if (tracks.isEmpty()) { pickTracks(); return; }
        int idx = shuffle ? new Random().nextInt(tracks.size()) : (currentIndex - 1 + tracks.size()) % tracks.size();
        playTrack(idx);
    }

    private void nextTrack() {
        if (tracks.isEmpty()) { pickTracks(); return; }
        int idx = shuffle ? new Random().nextInt(tracks.size()) : (currentIndex + 1) % tracks.size();
        playTrack(idx);
    }

    private void refreshOutput() {
        if (outputText == null || audioManager == null) return;
        boolean bt = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                int type = device.getType();
                if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    (Build.VERSION.SDK_INT >= 31 && (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER))) {
                    bt = true;
                    break;
                }
            }
        }
        outputText.setText(bt ? "◉  Audio output: Bluetooth connected" : "◉  Audio output: Phone / selected device");
        outputText.setTextColor(bt ? CYAN : MUTED);
    }

    private String formatTime(int millis) {
        int sec = Math.max(0, millis / 1000);
        return String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60);
    }

    private void releaseEffects() {
        try { if (equalizer != null) equalizer.release(); } catch (Exception ignored) {}
        try { if (bassBoost != null) bassBoost.release(); } catch (Exception ignored) {}
        equalizer = null;
        bassBoost = null;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker);
        releaseEffects();
        if (player != null) {
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
        super.onDestroy();
    }
}
