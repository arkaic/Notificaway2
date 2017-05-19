package rillin.notificaway2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private NotificationReceiver mReceiver;
    private ArrayAdapter<String> mListViewAdapter;
    private List<String[]> mSavedData = new ArrayList<>();
    private List<String> mAppList = new ArrayList<>();  // registered with adapter

    // CONSTANTS
    private String SAVED_DATA_FILENAME = "saveddata.ser";
    private String DATA;
    private String ADD_NOTIFICATION;
    private String RESULT_DISPLAY;
    private String NOTIF_STATUS;
    private String COMMAND;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize CONSTANTS TODO better way of doing this?
        DATA = getString(R.string.DATA);
        ADD_NOTIFICATION = getString(R.string.ADD_NOTIFICATION);
        RESULT_DISPLAY = getString(R.string.RESULT_DISPLAY);
        NOTIF_STATUS = getString(R.string.NOTIF_STATUS);
        COMMAND = getString(R.string.COMMAND);

        // deserialize list from file
        mSavedData = readSavedDataFromFile();
        mAppList.clear();
        for (String[] item : mSavedData) {
            mAppList.add(item[1]);  // { packageName, appName }
        }
        mListViewAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mAppList);
        ListView listView = (ListView)findViewById(android.R.id.list);
        listView.setAdapter(mListViewAdapter);

        // set up intent broadcast receiving
        mReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.MAIN_ACTIVITY));
        registerReceiver(mReceiver, filter);

        // event handlers
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String appName = mAppList.get(position);
                String packageName = null;
                for (String[] item : mSavedData) {
                    if (item[1].equals(appName)) {
                        packageName = item[0];
                        break;
                    }
                }

                if (packageName == null)
                    return;

                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent != null)
                    startActivity(launchIntent);
                mSavedData.remove(position);
                mAppList.remove(position);
                mListViewAdapter.notifyDataSetChanged();
            }
        });
        findViewById(R.id.clearAllBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSavedData.clear();
                writeSavedDataToFile();
                mAppList.clear();
                mListViewAdapter.notifyDataSetChanged();
            }
        });
        findViewById(R.id.testStatusBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                broadcastToService(NOTIF_STATUS);
            }
        });

        broadcastToService(NOTIF_STATUS);
        startService(new Intent(this, NotificawayService.class));
    }

    @Override
    public void onResume() {
        super.onResume();
        broadcastToService(NOTIF_STATUS);
    }

    @Override
    public void onPause() {
        super.onPause();
        writeSavedDataToFile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    /** autogenned */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /** autogenned */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private List<String[]> readSavedDataFromFile() {
        List<String[]> savedData = null;
        try {
            // TODO optimize when app is currently foreground; below assumes it's in background
            // http://stackoverflow.com/questions/5504632/how-can-i-tell-if-android-app-is-running-in-the-foreground
            ObjectInputStream ois = new ObjectInputStream(this.openFileInput(SAVED_DATA_FILENAME));
            savedData = (List)ois.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (savedData == null)
                savedData = new ArrayList<>();
            return savedData;
        }
    }

    private void writeSavedDataToFile() {
        try {
            FileOutputStream fos = this.openFileOutput(SAVED_DATA_FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(mSavedData);
            oos.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastToService(String commandValue) {
        Intent i = new Intent(getString(R.string.NOTIFICAWAY_SERVICE));  // todo put into privates
        i.putExtra(COMMAND, commandValue);
        sendBroadcast(i);
    }

    /** Receives message from other app components (eg NotificawayService) */
    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String commandType = intent.getStringExtra(COMMAND);
            if (commandType.equals(ADD_NOTIFICATION)) {
                // receiving service's broadcast after intercepting a notification
                mSavedData = readSavedDataFromFile();

                // renew adapter's list
                mAppList.clear();
                for (String[] item : mSavedData) {
                    mAppList.add(item[1]);  // item={packageName, appName}
                }
                mListViewAdapter.notifyDataSetChanged();
            } else if (commandType.equals(RESULT_DISPLAY)) {
                String toDisplay = intent.getStringExtra(DATA);
                Snackbar.make(findViewById(R.id.toolbar), toDisplay, Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .show();
            }
        }
    }
}
