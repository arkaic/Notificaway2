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
    private String packageName = this.getClass().getPackage().getName();

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
        // TODO copy notification data and add to list
        // Note: need to reset Notif access on every build&run
        broadcastToMain(getString(R.string.ADD_NOTIFICATION), sbn.getPackageName());
        this.cancelAllNotifications();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    private boolean hasNotificationAccess() {
        return NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName);
    }

    private void broadcastToMain(String commandType, String val) {
        Intent i = new Intent(getString(R.string.MAIN_ACTIVITY));
        i.putExtra(getString(R.string.COMMAND), commandType);
        i.putExtra(getString(R.string.DATA), val);
        sendBroadcast(i);
    }

    class NotificawayServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String commandVal = intent.getStringExtra(getString(R.string.COMMAND));
            if (commandVal.equals(getString(R.string.NOTIF_STATUS))) {
                String msg;
                if (!hasNotificationAccess()) {
                    msg = "Has no notification access";
                } else {
                    StatusBarNotification[] notifs = getActiveNotifications();
                    msg = (notifs == null)
                        ? "Notifications is null, try re-enabling access to notifications?"
                        : notifs.length + " notifications";
                }
                broadcastToMain(getString(R.string.RESULT_DISPLAY), msg);
            }
        }
    }
}
