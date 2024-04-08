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

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.session.MediaSessionLegacyHelper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.android.internal.R;
import com.android.internal.statusbar.IStatusBarService;
import com.android.server.SystemService;

public final class ShakeGestureService extends SystemService {

    private static final String TAG = "ShakeGestureService";
    
    private static final String SHAKE_GESTURES_ENABLED = "shake_gestures_enabled";
    private static final String SHAKE_GESTURES_ACTION = "shake_gestures_action";

    private final Context mContext;
    private final ShakeGestureUtils mShakeGestureUtils;
    private final AudioManager mAudioManager;
    private final CameraManager mCameraManager;
    private final PowerManager mPowerManager;
    private final Vibrator mVibrator;
    private final NotificationManager mNotifManager;
    private IStatusBarService mStatusBarService;
    private static ShakeGestureService instance;
    private ScreenshotCallback mScreenshotCallback;

    private String mCameraId;
    private boolean isFlashOn = false;
    final Object mServiceAcquireLock = new Object();

    private ShakeGestureService(Context context) {
        super(context);
        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mNotifManager = mContext.getSystemService(NotificationManager.class);
        mShakeGestureUtils = new ShakeGestureUtils(mContext);
        try {
            mCameraId = mCameraManager.getCameraIdList()[0];
        } catch (Exception e) {}
    }

    public static synchronized ShakeGestureService getInstance(Context context) {
        if (instance == null) {
            instance = new ShakeGestureService(context);
        }
        return instance;
    }

    public interface ScreenshotCallback {
        void onScreenshotTaken();
    }

    @Override
    public void onStart() {
        mShakeGestureUtils.registerListener(new ShakeGestureUtils.OnShakeListener() {
            @Override
            public void onShake() {
                if (isShakeGestureEnabled()) {
                    performShakeAction();
                }
            }
        });
    }

    IStatusBarService getStatusBarService() {
        synchronized (mServiceAcquireLock) {
            if (mStatusBarService == null) {
                mStatusBarService = IStatusBarService.Stub.asInterface(
                        ServiceManager.getService("statusbar"));
            }
            return mStatusBarService;
        }
    }

    private boolean isShakeGestureEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SHAKE_GESTURES_ENABLED, 0) == 1;
    }

    private void performShakeAction() {
        int singleShakeAction = getShakeGestureAction();
        doAction(singleShakeAction);
    }

    private int getShakeGestureAction() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SHAKE_GESTURES_ACTION, 0);
    }

    private void doAction(int gestureAction) {
        switch (gestureAction) {
            case 1:
                toggleFlashlight();
                break;
            case 2:
                dispatchMediaKeyWithWakeLockToMediaSession(
                        mAudioManager.isMusicActive() ? KeyEvent.KEYCODE_MEDIA_NEXT : KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                break;
            case 3:
                mAudioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
                break;
            case 4:
                turnScreenOnOrOff();
                break;
            case 5:
                clearAllNotifications();
                break;
            case 6:
                toggleRingerModes();
                break;
            case 7:
                if (mScreenshotCallback != null) {
                    mScreenshotCallback.onScreenshotTaken();
                }
                break;
            case 0:
            default:
                break;
        }
    }

    private void toggleFlashlight() {
        PowerManager.WakeLock wakeLock = acquireWakelock();
        try {
            synchronized (this) {
                mCameraManager.setTorchMode(mCameraId, !isFlashOn);
                isFlashOn = !isFlashOn;
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to access camera", e);
        } finally {
            releaseWakelock(wakeLock);
        }
    }

    private void dispatchMediaKeyWithWakeLockToMediaSession(final int keycode) {
        final MediaSessionLegacyHelper helper = MediaSessionLegacyHelper.getHelper(mContext);
        if (helper == null) {
            return;
        }
        KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keycode, 0);
        helper.sendMediaButtonEvent(event, true);
        event = KeyEvent.changeAction(event, KeyEvent.ACTION_UP);
        helper.sendMediaButtonEvent(event, true);
    }

    private void turnScreenOnOrOff() {
        if (mPowerManager.isInteractive()) {
            mPowerManager.goToSleep(SystemClock.uptimeMillis());
        } else {
            PowerManager.WakeLock wakeLock = acquireWakelock();
            try {
                mPowerManager.wakeUp(SystemClock.uptimeMillis());
            } finally {
                releaseWakelock(wakeLock);
            }
        }
    }
    
    private PowerManager.WakeLock acquireWakelock() {
        PowerManager.WakeLock wakeLock = mPowerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();
        return mPowerManager.isInteractive() ? null : wakeLock;
    }
    
    private void releaseWakelock(PowerManager.WakeLock wakeLock) {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void clearAllNotifications() {
        IStatusBarService statusBarService = getStatusBarService();
        if (statusBarService != null) {
            try {
                statusBarService.onClearAllNotifications(ActivityManager.getCurrentUser());
            } catch (RemoteException e) {}
        }
    }
    
    private void toggleRingerModes() {
        switch (mAudioManager.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                if (mVibrator.hasVibrator()) {
                    mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    Toast.makeText(mContext, mContext.getString(R.string.shake_gesture_toast_vibrate_mode), Toast.LENGTH_SHORT).show();
                }
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                mNotifManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                Toast.makeText(mContext, mContext.getString(R.string.shake_gesture_toast_dnd_mode), Toast.LENGTH_SHORT).show();
                break;
            case AudioManager.RINGER_MODE_SILENT:
                mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                Toast.makeText(mContext, mContext.getString(R.string.shake_gesture_toast_normal_mode), Toast.LENGTH_SHORT).show();
                break;
        }
    }
    
    public void setScreenshotCallback(ScreenshotCallback callback) {
        mScreenshotCallback = callback;
    }
}
