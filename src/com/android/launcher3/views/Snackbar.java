/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.launcher3.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.TextView;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.dragndrop.DragLayer;

/**
 * A toast-like UI at the bottom of the screen with a label, button action, and dismiss action.
 */
public class Snackbar extends AbstractFloatingView {

    private static final long SHOW_DURATION_MS = 180;
    private static final long HIDE_DURATION_MS = 180;
    private static final long TIMEOUT_DURATION_MS = 4000;

    private final Launcher mLauncher;
    private Runnable mOnDismissed;

    public Snackbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Snackbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
        inflate(context, R.layout.snackbar, this);
    }

    public static void show(Launcher launcher, int labelStringResId, int actionStringResId,
            Runnable onDismissed, Runnable onActionClicked) {
        closeOpenViews(launcher, true, TYPE_SNACKBAR);
        Snackbar snackbar = new Snackbar(launcher, null);
        // Set some properties here since inflated xml only contains the children.
        snackbar.setOrientation(HORIZONTAL);
        snackbar.setGravity(Gravity.CENTER_VERTICAL);
        Resources res = launcher.getResources();
        snackbar.setElevation(res.getDimension(R.dimen.deep_shortcuts_elevation));
        int padding = res.getDimensionPixelSize(R.dimen.snackbar_padding);
        snackbar.setPadding(padding, padding, padding, padding);
        snackbar.setBackgroundResource(R.drawable.round_rect_primary);

        snackbar.mIsOpen = true;
        DragLayer dragLayer = launcher.getDragLayer();
        dragLayer.addView(snackbar);

        DragLayer.LayoutParams params = (DragLayer.LayoutParams) snackbar.getLayoutParams();
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        params.height = res.getDimensionPixelSize(R.dimen.snackbar_height);
        int margin = res.getDimensionPixelSize(R.dimen.snackbar_margin);
        Rect insets = launcher.getDeviceProfile().getInsets();
        params.width = dragLayer.getWidth() - margin * 2 -  insets.left - insets.right;
        params.setMargins(0, margin + insets.top, 0, margin + insets.bottom);

        ((TextView) snackbar.findViewById(R.id.label)).setText(labelStringResId);
        ((TextView) snackbar.findViewById(R.id.action)).setText(actionStringResId);
        snackbar.findViewById(R.id.action).setOnClickListener(v -> {
            if (onActionClicked != null) {
                onActionClicked.run();
            }
            snackbar.mOnDismissed = null;
            snackbar.close(true);
        });
        snackbar.mOnDismissed = onDismissed;

        snackbar.setAlpha(0);
        snackbar.setScaleX(0.8f);
        snackbar.setScaleY(0.8f);
        snackbar.animate()
                .alpha(1f)
                .withLayer()
                .scaleX(1)
                .scaleY(1)
                .setDuration(SHOW_DURATION_MS)
                .setInterpolator(Interpolators.ACCEL_DEACCEL)
                .start();
        snackbar.postDelayed(() -> snackbar.close(true), TIMEOUT_DURATION_MS);
    }

    @Override
    protected void handleClose(boolean animate) {
        if (mIsOpen) {
            if (animate) {
                animate().alpha(0f)
                        .withLayer()
                        .setStartDelay(0)
                        .setDuration(HIDE_DURATION_MS)
                        .setInterpolator(Interpolators.ACCEL)
                        .withEndAction(this::onClosed)
                        .start();
            } else {
                animate().cancel();
                onClosed();
            }
            mIsOpen = false;
        }
    }

    private void onClosed() {
        mLauncher.getDragLayer().removeView(this);
        if (mOnDismissed != null) {
            mOnDismissed.run();
        }
    }

    @Override
    public void logActionCommand(int command) {
        // TODO
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_SNACKBAR) != 0;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            DragLayer dl = mLauncher.getDragLayer();
            if (!dl.isEventOverView(this, ev)) {
                close(true);
            }
        }
        return false;
    }
}