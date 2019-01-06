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
 * limitations under the License
 */

package com.android.car.notification;

import static com.google.common.truth.Truth.assertThat;

import android.app.Notification;
import android.content.Context;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(CarNotificationRobolectricTestRunner.class)
public class CarNotificationDiffTest {

    private Context mContext;
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

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

    private Notification.Builder mNotificationBuilder;

    private StatusBarNotification mNotification1;
    private StatusBarNotification mNotification2;
    private NotificationGroup mNotificationGroup1;
    private NotificationGroup mNotificationGroup2;
    private List<NotificationGroup> mNotificationGroupList1;
    private List<NotificationGroup> mNotificationGroupList2;

    @Before
    public void setupBaseActivityAndLayout() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mNotificationBuilder = new Notification.Builder(mContext,
                CHANNEL_ID)
                .setContentTitle(CONTENT_TITLE)
                .setSmallIcon(android.R.drawable.sym_def_app_icon);
        mNotificationGroup1 = new NotificationGroup();
        mNotificationGroup2 = new NotificationGroup();
        mNotificationGroupList1 = new ArrayList<>();
        mNotificationGroupList2 = new ArrayList<>();
        mNotification1 = new StatusBarNotification(PKG_1, OP_PKG,
                ID, TAG, UID, INITIAL_PID, mNotificationBuilder.build(), USER_HANDLE,
                OVERRIDE_GROUP_KEY, POST_TIME);
        mNotification2 = new StatusBarNotification(PKG_2, OP_PKG,
                ID, TAG, UID, INITIAL_PID, mNotificationBuilder.build(), USER_HANDLE,
                OVERRIDE_GROUP_KEY, POST_TIME);
        mNotificationGroup1.addNotification(mNotification1);
        mNotificationGroup2.addNotification(mNotification2);
        mNotificationGroupList1.add(mNotificationGroup1);
        mNotificationGroupList2.add(mNotificationGroup2);
    }

    /**
     * Test that the CarNotificationDiff's sameNotificationKey should return true.
     */
    @Test
    public void sameNotificationKey__shouldReturnTrue() {
        assertThat(
                CarNotificationDiff.sameNotificationKey(mNotification1, mNotification1)).isTrue();
    }

    /**
     * Test that the CarNotificationDiff's sameNotificationKey should return false.
     */
    @Test
    public void differentNotificationKey_returnsFalse() {
        assertThat(
                CarNotificationDiff.sameNotificationKey(mNotification1, mNotification2)).isFalse();
    }

    /**
     * Test that the CarNotificationDiff's sameGroupUniqueIdentifiers should return true.
     */
    @Test
    public void sameGroupUniqueIdentifiers_shouldReturnTrue() {
        assertThat(CarNotificationDiff.sameGroupUniqueIdentifiers(mNotificationGroup1,
                mNotificationGroup1)).isTrue();
    }

    /**
     * Test that the CarNotificationDiff's sameGroupUniqueIdentifiers should return false.
     */
    @Test
    public void differentGroupUniqueIdentifiers_shouldReturnFalse() {
        assertThat(CarNotificationDiff.sameGroupUniqueIdentifiers(mNotificationGroup1,
                mNotificationGroup2)).isFalse();
    }

    /**
     * Test that the CarNotificationDiff's areItemsTheSame should return true.
     */
    @Test
    public void sameItems_shouldReturnTrue() {
        CarNotificationDiff carNotificationDiff = new CarNotificationDiff(mContext,
                mNotificationGroupList1, mNotificationGroupList1);
        assertThat(carNotificationDiff.areItemsTheSame(0, 0)).isTrue();
    }

    /**
     * Test that the CarNotificationDiff's areItemsTheSame should return false.
     */
    @Test
    public void differentItems_shouldReturnFalse() {
        CarNotificationDiff carNotificationDiff = new CarNotificationDiff(mContext,
                mNotificationGroupList1, mNotificationGroupList2);
        assertThat(carNotificationDiff.areItemsTheSame(0, 0)).isFalse();
    }

}
