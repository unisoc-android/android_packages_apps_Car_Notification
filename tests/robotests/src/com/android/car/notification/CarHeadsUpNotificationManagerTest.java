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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.content.Context;
import android.os.UserHandle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

@RunWith(CarNotificationRobolectricTestRunner.class)
public class CarHeadsUpNotificationManagerTest {

    private Context mContext;

    @Mock
    NotificationListenerService.RankingMap mRankingMapMock;

    @Mock
    NotificationClickHandlerFactory mClickHandlerFactory;

    private CarHeadsUpNotificationManager mManager;

    private static final String PKG_1 = "package_1";
    private static final String PKG_2 = "package_2";
    private static final String OP_PKG = "OpPackage";
    private static final int ID = 1;
    private static final String TAG = "Tag";
    private static final int UID = 2;
    private static final int INITIAL_PID = 3;
    private static final String CHANNEL_ID = "CHANNEL_ID";
    private static final String CONTENT_TITLE = "CONTENT_TITLE";
    private static final String OVERRIDE_GROUP_KEY = "OVERRIDE_GROUP_KEY";
    private static final long POST_TIME = 12345l;
    private static final UserHandle USER_HANDLE = new UserHandle(12);

    private Notification.Builder mNotificationBuilder1;
    private Notification.Builder mNotificationBuilder2;

    private StatusBarNotification mNotification1;
    private StatusBarNotification mNotification2;


    @Before
    public void setupBaseActivityAndLayout() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;

        when(mClickHandlerFactory.getClickHandler(any())).thenReturn(new View.OnClickListener() {
            @Override
            public void onClick(View v) { }
        });
        mNotificationBuilder1 = new Notification.Builder(mContext, CHANNEL_ID)
                .setContentTitle(CONTENT_TITLE)
                .setSmallIcon(android.R.drawable.sym_def_app_icon);
        mNotificationBuilder2 = new Notification.Builder(mContext, CHANNEL_ID)
                .setContentTitle(CONTENT_TITLE)
                .setCategory(Notification.CATEGORY_CAR_WARNING)
                .setSmallIcon(android.R.drawable.sym_def_app_icon);
        mNotification1 = new StatusBarNotification(PKG_1, OP_PKG,
                ID, TAG, UID, INITIAL_PID, mNotificationBuilder1.build(), USER_HANDLE,
                OVERRIDE_GROUP_KEY, POST_TIME);
        mNotification2 = new StatusBarNotification(PKG_2, OP_PKG,
                ID, TAG, UID, INITIAL_PID, mNotificationBuilder2.build(), USER_HANDLE,
                OVERRIDE_GROUP_KEY, POST_TIME);

    }

    /**
     * Resets the state of the shadow after every test is run.
     */
    @After
    public void resetShadow() {
        mManager = null;
        mContext = null;
    }

    /**
     * Test that Heads up notification should not be shown.
     */
    @Test
    public void maybeShowHeadsUp_hasNoRanking_returnsFalse() {
        mManager = new CarHeadsUpNotificationManager(mContext);
        when(mRankingMapMock.getRanking(any(), any())).thenReturn(false);

        mManager.maybeShowHeadsUp(mNotification1, mRankingMapMock);
        View notificationView = mManager.getNotificationView();

        assertThat(notificationView).isNull();
    }

    /**
     * Test that Heads up notification should be shown when category is CATEGORY_CAR_WARNING.
     */
    @Test
    public void maybeShowHeadsUp_isCarWarning_returnsTrue() {
        mManager = CarHeadsUpNotificationManager.getInstance(mContext, mClickHandlerFactory);
        when(mRankingMapMock.getRanking(any(), any())).thenReturn(true);

        Context context = RuntimeEnvironment.application;
        Shadows.shadowOf(context.getPackageManager()).addPackage(PKG_2);
        mManager.maybeShowHeadsUp(mNotification2, mRankingMapMock);
        View notificationView = mManager.getNotificationView();

        assertThat(notificationView).isNotNull();
    }
}
