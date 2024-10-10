/*
 * Copyright (C) 2024 the risingOS Android Project
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

import android.app.ActivityThread;
import android.os.SystemProperties;
import android.util.ArrayMap;

import java.util.HashSet;
import java.util.Set;

/**
 * @hide
 */
public class FeatureHooksUtils {

    private static final Set<String> featuresPixel = new HashSet<>(Set.of(
            "com.google.android.apps.photos.PIXEL_2019_PRELOAD",
            "com.google.android.apps.photos.PIXEL_2019_MIDYEAR_PRELOAD",
            "com.google.android.apps.photos.PIXEL_2018_PRELOAD",
            "com.google.android.apps.photos.PIXEL_2017_PRELOAD",
            "com.google.android.feature.PIXEL_2021_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2020_EXPERIENCE",
            "com.google.android.feature.PIXEL_2020_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2019_EXPERIENCE",
            "com.google.android.feature.PIXEL_2019_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2018_EXPERIENCE",
            "com.google.android.feature.PIXEL_2017_EXPERIENCE",
            "com.google.android.feature.PIXEL_EXPERIENCE",
            "com.google.android.feature.GOOGLE_BUILD",
            "com.google.android.feature.GOOGLE_EXPERIENCE"
    ));

    private static final Set<String> featuresPixelOthers = new HashSet<>(Set.of(
            "com.google.android.feature.ASI",
            "com.google.android.feature.ANDROID_ONE_EXPERIENCE",
            "com.google.android.feature.GOOGLE_FI_BUNDLED",
            "com.google.android.feature.LILY_EXPERIENCE",
            "com.google.android.feature.TURBO_PRELOAD",
            "com.google.android.feature.WELLBEING",
            "com.google.lens.feature.IMAGE_INTEGRATION",
            "com.google.lens.feature.CAMERA_INTEGRATION",
            "com.google.photos.trust_debug_certs",
            "com.google.android.feature.AER_OPTIMIZED",
            "com.google.android.feature.NEXT_GENERATION_ASSISTANT",
            "android.software.game_service",
            "com.google.android.feature.EXCHANGE_6_2",
            "com.google.android.apps.dialer.call_recording_audio",
            "com.google.android.apps.dialer.SUPPORTED"
    ));

    private static final Set<String> featuresTensor = new HashSet<>(Set.of(
            "com.google.android.feature.PIXEL_2025_EXPERIENCE",
            "com.google.android.feature.PIXEL_2025_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2024_EXPERIENCE",
            "com.google.android.feature.PIXEL_2024_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2023_EXPERIENCE",
            "com.google.android.feature.PIXEL_2023_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2022_EXPERIENCE",
            "com.google.android.feature.PIXEL_2022_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2021_EXPERIENCE"
    ));

    private static final Set<String> featuresNexus = new HashSet<>(Set.of(
            "com.google.android.apps.photos.NEXUS_PRELOAD",
            "com.google.android.apps.photos.nexus_preload",
            "com.google.android.feature.PIXEL_EXPERIENCE",
            "com.google.android.feature.GOOGLE_BUILD",
            "com.google.android.feature.GOOGLE_EXPERIENCE"
    ));

    private static final Set<String> pixelPackages = new HashSet<>(Set.of(
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.nexuslauncher",
            "com.google.android.dialer",
            "com.google.android.apps.pixel.agent",
            "com.google.android.apps.pixel.creativeassistant"
    ));

    public static boolean hasSystemFeature(String name, int version, boolean hasSystemFeature) {
        if (SystemProperties.getBoolean(PropsHooksUtils.ENABLE_PROP_OPTIONS, true)) {
            String packageName = ActivityThread.currentPackageName();
            if (packageName != null) {
                boolean isGPhotosSpoofEnabled = SystemProperties.getBoolean(PropsHooksUtils.SPOOF_PIXEL_GPHOTOS, true);
                boolean isTensorDevice = SystemProperties.get("ro.product.model").matches("Pixel [6-9][a-zA-Z ]*");
                if (pixelPackages.contains(packageName)) {
                    if (containsAnyFeatureSet(name, featuresPixel, featuresPixelOthers, featuresTensor, featuresNexus)) {
                        return true;
                    }
                }
                if (packageName.equals("com.google.android.apps.photos") && isGPhotosSpoofEnabled) {
                    if (featuresPixel.contains(name)) return false;
                    return containsAnyFeatureSet(name, featuresPixelOthers, featuresNexus);
                }
                if (packageName.equals("com.google.android.as")) {
                    return isTensorDevice && featuresTensor.contains(name);
                }
                if (!isTensorDevice && featuresTensor.contains(name)) {
                    return false;
                }
                if (containsAnyFeatureSet(name, featuresPixel, featuresPixelOthers)) {
                    return true;
                }
            }
        }
        return hasSystemFeature;
    }

    private static boolean containsAnyFeatureSet(String name, Set<String>... featureSets) {
        for (Set<String> features : featureSets) {
            if (features.contains(name)) {
                return true;
            }
        }
        return false;
    }
}
