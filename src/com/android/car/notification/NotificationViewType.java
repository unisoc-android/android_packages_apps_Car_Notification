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

import android.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({
        NotificationViewType.GROUP_COLLAPSED,
        NotificationViewType.GROUP_EXPANDED,
        NotificationViewType.GROUP_SUMMARY,
        NotificationViewType.BASIC,
        NotificationViewType.BASIC_HEADSUP,
        NotificationViewType.BASIC_IN_GROUP,
        NotificationViewType.MESSAGE,
        NotificationViewType.MESSAGE_HEADSUP,
        NotificationViewType.MESSAGE_IN_GROUP,
        NotificationViewType.PROGRESS,
        NotificationViewType.PROGRESS_IN_GROUP,
        NotificationViewType.INBOX,
        NotificationViewType.INBOX_HEADSUP,
        NotificationViewType.INBOX_IN_GROUP,
        NotificationViewType.EMERGENCY,
        NotificationViewType.EMERGENCY_HEADSUP
})
@Retention(RetentionPolicy.SOURCE)
@interface NotificationViewType {

    int GROUP_COLLAPSED = 1;
    int GROUP_EXPANDED = 2;
    int GROUP_SUMMARY = 3;

    int BASIC = 4;
    int BASIC_HEADSUP = 5;
    int BASIC_IN_GROUP = 6;

    int MESSAGE = 7;
    int MESSAGE_HEADSUP = 8;
    int MESSAGE_IN_GROUP = 9;

    int PROGRESS = 10;
    int PROGRESS_IN_GROUP = 11;

    int INBOX = 12;
    int INBOX_HEADSUP = 13;
    int INBOX_IN_GROUP = 14;

    int EMERGENCY = 15;
    int EMERGENCY_HEADSUP = 16;
}
