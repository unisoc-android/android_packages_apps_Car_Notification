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

@IntDef({NotificationViewType.BASIC,
        NotificationViewType.BASIC_HEADSUP,
        NotificationViewType.BASIC_IN_GROUP,
        NotificationViewType.MESSAGE,
        NotificationViewType.MESSAGE_HEADSUP,
        NotificationViewType.MESSAGE_IN_GROUP,
        NotificationViewType.PROGRESS,
        NotificationViewType.PROGRESS_IN_GROUP,
        NotificationViewType.GROUP_COLLAPSED,
        NotificationViewType.GROUP_EXPANDED
})
@Retention(RetentionPolicy.SOURCE)
@interface NotificationViewType {

    int BASIC = 0;
    int BASIC_HEADSUP = 1;
    int BASIC_IN_GROUP = 2;

    int MESSAGE = 3;
    int MESSAGE_HEADSUP = 4;
    int MESSAGE_IN_GROUP = 5;

    int PROGRESS = 6;
    int PROGRESS_IN_GROUP = 7;

    int GROUP_COLLAPSED = 8;
    int GROUP_EXPANDED = 9;
}
