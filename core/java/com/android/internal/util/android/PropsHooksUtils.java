/*
 * Copyright (C) 2020 The Pixel Experience Project
 *               2021-2024 crDroid Android Project
 *               2024 RisingOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.android;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PropsHooksUtils {

    private static final String TAG = PropsHooksUtils.class.getSimpleName();
    private static final String PROP_HOOKS = "persist.sys.pihooks_";
    private static final String PROP_HOOKS_MAINLINE = "persist.sys.pihooks_mainline_";
    private static final boolean DEBUG = SystemProperties.getBoolean(PROP_HOOKS + "DEBUG", false);

    public static final String SPOOF_PIXEL_GMS = "persist.sys.pixelprops.gms";
    public static final String SPOOF_PIXEL_GPHOTOS = "persist.sys.pixelprops.gphotos";
    public static final String ENABLE_PROP_OPTIONS = "persist.sys.pixelprops.all";
    public static final String ENABLE_GAME_PROP_OPTIONS = "persist.sys.gameprops.enabled";
    public static final String SPOOF_PIXEL_GOOGLE_APPS = "persist.sys.pixelprops.google";
    public static final String SPOOF_EXTRA_PACKAGES = "persist.sys.spoof.extra";

    private static final Map<String, Object> propsToChangeMainline;
    private static final Map<String, Object> propsToChangePixelXL;
    private static final Map<String, Object> propsToChangePixel5a;

    public static final String[] DEFAULT_PACKAGES_TO_SPOOF = {
        "com.google.android.apps.aiwallpapers",
        "com.google.android.apps.bard",
        "com.google.android.apps.customization.pixel",
        "com.google.android.apps.emojiwallpaper",
        "com.google.android.apps.nexuslauncher",
        "com.google.android.apps.pixel.agent",
        "com.google.android.apps.pixel.creativeassistant",
        "com.google.android.apps.privacy.wildlife",
        "com.google.android.apps.wallpaper",
        "com.google.android.apps.wallpaper.pixel",
        "com.google.android.as",
        "com.google.android.googlequicksearchbox",
        "com.google.android.inputmethod.latin",
        "com.google.android.tts",
        "com.google.android.wallpaper.effects"
    };

    private static final String[] GMS_SPOOF_KEYS = {
        "BRAND", "DEVICE", "DEVICE_INITIAL_SDK_INT", "FINGERPRINT", "ID",
        "MANUFACTURER", "MODEL", "PRODUCT", "RELEASE", "SECURITY_PATCH",
        "TAGS", "TYPE"
    };

    static {
        propsToChangeMainline = new HashMap<>();
        propsToChangeMainline.put("BRAND", SystemProperties.get(PROP_HOOKS_MAINLINE + "BRAND"));
        propsToChangeMainline.put("BOARD", SystemProperties.get(PROP_HOOKS_MAINLINE + "BOARD"));
        propsToChangeMainline.put("MANUFACTURER", SystemProperties.get(PROP_HOOKS_MAINLINE + "MANUFACTURER"));
        propsToChangeMainline.put("DEVICE", SystemProperties.get(PROP_HOOKS_MAINLINE + "DEVICE"));
        propsToChangeMainline.put("PRODUCT", SystemProperties.get(PROP_HOOKS_MAINLINE + "PRODUCT"));
        propsToChangeMainline.put("HARDWARE", SystemProperties.get(PROP_HOOKS_MAINLINE + "HARDWARE"));
        propsToChangeMainline.put("ID", SystemProperties.get(PROP_HOOKS_MAINLINE + "ID"));
        propsToChangeMainline.put("MODEL", SystemProperties.get(PROP_HOOKS_MAINLINE + "MODEL"));
        propsToChangeMainline.put("FINGERPRINT", SystemProperties.get(PROP_HOOKS_MAINLINE + "FINGERPRINT"));
        propsToChangePixelXL = new HashMap<>();
        propsToChangePixelXL.put("BRAND", "google");
        propsToChangePixelXL.put("MANUFACTURER", "Google");
        propsToChangePixelXL.put("DEVICE", "marlin");
        propsToChangePixelXL.put("PRODUCT", "marlin");
        propsToChangePixelXL.put("HARDWARE", "marlin");
        propsToChangePixelXL.put("ID", "QP1A.191005.007.A3");
        propsToChangePixelXL.put("MODEL", "Pixel XL");
        propsToChangePixelXL.put("FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");
        propsToChangePixel5a = new HashMap<>();
        propsToChangePixel5a.put("BRAND", "google");
        propsToChangePixel5a.put("MANUFACTURER", "Google");
        propsToChangePixel5a.put("DEVICE", "barbet");
        propsToChangePixel5a.put("PRODUCT", "barbet");
        propsToChangePixel5a.put("HARDWARE", "barbet");
        propsToChangePixel5a.put("MODEL", "Pixel 5a");
        propsToChangePixel5a.put("ID", "AP2A.240805.005");
        propsToChangePixel5a.put("FINGERPRINT", "google/barbet/barbet:14/AP2A.240805.005/12025142:user/release-keys");
    }

    public static void setProps(Context context) {
        String packageName = context.getPackageName();

        if (packageName == null || packageName.isEmpty()) {
            return;
        }

        setGameProps(packageName);

        if (!SystemProperties.getBoolean(ENABLE_PROP_OPTIONS, true)) {
            return;
        }

        boolean isPixelDevice = SystemProperties.get("ro.soc.manufacturer").equalsIgnoreCase("Google");
        String model = SystemProperties.get("ro.product.model");
        boolean isMainlineDevice = isPixelDevice && model.matches("Pixel [8-9][a-zA-Z ]*");
        boolean isTensorDevice = isPixelDevice && model.matches("Pixel [6-9][a-zA-Z ]*");

        Map<String, Object> propsToChange = new HashMap<>();

        final String processName = Application.getProcessName();

        Set<String> packagesToSpoof = getPackagesToSpoof();
        if (packagesToSpoof.contains(packageName)) {
            if (SystemProperties.getBoolean(SPOOF_PIXEL_GOOGLE_APPS, true)) {
                propsToChange.putAll(propsToChangeMainline);
            }
        }

        if (packageName.equals("com.google.android.apps.photos")) {
            if (SystemProperties.getBoolean(SPOOF_PIXEL_GPHOTOS, true)) {
                propsToChange.putAll(propsToChangePixelXL);
            } else {
                if (!isMainlineDevice) {
                    propsToChange.putAll(propsToChangeMainline);
                }
            }
        }

        if (packageName.equals("com.snapchat.android")) {
            propsToChange.putAll(propsToChangePixelXL);
        }

        if (packageName.equals("com.google.android.settings.intelligence")) {
            setPropValue("FINGERPRINT", "eng.nobody." +
                new java.text.SimpleDateFormat("yyyyMMdd.HHmmss").format(new java.util.Date()));
        }

        if (packageName.equals("com.google.android.gms")) {
            setPropValue("TIME", System.currentTimeMillis());
            if (processName != null && processName.toLowerCase().contains("unstable")
                && SystemProperties.getBoolean(SPOOF_PIXEL_GMS, true)) {
                spoofBuildGms();
            }
            if (!isMainlineDevice && (processName == null || !processName.toLowerCase().contains("unstable"))) {
                propsToChange.putAll(propsToChangeMainline);
            }
        }

        if (!propsToChange.isEmpty()) {
            if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
            for (Map.Entry<String, Object> prop : propsToChange.entrySet()) {
                String key = prop.getKey();
                Object value = prop.getValue();
                if (DEBUG) Log.d(TAG, "Defining " + key + " prop for: " + packageName);
                setPropValue(key, value);
            }
        }
    }

    public static void setGameProps(String packageName) {
        if (!SystemProperties.getBoolean(ENABLE_GAME_PROP_OPTIONS, false)) {
            return;
        }
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        Map<String, String> gamePropsToChange = new HashMap<>();
        String[] keys = {"BRAND", "DEVICE", "MANUFACTURER", "MODEL", "FINGERPRINT", "PRODUCT"};
        for (String key : keys) {
            String systemPropertyKey = "persist.sys.gameprops." + packageName + "." + key;
            String value = SystemProperties.get(systemPropertyKey);
            if (value != null && !value.isEmpty()) {
                gamePropsToChange.put(key, value);
                if (DEBUG) Log.d(TAG, "Got system property: " + systemPropertyKey + " = " + value);
            }
        }
        if (!gamePropsToChange.isEmpty()) {
            if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
            for (Map.Entry<String, String> prop : gamePropsToChange.entrySet()) {
                String key = prop.getKey();
                String value = prop.getValue();
                if (DEBUG) Log.d(TAG, "Defining " + key + " prop for: " + packageName);
                setPropValue(key, value);
            }
        }
    }

    public static Set<String> getPackagesToSpoof() {
        Set<String> packages = new HashSet<>(Arrays.asList(DEFAULT_PACKAGES_TO_SPOOF));
        String extraPackages = SystemProperties.get(SPOOF_EXTRA_PACKAGES, "");
        if (!extraPackages.isEmpty()) {
            packages.addAll(Arrays.asList(extraPackages.split(",")));
        }
        if (DEBUG) {
            Log.d(TAG, "Packages to spoof: " + packages);
        }
        return packages;
    }

    public static void addExtraPackage(String packageName) {
        String extraPackages = SystemProperties.get(SPOOF_EXTRA_PACKAGES, "");
        Set<String> packages = new HashSet<>();
        if (!extraPackages.isEmpty()) {
            packages.addAll(Arrays.asList(extraPackages.split(",")));
        }
        if (packages.add(packageName)) {
            SystemProperties.set(SPOOF_EXTRA_PACKAGES, String.join(",", packages));
        }
    }

    public static void removeExtraPackage(String packageName) {
        String extraPackages = SystemProperties.get(SPOOF_EXTRA_PACKAGES, "");
        Set<String> packages = new HashSet<>();
        if (!extraPackages.isEmpty()) {
            packages.addAll(Arrays.asList(extraPackages.split(",")));
        }
        if (packages.remove(packageName)) {
            SystemProperties.set(SPOOF_EXTRA_PACKAGES, String.join(",", packages));
        }
    }

    private static void setPropValue(String key, Object value) {
        try {
            Field field = getBuildClassField(key);
            if (field != null) {
                field.setAccessible(true);
                if (field.getType() == int.class) {
                    if (value instanceof String) {
                        field.set(null, Integer.parseInt((String) value));
                    } else if (value instanceof Integer) {
                        field.set(null, (Integer) value);
                    }
                } else if (field.getType() == long.class) {
                    if (value instanceof String) {
                        field.set(null, Long.parseLong((String) value));
                    } else if (value instanceof Long) {
                        field.set(null, (Long) value);
                    }
                } else {
                    field.set(null, value.toString());
                }
                field.setAccessible(false);
                dlog("Set prop " + key + " to " + value);
            } else {
                Log.e(TAG, "Field " + key + " not found in Build or Build.VERSION classes");
            }
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static Field getBuildClassField(String key) throws NoSuchFieldException {
        try {
            Field field = Build.class.getDeclaredField(key);
            dlog("Field " + key + " found in Build.class");
            return field;
        } catch (NoSuchFieldException e) {
            Field field = Build.VERSION.class.getDeclaredField(key);
            dlog("Field " + key + " found in Build.VERSION.class");
            return field;
        }
    }

    private static void spoofBuildGms() {
        for (String key : GMS_SPOOF_KEYS) {
            setPropValue(key, SystemProperties.get(PROP_HOOKS + key));
        }
    }

    private static boolean isCallerSafetyNet() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                        .anyMatch(elem -> elem.getClassName().toLowerCase()
                            .contains("droidguard"));
    }

    public static void onEngineGetCertificateChain() {
        if (!SystemProperties.getBoolean(SPOOF_PIXEL_GMS, true)) return;
        // Check stack for SafetyNet or Play Integrity
        if (isCallerSafetyNet()) {
            Log.i(TAG, "Blocked key attestation");
            throw new UnsupportedOperationException();
        }
    }

    private static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}
