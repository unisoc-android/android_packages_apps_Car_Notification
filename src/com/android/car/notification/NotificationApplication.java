package com.android.car.notification;

import android.app.Application;
import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * Application class that makes connections to the car service api so components can share these
 * objects
 */
public class NotificationApplication extends Application {
    private static final String TAG = "NotificationApplication";
    private Car mCar;
    private CarUxRestrictionsManager mManager;

    private ServiceConnection mCarConnectionListener = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                mManager = (CarUxRestrictionsManager) mCar.getCarManager(
                        Car.CAR_UX_RESTRICTION_SERVICE);
                PreprocessingManager preprocessingManager = PreprocessingManager.getInstance(
                        getApplicationContext());
                preprocessingManager.setCarUxRestrictionsManager(mManager);
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car not connected in CarConnectionListener", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };


    /**
     * Get the CarUxRestrictionsManager used to determine visual treatment of notifications.
     *
     * @return CarUxRestrictionManager or {@code null} if the connection to the car service is
     * not yet established
     */
    @Nullable
    public CarUxRestrictionsManager getCarUxRestrictionsManager() {
        return mManager;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mCar = Car.createCar(this, mCarConnectionListener);
        mCar.connect();
    }
}
