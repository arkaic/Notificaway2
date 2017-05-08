package rillin.notificaway2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private NotificationReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set up intent broadcast receiving
        receiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.MAIN_ACTIVITY));
        registerReceiver(receiver, filter);

        // button event handlers
        findViewById(R.id.clearAllBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                commandTheListener(getString(R.string.CLEAR_ALL));
            }
        });
        findViewById(R.id.testStatusBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                commandTheListener(getString(R.string.NOTIF_STATUS));
            }
        });

        startService(new Intent(this, NotificawayService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
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

    private void commandTheListener(String commandValue) {
        Intent i = new Intent(getString(R.string.NOTIFICAWAY_SERVICE));
        i.putExtra(getString(R.string.COMMAND), commandValue);
        sendBroadcast(i);
    }

    /** Receives message from other app components */
    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String toDisplay = intent.getStringExtra(getString(R.string.RESULT_DISPLAY));
            Snackbar.make(findViewById(R.id.toolbar), toDisplay, Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .show();
        }
    }
}
