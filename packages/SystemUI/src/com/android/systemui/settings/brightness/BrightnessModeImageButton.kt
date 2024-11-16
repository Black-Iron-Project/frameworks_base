/*
 * Copyright (C) 2024 The risingOS Android Project
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

package com.android.systemui.settings.brightness

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.UserHandle
import android.provider.Settings
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.ImageButton

import com.android.settingslib.Utils

class BrightnessModeImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageButton(context, attrs, defStyleAttr) {

    private val contentResolver = context.contentResolver
    private val translucentStyles = setOf(1, 2, 3, 9)

    private val settingsObserver = object : ContentObserver(Handler(context.mainLooper)) {
        override fun onChange(selfChange: Boolean) {
            updateTint()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
            false,
            settingsObserver
        )
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.QS_PANEL_STYLE),
            false,
            settingsObserver
        )
        updateTint()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        contentResolver.unregisterContentObserver(settingsObserver)
    }

    private fun updateTint() {
        val automatic = Settings.System.getIntForUser(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            UserHandle.USER_CURRENT
        )
        val isAutomatic = automatic != Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        val translucentStyleEnabled = translucentStyles.contains(
            Settings.System.getIntForUser(
                contentResolver,
                Settings.System.QS_PANEL_STYLE,
                0,
                UserHandle.USER_CURRENT
            )
        )
        val tintColor = if (isAutomatic && !translucentStyleEnabled) {
            Utils.getColorAttrDefaultColor(context, android.R.attr.textColorPrimaryInverse)
        } else {
            Utils.getColorAttrDefaultColor(context, android.R.attr.colorAccent)
        }
        drawable?.setTint(tintColor)
    }
}
