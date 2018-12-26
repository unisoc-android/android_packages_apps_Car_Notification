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

package com.android.car.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.os.IBinder;
import android.view.View;

import com.android.car.notification.testutils.ShadowCar;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link CarNotificationCenterActivity} .
 */
@RunWith(CarNotificationRobolectricTestRunner.class)
@Config(shadows = {ShadowCar.class})
public class CarNotificationCenterActivityTest {

    private TestCarNotificationCenterActivity mActivity;
    private Context mContext;
    private ActivityController<TestCarNotificationCenterActivity> mActivityController;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Before
    public void setupBaseActivityAndLayout() {
        MockitoAnnotations.initMocks(this);
        IBinder mBinder = mock(IBinder.class);

        mActivityController = ActivityController.of(new TestCarNotificationCenterActivity());
        mActivity = mActivityController.get();
        mActivity.setBinder(mBinder);
        mActivityController.create();
        mContext = RuntimeEnvironment.application;
    }

    @After
    public void tearDown() {
        ShadowCar.reset();
    }

    /**
     * Test that the CarNotificationCenterActivity's view is loaded and has a exit button.
     */
    @Test
    public void testExitButtonInCarNotificationCenterActivity() {
        View contentView = mActivity.findViewById(R.id.exit_button_container);
        assertThat(contentView).isNotNull();
    }

    /**
     * This class is needed to for testing {@link CarNotificationCenterActivity} as robolectric
     * always return a null when bindservice() method is called. This class helps setting up a
     * binder to continue with the the unit testing for {@link CarNotificationCenterActivity}.
     */
    private static class TestCarNotificationCenterActivity extends CarNotificationCenterActivity {

        private IBinder mServiceBinder;

        public void setBinder(IBinder binder) {
            mServiceBinder = binder;
        }
    }
}