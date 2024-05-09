/*
 * Copyright (C) 2023-2024 The RisingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.blackiron.server;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.session.MediaSessionLegacyHelper;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

import com.android.internal.R;
import com.android.server.SystemService;

import java.util.Arrays;

public final class ShakeGestureService extends SystemService {

    private static final String TAG = "ShakeGestureService";

    private static final String SHAKE_GESTURES_ENABLED = "shake_gestures_enabled";
    private static final String SHAKE_GESTURES_ACTION = "shake_gestures_action";
    private static final int USER_ALL = UserHandle.USER_ALL;

    private final Context mContext;
    private final ShakeGestureUtils mShakeGestureUtils;
    private final AudioManager mAudioManager;
    private final PowerManager mPowerManager;
    private static ShakeGestureService instance;
    private ShakeGesturesCallbacks mShakeCallbacks;

    private final SettingsObserver mSettingsObserver;
    private boolean mShakeServiceEnabled = false;
    private int mShakeGestureAction = 0;

    private PowerManager.WakeLock mWakeLock = null;

    private ShakeGestureService(Context context) {
        super(context);
        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mShakeGestureUtils = new ShakeGestureUtils(mContext);
        mSettingsObserver = new SettingsObserver(new Handler());
        updateSettings();
    }

    public static synchronized ShakeGestureService getInstance(Context context) {
        if (instance == null) {
            instance = new ShakeGestureService(context);
        }
        return instance;
    }

    public interface ShakeGesturesCallbacks {
        void onClearAllNotifications();
        void onMediaKeyDispatch();
        void onScreenshotTaken();
        void onToggleRingerModes();
        void onToggleTorch();
        void onToggleVolumePanel();
        void onKillApp();
    }

    @Override
    public void onStart() {
        mSettingsObserver.observe();
        mShakeGestureUtils.registerListener(new ShakeGestureUtils.OnShakeListener() {
            @Override
            public void onShake() {
                if (mShakeServiceEnabled) {
                    doAction();
                }
            }
        });
    }

    private void updateSettings() {
        mShakeServiceEnabled = Settings.System.getInt(mContext.getContentResolver(),
                SHAKE_GESTURES_ENABLED, 0) == 1;
        mShakeGestureAction = Settings.System.getInt(mContext.getContentResolver(),
                SHAKE_GESTURES_ACTION, 0);
    }

    private void doAction() {
        int[] wakelockActions = {1, 4, 6};
        boolean actionNeedsWakelock = Arrays.asList(wakelockActions).contains(mShakeGestureAction);
        if (actionNeedsWakelock) {
            setUsesWakelock();
        }
        try {
            doAction(mShakeGestureAction);
        } catch (Exception e) {
        } finally {
            releaseWakelock();
        }
    }

    private void doAction(int gestureAction) {
        if (mShakeCallbacks == null) return;
        switch (gestureAction) {
            case 1:
                mShakeCallbacks.onToggleTorch();
                break;
            case 2:
                mShakeCallbacks.onMediaKeyDispatch();
                break;
            case 3:
                mShakeCallbacks.onToggleVolumePanel();
                break;
            case 4:
                turnScreenOnOrOff();
                break;
            case 5:
                mShakeCallbacks.onClearAllNotifications();
                break;
            case 6:
                mShakeCallbacks.onToggleRingerModes();
                break;
            case 7:
                mShakeCallbacks.onScreenshotTaken();
                break;
            case 8:
                mShakeCallbacks.onKillApp();
                break;
            case 0:
            default:
                break;
        }
    }

    private void setUsesWakelock() {
        if (mWakeLock == null) {
            mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            mWakeLock.acquire();
        }
    }

    private void turnScreenOnOrOff() {
        if (mPowerManager.isInteractive()) {
            mPowerManager.goToSleep(SystemClock.uptimeMillis());
        } else {
            mPowerManager.wakeUp(SystemClock.uptimeMillis());
        }
    }

    private void releaseWakelock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    public void setShakeCallbacks(ShakeGesturesCallbacks callback) {
        mShakeCallbacks = callback;
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }
        void observe() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(SHAKE_GESTURES_ENABLED), false, this, USER_ALL);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(SHAKE_GESTURES_ACTION), false, this, USER_ALL);
        }
        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }
}
