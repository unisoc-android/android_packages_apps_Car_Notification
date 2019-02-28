/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.notification;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Custom {@link FrameLayout} to display Heads up notifications. The only change from the {@link
 * FrameLayout} is that it will consume all the touch intercepts for the defined duration before
 * passing it to its children.
 */
public class HeadsUpNotificationView extends FrameLayout {

    private long mTouchAcceptanceDelay;
    private boolean mShouldConsumeTouch = true;

    public HeadsUpNotificationView(Context context) {
        super(context);
        init(context);
    }

    public HeadsUpNotificationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HeadsUpNotificationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public HeadsUpNotificationView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mTouchAcceptanceDelay = context.getResources().getInteger(
                R.integer.touch_acceptance_delay);
        Handler handler = new Handler();
        handler.postDelayed(() -> toggleConsumedTouch(), mTouchAcceptanceDelay);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mShouldConsumeTouch;
    }

    private void toggleConsumedTouch() {
        mShouldConsumeTouch = !mShouldConsumeTouch;
    }
}
