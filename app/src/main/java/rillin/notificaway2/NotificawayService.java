package rillin.notificaway2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationManagerCompat;

/** rihllin 5/2017. */

public class NotificawayService extends NotificationListenerService {

    private NotificawayServiceReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();
        if (!hasNotificationAccess()) {
            // todo prompt user to give notification access to this service
        }

        // set up intent broadcast receiving
        receiver = new NotificawayServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.NOTIFICAWAY_SERVICE));
        registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    private boolean hasNotificationAccess() {
        return NotificationManagerCompat.getEnabledListenerPackages(this).contains(this.getClass().getPackage().getName());
    }

    class NotificawayServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }
}
