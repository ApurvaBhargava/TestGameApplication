package com.example.myapplication;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.IDbInterface;
import org.opendatakit.database.service.InternalUserDbInterfaceAidlWrapperImpl;
import org.opendatakit.database.service.UserDbInterfaceImpl;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.properties.PropertiesSingleton;
import org.opendatakit.sync.service.IOdkSyncServiceInterface;
import org.opendatakit.sync.service.SyncAttachmentState;
import org.opendatakit.sync.service.SyncStatus;
import org.opendatakit.utilities.ODKFileUtils;
import org.opendatakit.utilities.StaticStateManipulator;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    String appName = "default";
    //private PropertiesSingleton props;
    //private DbHandle dbHandle;

    IOdkSyncServiceInterface sService = null;
    private boolean isBoundSync;
    Intent intent_sync = new Intent();
    ServiceConnection sConnection = null;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*//check if runtime permission for writing to external storage is granted, else, request it
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.v("TAG","Permission is granted");
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        //ODKFileUtils.verifyExternalStorageAvailability();
        //ODKFileUtils.assertDirectoryStructure(appName);

        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            StaticStateManipulator.get().reset();
            PropertiesSingleton props = CommonToolProperties.get(this, appName);
            Map<String, String> properties = new HashMap<String, String>();
            //secure properties (error in setting: cannot set from outside of ODK-X Services)
            //properties.put(CommonToolProperties.KEY_ALLOW_NON_SECURE_AUTHENTICATION, "true");
            //properties.put(CommonToolProperties.KEY_AUTHENTICATED_USER_ID, "common.userid");
            //properties.put(CommonToolProperties.KEY_PASSWORD, "");
            //general properties
            properties.put(CommonToolProperties.KEY_SYNC_SERVER_URL, "http://a1e78c0b9ce7.ngrok.io");
            //device properties
            properties.put(CommonToolProperties.KEY_AUTHENTICATION_TYPE, "common.auth_credentials");
            properties.put(CommonToolProperties.KEY_USERNAME, "");
            props.setProperties(properties);

            StaticStateManipulator.get().reset();
            props = CommonToolProperties.get(this, appName);
            Log.d("PROPERTY VALUE", props.getProperty(CommonToolProperties.KEY_SYNC_SERVER_URL));
            Log.d("PROPERTY VALUE", props.getProperty(CommonToolProperties.KEY_AUTHENTICATION_TYPE));
            Log.d("PROPERTY VALUE", props.getProperty(CommonToolProperties.KEY_USERNAME));
        }*/

        sConnection = new ServiceConnection() {
            // Called when the connection with the service is established
            public void onServiceConnected(ComponentName className, IBinder service) {
                // Following the example above for an AIDL interface,
                // this gets an instance of the IRemoteInterface, which we can use to call on the service
                sService = IOdkSyncServiceInterface.Stub.asInterface(service);
                Log.i("TAG", "Service connected");
                isBoundSync = true;
            }
            // Called when the connection with the service disconnects unexpectedly
            public void onServiceDisconnected(ComponentName className) {
                Log.i("TAG", "Service disconnected due to unexplained causes.");
            }
        };

        intent_sync.setComponent(new ComponentName("org.opendatakit.services", "org.opendatakit.services.sync.service.OdkSyncService"));
        intent_sync.setAction(IOdkSyncServiceInterface.class.getName());
        bindService(intent_sync, sConnection, Context.BIND_AUTO_CREATE);

        final Button createButton = findViewById(R.id.button_create_game);
        createButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intentNext = new Intent(MainActivity.this, CreateFormActivity.class);
                MainActivity.this.startActivity(intentNext);
            }
        });

        final Button selectButton = findViewById(R.id.button_select_game);
        selectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intentNext = new Intent(MainActivity.this, SelectGameActivity.class);
                MainActivity.this.startActivity(intentNext);
            }
        });

        final Button startButton = findViewById(R.id.button_start_game);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // run synchronization and wait until sync status changes from SYNCING to something else
                try {
                    boolean isSynced = sService.synchronizeWithServer(appName, SyncAttachmentState.REDUCED_SYNC);
                    Log.i("TAG IS SYNCED", String.valueOf(isSynced));
                    while(true) if(sService.getSyncStatus(appName)!= SyncStatus.SYNCING) break;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                // clear app synchronizer
                try {
                    sService.clearAppSynchronizer(appName);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                //unbind and stop service
                unbindService(sConnection);
                boolean hasStopped = stopService(intent_sync);
                // wait until service has stopped
                while(true) {
                    if(hasStopped) {
                        Log.i("TAG", "Service unbounded.");
                        isBoundSync = false;
                        break;
                    }
                }
                startButton.setVisibility(View.GONE);
                createButton.setVisibility(View.VISIBLE);
                selectButton.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        sConnection = new ServiceConnection() {
            // Called when the connection with the service is established
            public void onServiceConnected(ComponentName className, IBinder service) {
                // Following the example above for an AIDL interface,
                // this gets an instance of the IRemoteInterface, which we can use to call on the service
                sService = IOdkSyncServiceInterface.Stub.asInterface(service);
                Log.i("TAG", "Service connected");
                isBoundSync = true;
            }
            // Called when the connection with the service disconnects unexpectedly
            public void onServiceDisconnected(ComponentName className) {
                Log.i("TAG", "Service disconnected due to unexplained causes.");
            }
        };

        intent_sync.setComponent(new ComponentName("org.opendatakit.services", "org.opendatakit.services.sync.service.OdkSyncService"));
        intent_sync.setAction(IOdkSyncServiceInterface.class.getName());
        bindService(intent_sync, sConnection, Context.BIND_AUTO_CREATE);
    }
}