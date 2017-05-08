package rillin.notificaway2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationManagerCompat;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/** rihllin 5/2017. */

public class NotificawayService extends NotificationListenerService {

    private NotificawayServiceReceiver receiver;
    private String mPackageName = this.getClass().getPackage().getName();

    private String SAVED_DATA_FILENAME = "saveddata.ser";

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
        try {
            // read file and deserialize list from it
            ObjectInputStream ois = new ObjectInputStream(this.openFileInput(SAVED_DATA_FILENAME));
            List<String[]> savedData = (List)ois.readObject();
            if (savedData == null)
                savedData = new ArrayList<>();

            // retrieve app name and store into savedData uniquely
            PackageManager packageManager= getApplicationContext().getPackageManager();
            String appName = (String)packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.getPackageName(), PackageManager.GET_META_DATA)
            );
            String[] dataToAdd = { sbn.getPackageName(), appName };
            boolean dataExists = false;
            for (String[] item : savedData) {
                dataExists = item[0].equals(dataToAdd[0]);
                if (dataExists)
                    break;
            }
            if (!dataExists)
                savedData.add(dataToAdd);

            // write savedData back into file
            FileOutputStream fos = this.openFileOutput(SAVED_DATA_FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(savedData);
            oos.close();
            fos.close();

            // broadcast and clear the notification
            broadcastToMain(getString(R.string.ADD_NOTIFICATION), sbn.getPackageName());
            this.cancelNotification(sbn.getKey());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private boolean hasNotificationAccess() {
        return NotificationManagerCompat.getEnabledListenerPackages(this).contains(mPackageName);
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
