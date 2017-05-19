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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** rihllin 5/2017. */

public class NotificawayService extends NotificationListenerService {

    private String SAVED_DATA_FILENAME = "saveddata.ser";
    private String DATA;
    private String ADD_NOTIFICATION;
    private String RESULT_DISPLAY;
    private String NOTIF_STATUS;
    private String COMMAND;
    private String MAIN_ACTIVITY;

    private Set<String> mAppFilter = new HashSet<>();  // notifications to keep

    private NotificawayServiceReceiver receiver;
    private String mPackageName = this.getClass().getPackage().getName();

    @Override
    public void onCreate() {
        super.onCreate();

        // pre-determined notifications to filter
        mAppFilter.add("Android System");
        mAppFilter.add("Maps");
        mAppFilter.add("Internet Speed Meter Lite");

        // Initialize CONSTANTS TODO better way of doing this?
        DATA = getString(R.string.DATA);
        ADD_NOTIFICATION = getString(R.string.ADD_NOTIFICATION);
        RESULT_DISPLAY = getString(R.string.RESULT_DISPLAY);
        NOTIF_STATUS = getString(R.string.NOTIF_STATUS);
        COMMAND = getString(R.string.COMMAND);
        MAIN_ACTIVITY = getString(R.string.MAIN_ACTIVITY);

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
        // Note: need to reset Notif access on every build&run
        try {
            // retrieve app name and store into savedData uniquely
            PackageManager packageManager = getApplicationContext().getPackageManager();
            String packageName = sbn.getPackageName();
            String appName = (String) packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            );

            if (mAppFilter.contains(appName))
                return;

            // read file and deserialize list from it
            ObjectInputStream ois = new ObjectInputStream(this.openFileInput(SAVED_DATA_FILENAME));
            List<String[]> savedData = (List)ois.readObject();
            if (savedData == null)
                savedData = new ArrayList<>();
            String[] dataToAdd = { packageName, appName };
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
            broadcastToMain(ADD_NOTIFICATION, packageName);
            cancelNotification(sbn.getKey());
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

    @Override
    public void onNotificationRemoved (StatusBarNotification sbn) {
        // TODO
    }

    private boolean hasNotificationAccess() {
        return NotificationManagerCompat.getEnabledListenerPackages(this).contains(mPackageName);
    }

    private void broadcastToMain(String commandType, String val) {
        Intent i = new Intent(MAIN_ACTIVITY);
        i.putExtra(COMMAND, commandType);
        i.putExtra(DATA, val);
        sendBroadcast(i);
    }

    class NotificawayServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String commandVal = intent.getStringExtra(COMMAND);
            if (commandVal.equals(NOTIF_STATUS)) {
                String directions = "(Settings > Apps > [top-right gear icon] > Special Access > Notification Access)";
                if (!hasNotificationAccess()) {
                    broadcastToMain(RESULT_DISPLAY, "No notification access, please enable: "+directions);
                } else {
                    if (getActiveNotifications() == null) {
                        broadcastToMain(RESULT_DISPLAY, "Please re-enable Notification Access: "+directions);
                    }
                }
            }
        }
    }
}
