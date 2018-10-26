package com.android.car.notification;

import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import androidx.car.widget.PagedListView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.android.car.notification.template.GroupNotificationViewHolder;

import java.util.List;


/**
 * Layout that contains Car Notifications.
 *
 * It does some extra setup in the onFinishInflate method because it may not get used from an
 * activity where one would normally attach RecyclerViews
 */
public class CarNotificationView extends RelativeLayout
        implements CarUxRestrictionsManager.OnUxRestrictionsChangedListener {

    private CarNotificationViewAdapter mAdapter;
    private Context mContext;
    public CarNotificationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    /**
     * Attaches the CarNotificationViewAdapter and CarNotificationItemTouchHelper to the
     * notification list.
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        PagedListView listView = findViewById(R.id.notifications);
        mAdapter = new CarNotificationViewAdapter(mContext,/* isGroupNotificationAdapter= */ false);
        listView.setAdapter(mAdapter);
        ((SimpleItemAnimator) listView.getRecyclerView().getItemAnimator())
                .setSupportsChangeAnimations(false);

        new ItemTouchHelper(
                new CarNotificationItemTouchHelper(mAdapter) {
                    @Override
                    public int getSwipeDirs(RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder) {
                        if (viewHolder instanceof GroupNotificationViewHolder) {
                            String groupKey =
                                    mAdapter.getNotificationAtPosition(
                                            viewHolder.getAdapterPosition()).getGroupKey();
                            // disable swiping for expanded group notifications, so that the child
                            // recycler view can receive the touch event
                            if (mAdapter.isExpanded(groupKey)) {
                                return 0;
                            }
                        }
                        return super.getSwipeDirs(recyclerView, viewHolder);
                    }
                })
                .attachToRecyclerView(listView.getRecyclerView());
    }

    /**
     * Updates notifications and update views.
     */
    public void setNotifications(List<NotificationGroup> notifications) {
        mAdapter.setNotifications(notifications);
    }

    @Override
    public void onUxRestrictionsChanged(CarUxRestrictions restrictionInfo) {
        mAdapter.setCarUxRestrictions(restrictionInfo);
    }
}
