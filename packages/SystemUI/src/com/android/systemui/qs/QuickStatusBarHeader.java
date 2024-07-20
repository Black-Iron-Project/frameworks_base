/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.qs;

import static android.app.StatusBarManager.DISABLE2_QUICK_SETTINGS;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.systemui.res.R;
import com.android.systemui.shade.LargeScreenHeaderHelper;
import com.android.systemui.util.LargeScreenUtils;
import com.android.systemui.qs.TouchAnimator;
import com.android.systemui.qs.TouchAnimator.Builder;

/**
 * View that contains the top-most bits of the QS panel (primarily the status bar with date, time,
 * battery, carrier info and privacy icons) and also contains the {@link QuickQSPanel}.
 */
public class QuickStatusBarHeader extends FrameLayout {

    private boolean mExpanded;
    private boolean mQsDisabled;

    protected QuickQSPanel mHeaderQsPanel;

    private boolean mSceneContainerEnabled;
    
    public TouchAnimator mQQSContainerAnimator;
    
    private ViewGroup mQSControlLayout;

    public QuickStatusBarHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHeaderQsPanel = findViewById(R.id.quick_qs_panel);

        mQSControlLayout = findViewById(R.id.qs_controls);

        updateResources();
    }

    void setSceneContainerEnabled(boolean enabled) {
        mSceneContainerEnabled = enabled;
        if (mSceneContainerEnabled) {
            updateResources();
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Only react to touches inside QuickQSPanel
        if (event.getY() > mHeaderQsPanel.getTop()) {
            return super.onTouchEvent(event);
        } else {
            return false;
        }
    }

    void updateResources() {
        Resources resources = mContext.getResources();
        boolean largeScreenHeaderActive =
                LargeScreenUtils.shouldUseLargeScreenShadeHeader(resources);

        ViewGroup.LayoutParams lp = getLayoutParams();
        if (mQsDisabled) {
            lp.height = 0;
        } else {
            lp.height = WRAP_CONTENT;
        }
        setLayoutParams(lp);
        
        boolean mQsWidgetsEnabled = TileUtils.canShowQsWidgets(mContext);
 
        MarginLayoutParams qqsLP = (MarginLayoutParams) mHeaderQsPanel.getLayoutParams();
        if (mSceneContainerEnabled) {
            qqsLP.topMargin = 0;
        } else if (largeScreenHeaderActive) {
            qqsLP.topMargin = mContext.getResources()
                    .getDimensionPixelSize(R.dimen.qqs_layout_margin_top);
        } else {
            qqsLP.topMargin = LargeScreenHeaderHelper.getLargeScreenHeaderHeight(mContext);
        }

        int qqsTopMargin = mContext.getResources()
                    .getDimensionPixelSize(largeScreenHeaderActive 
                    ? R.dimen.qqs_layout_margin_top 
                    : R.dimen.large_screen_shade_header_min_height);
        qqsLP.topMargin = mQsWidgetsEnabled ? 0 : qqsTopMargin;
        mHeaderQsPanel.setLayoutParams(qqsLP);

        if (mQsWidgetsEnabled && !mQsDisabled) {
            mQSControlLayout.setVisibility(View.VISIBLE);
            MarginLayoutParams qsControlsLp = (MarginLayoutParams) mQSControlLayout.getLayoutParams();
            int qqsMarginTop = resources.getDimensionPixelSize(largeScreenHeaderActive ?
                                R.dimen.qqs_layout_margin_top : R.dimen.large_screen_shade_header_min_height);
            qsControlsLp.topMargin = qqsMarginTop;
            mQSControlLayout.setLayoutParams(qsControlsLp);

            float qqsExpandY = resources.getDimensionPixelSize(R.dimen.qs_header_height)
                                + resources.getDimensionPixelSize(R.dimen.qs_controls_top_margin)
                                - qqsMarginTop;
            TouchAnimator.Builder builderP = new TouchAnimator.Builder()
                .addFloat(mQSControlLayout, "translationY", 0, qqsExpandY);
            mQQSContainerAnimator = builderP.build();
        } else {
            mQSControlLayout.setVisibility(View.GONE);
        }
    }

    public void setExpanded(boolean expanded, QuickQSPanelController quickQSPanelController) {
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        quickQSPanelController.setExpanded(expanded);
    }

    public void setExpansion(boolean forceExpanded, float expansionFraction, float panelTranslationY) {
        if (!TileUtils.canShowQsWidgets(mContext)) return;
        if (mQQSContainerAnimator != null) {
            mQQSContainerAnimator.setPosition(forceExpanded ? 1f : expansionFraction);
        }
        setAlpha(forceExpanded ? expansionFraction : 1);
	}

    public void disable(int state1, int state2, boolean animate) {
        final boolean disabled = (state2 & DISABLE2_QUICK_SETTINGS) != 0;
        if (disabled == mQsDisabled) return;
        mQsDisabled = disabled;
        mHeaderQsPanel.setDisabledByPolicy(disabled);
        updateResources();
    }

    private void setContentMargins(View view, int marginStart, int marginEnd) {
        MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
        lp.setMarginStart(marginStart);
        lp.setMarginEnd(marginEnd);
        view.setLayoutParams(lp);
    }

    /**
     * @return height with the squishiness fraction applied.
     */
    public int getSquishedHeight() {
        return mHeaderQsPanel.getSquishedHeight();
    }
}
