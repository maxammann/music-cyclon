package max.music_cyclon;


import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.samples.apps.iosched.ui.widget.SlidingTabLayout;

import org.json.JSONException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import max.music_cyclon.service.LibraryService;

/**
 * The main activity for synchronisation
 * <p>
 * This class manages:
 * <ul>
 * <li>
 *     the {@link PagerAdapter} with the references to all configs and their loading and saving.
 * </li>
 * <li>the link to the {@link LibraryService} with bi-directional message dispatching</li>
 * <li>the general layout</li>
 * <li>permission requests</li>
 * </ul>
 *
 */
public class SynchronizeActivity extends AppCompatActivity {

    private PagerAdapter pagerAdapter;

    /**
     * Messenger for communicating with service.
     */
    private Messenger serviceObject = null;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    private boolean isBound;

    /**
     * The dialog which is being displayed while a sync is in progress
     */
    private ProgressDialog syncProgress = null;

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger mMessenger = new Messenger(new IncomingHandler(new WeakReference<>(this)));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_synchronize);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        List<SynchronizeConfig> configs = Collections.emptyList();
        try {
            FileInputStream in = openFileInput("configs.json");
            configs = SynchronizeConfig.load(in);
            in.close();
        } catch (IOException | JSONException e) {
            Log.e("CONFIG", "Failed loading the config", e);
        }

        pagerAdapter = new PagerAdapter(configs, getSupportFragmentManager());


        final ViewPager pager = (ViewPager) findViewById(R.id.container);
        assert pager != null;
        pager.setAdapter(pagerAdapter);


        // Initialize tabs
        final SlidingTabLayout tabs = (SlidingTabLayout) findViewById(R.id.tabs);
        assert tabs != null;

        tabs.setDistributeEvenly(true);
        tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return ContextCompat.getColor(SynchronizeActivity.this, R.color.accentColor);
            }
        });

        tabs.setViewPager(pager);

        // Update tabs on dataset change
        pagerAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                tabs.setViewPager(pager);
            }
        });

        View addButton = findViewById(R.id.add_button);
        assert addButton != null;
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pagerAdapter.add(UUID.randomUUID().toString().substring(0, 5));
                pagerAdapter.notifyDataSetChanged();
            }
        });

        // Request permissions
        requestPermissions();
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindLibraryService();

        try {
            FileOutputStream fos = openFileOutput("configs.json", Context.MODE_PRIVATE);
            getPagerAdapter().save(fos);
            fos.close();
        } catch (IOException | JSONException e) {
            Log.e("CONFIG", "Failed saving the config", e);
        }
    }

    public PagerAdapter getPagerAdapter() {
        return pagerAdapter;
    }

    public Dialog getSyncProgress() {
        return syncProgress;
    }

    public void clearSyncProgress() {
        syncProgress = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent preferenceIntent = new Intent(this, MainPreferenceActivity.class);
                startActivity(preferenceIntent);
                return true;
            case R.id.action_version:
                notYetImplemented(this);
                break;
            case R.id.action_help:
                notYetImplemented(this);
                break;
            case R.id.action_sync:
                startLibraryService();

                bindLibraryService();

                // Show sync control dialog
                syncProgress = new ProgressDialog(SynchronizeActivity.this);
                syncProgress.setMessage("Synchronizing");
                syncProgress.setCancelable(false);
                syncProgress.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                syncProgress.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

        return false;
    }

    public static void notYetImplemented(Context context) {
        new AlertDialog.Builder(context).setMessage("Not yet implemented!").show();
    }

    private static class IncomingHandler extends Handler {
        private final WeakReference<SynchronizeActivity> activity;

        private IncomingHandler(WeakReference<SynchronizeActivity> activity) {
            this.activity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LibraryService.MSG_FINISHED:
                    SynchronizeActivity activity = this.activity.get();
                    if (activity != null) {
                        Dialog dialog = activity.getSyncProgress();
                        if (dialog != null) {
                            dialog.dismiss();
                            activity.clearSyncProgress();
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            serviceObject = new Messenger(service);

            try {
                Message msg = Message.obtain(null, LibraryService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                serviceObject.send(msg);
            } catch (RemoteException ignored) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            serviceObject = null;
        }
    };

    private void startLibraryService() {
        Intent intent = new Intent(SynchronizeActivity.this, LibraryService.class);
        List<SynchronizeConfig> configs = getPagerAdapter().getConfigData();
        intent.putExtra("configs", configs.toArray(new SynchronizeConfig[configs.size()]));
        SynchronizeActivity.this.startService(intent);
    }

    private void bindLibraryService() {
        bindService(new Intent(
                SynchronizeActivity.this,
                LibraryService.class
        ), mConnection, Context.BIND_AUTO_CREATE);
        isBound = true;
    }

    private void unbindLibraryService() {
        if (isBound) {
            if (serviceObject != null) {
                try {
                    Message msg = Message.obtain(null, LibraryService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    serviceObject.send(msg);
                } catch (RemoteException ignored) {
                }
            }

            // Detach our existing connection.
            unbindService(mConnection);
            isBound = false;
        }
    }
}
