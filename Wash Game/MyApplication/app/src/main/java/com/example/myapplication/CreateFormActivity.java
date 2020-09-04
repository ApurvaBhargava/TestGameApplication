package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.IDbInterface;
import org.opendatakit.database.service.InternalUserDbInterfaceAidlWrapperImpl;
import org.opendatakit.database.service.UserDbInterfaceImpl;
import org.opendatakit.exception.ActionNotAuthorizedException;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.sync.service.IOdkSyncServiceInterface;
import org.opendatakit.sync.service.SyncAttachmentState;
import org.opendatakit.sync.service.SyncStatus;
import org.opendatakit.utilities.LocalizationUtils;

public class CreateFormActivity extends AppCompatActivity {

    String creatorName, gameID, roundNum, playerNum, pa, pb, pc;

    boolean serviceRegFlag = false;

    IDbInterface mService = null;
    UserDbInterfaceImpl nService = null;
    InternalUserDbInterfaceAidlWrapperImpl oService;
    private boolean isBound;
    String appName = "default";
    Intent intent = new Intent();
    ServiceConnection mConnection;

    IOdkSyncServiceInterface sService = null;
    private boolean isBoundSync;
    Intent intent_sync = new Intent();
    ServiceConnection sConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_form);

        mConnection = new ServiceConnection() {
            // Called when the connection with the service is established
            public void onServiceConnected(ComponentName className, IBinder service) {
                // Following the example above for an AIDL interface,
                // this gets an instance of the IRemoteInterface, which we can use to call on the service
                mService = IDbInterface.Stub.asInterface(service);
                Log.i("TAG", "Service connected");
                isBound = true;
            }

            // Called when the connection with the service disconnects unexpectedly
            public void onServiceDisconnected(ComponentName className) {
                Log.i("TAG", "Service disconnected due to unexplained causes.");
                mService = null;
            }
        };

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

        intent.setComponent(new ComponentName("org.opendatakit.services", "org.opendatakit.services.database.service.OdkDatabaseService"));
        intent.setAction(IDbInterface.class.getName());
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        intent_sync.setComponent(new ComponentName("org.opendatakit.services", "org.opendatakit.services.sync.service.OdkSyncService"));
        intent_sync.setAction(IOdkSyncServiceInterface.class.getName());
        bindService(intent_sync, sConnection, Context.BIND_AUTO_CREATE);

        Button createButton = findViewById(R.id.button_create_game_final);
        createButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                serviceRegFlag = true;

                EditText edit = findViewById(R.id.EditCreatorName);
                creatorName = edit.getText().toString();
                edit = findViewById(R.id.EditGameID);
                gameID = edit.getText().toString();
                edit = findViewById(R.id.EditRoundNum);
                roundNum = edit.getText().toString();
                edit = findViewById(R.id.EditPlayerNum);
                playerNum = edit.getText().toString();
                edit = findViewById(R.id.ParamA);
                pa = edit.getText().toString();
                edit = findViewById(R.id.ParamB);
                pb = edit.getText().toString();
                edit = findViewById(R.id.ParamC);
                pc = edit.getText().toString();

                try {
                    DbHandle db = mService.openDatabase(appName);
                    String currentTableId = "games";
                    oService = new InternalUserDbInterfaceAidlWrapperImpl(mService);
                    nService = new UserDbInterfaceImpl(oService);
                    // Insert into table locally
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("gameid", gameID);
                    contentValues.put("creator", creatorName);
                    contentValues.put("nplayers", playerNum);
                    contentValues.put("nrounds", roundNum);
                    contentValues.put("parama", pa);
                    contentValues.put("paramb", pb);
                    contentValues.put("paramc", pc);
                    contentValues.put("iscompleted", "n");
                    oService.insertRowWithId(appName, db, currentTableId, contentValues, LocalizationUtils.genUUID());
                } catch (RemoteException | ServicesAvailabilityException | ActionNotAuthorizedException e) {
                    e.printStackTrace();
                }
                if(isBound) {
                    unbindService(mConnection);
                    stopService(intent);
                }

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

                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbind and stop service
        if(!serviceRegFlag) {
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
            if(isBound) {
                unbindService(mConnection);
                stopService(intent);
            }
        }
    }
}
