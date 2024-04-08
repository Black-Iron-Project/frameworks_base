package com.android.systemui.volume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.android.systemui.res.R;
import com.android.systemui.util.ArcProgressWidget;

public class VolumeProgressImageView extends ImageView {

    private Context mContext;
    private int mVolumePercent;

    public VolumeProgressImageView(Context context) {
        super(context);
        mContext = context;
    }

    public VolumeProgressImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public VolumeProgressImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        init();
    }

    private void init() {
        updateVolumePercent();
        registerVolumeReceiver();
    }

    private void registerVolumeReceiver() {
        mContext.registerReceiver(volumeReceiver, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
    }

    private void unregisterVolumeReceiver() {
        mContext.unregisterReceiver(volumeReceiver);
    }

    private void updateVolumePercent() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mVolumePercent = (currentVolume * 100) / maxVolume;
        updateImageView();
    }

    private void updateImageView() {
        Bitmap widgetBitmap = ArcProgressWidget.generateBitmap(
                mContext,
                mVolumePercent,
                String.valueOf(mVolumePercent) + "%",
                40,
                ContextCompat.getDrawable(mContext, R.drawable.ic_volume_up),
                36
        );
        setImageBitmap(widgetBitmap);
    }

    private final BroadcastReceiver volumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) {
                updateVolumePercent();
            }
        }
    };
}

