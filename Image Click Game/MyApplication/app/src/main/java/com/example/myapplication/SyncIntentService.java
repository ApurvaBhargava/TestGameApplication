package com.example.myapplication;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.opendatakit.sync.service.IOdkSyncServiceInterface;
import org.opendatakit.sync.service.SyncAttachmentState;
import org.opendatakit.sync.service.SyncStatus;
import org.opendatakit.utilities.ODKFileUtils;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class SyncIntentService extends IntentService {
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_SYNC = "com.example.myapplication.action.sync";
    private static final String ACTION_RESET_SERVER = "com.example.myapplication.action.resetServer";

    IOdkSyncServiceInterface sService = null;
    private boolean isBound;
    Intent intent_sync = new Intent();

    final ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Following the example above for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            sService = IOdkSyncServiceInterface.Stub.asInterface(service);
            Log.i("TAG", "Service connected");
            isBound = true;
        }
        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.i("TAG", "Service disconnected due to unexplained causes.");
        }
    };

    public SyncIntentService() {
        super("SyncIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SYNC.equals(action)) {
                while (true) {
                    String appName = ODKFileUtils.getOdkDefaultAppName();
                    intent_sync.setComponent(new ComponentName("org.opendatakit.services", "org.opendatakit.services.sync.service.OdkSyncService"));
                    intent_sync.setAction(IOdkSyncServiceInterface.class.getName());
                    bindService(intent_sync, mConnection, Context.BIND_AUTO_CREATE);
                    //wait until bindService completes
                    while (true) if (isBound) break;
                    // run synchronization and wait until sync status changes from SYNCING to something else
                    try {
                        boolean isSynced = sService.synchronizeWithServer(appName, SyncAttachmentState.REDUCED_SYNC);
                        Log.i("TAG IS SYNCED", String.valueOf(isSynced));
                        while(true) if(sService.getSyncStatus(appName)!=SyncStatus.SYNCING) break;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    //unbind and stop service
                    unbindService(mConnection);
                    boolean hasStopped = stopService(intent_sync);
                    // wait until service has stopped
                    while(true) {
                        if(hasStopped) {
                            Log.i("TAG", "Service unbounded.");
                            isBound = false;
                            break;
                        }
                    }
                    // check most recent sync status, if it is not SYNC_COMPLETE, find what is the issue
                    /*try {
                        Log.i("TAG SYNC STATUS", String.valueOf(sService.getSyncStatus(appName)));
                        if(sService.getSyncStatus(appName)==SyncStatus.SERVER_IS_NOT_ODK_SERVER) {
                            Log.i("SERVER CONFIG TEST", String.valueOf(sService.verifyServerSettings(appName)));
                            
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }*/
                }
            } else if (ACTION_RESET_SERVER.equals(action)) {
                    Log.e("IMPLEMENTATION NOTE:", "Not implemented yet");
            }
        }
    }
}
