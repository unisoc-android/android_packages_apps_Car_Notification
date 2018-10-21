package com.android.car.notification;

import android.car.CarNotConnectedException;
import android.car.drivingstate.CarUxRestrictions;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class is a bridge to collect signals from the notification and ux restriction services and
 * trigger the correct UI updates.
 */
public class NotificationViewController {

    private static final String TAG = "NotificationViewControl";
    private final CarNotificationView mCarNotificationView;
    private final PreprocessingManager mPreprocessingManager;
    private final CarNotificationListener mCarNotificationListener;
    private CarUxRestrictionManagerWrapper mUxResitrictionListener;
    private NotificationUpdateHandler mNotificationUpdateHandler = new NotificationUpdateHandler();

    public NotificationViewController(CarNotificationView carNotificationView,
            PreprocessingManager preprocessingManager,
            CarNotificationListener carNotificationListener,
            CarUxRestrictionManagerWrapper uxResitrictionListener) {
        mCarNotificationView = carNotificationView;
        mPreprocessingManager = preprocessingManager;
        mCarNotificationListener = carNotificationListener;
        mUxResitrictionListener = uxResitrictionListener;
    }

    /**
     * Updates UI and registers required listeners
     */
    public void enable() {
        mCarNotificationListener.setHandler(mNotificationUpdateHandler);
        mUxResitrictionListener.setCarNotificationView(mCarNotificationView);
        try {
            CarUxRestrictions currentRestrictions =
                    mUxResitrictionListener.getCurrentCarUxRestrictions();
            mCarNotificationView.onUxRestrictionsChanged(currentRestrictions);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car not connected", e);
        }
        updateNotifications();
    }

    /**
     * Removes listeners
     */
    public void disable() {
        mCarNotificationListener.setHandler(null);
        mUxResitrictionListener.setCarNotificationView(null);
    }


    /**
     * Update all notifications and ranking
     */
    private void updateNotifications() {
        mCarNotificationView.setNotifications(
                mPreprocessingManager.process(
                        mCarNotificationListener.getNotifications(),
                        mCarNotificationListener.getCurrentRanking()));
    }

    private class NotificationUpdateHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            if (message.what == CarNotificationListener.NOTIFY_NOTIFICATIONS_CHANGED) {
                updateNotifications();

            } else if (message.what == CarNotificationListener.NOTIFY_NOTIFICATION_ADDED) {
                updateNotifications();
            }
        }
    }
}
