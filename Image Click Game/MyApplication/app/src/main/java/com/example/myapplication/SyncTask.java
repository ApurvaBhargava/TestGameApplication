package com.example.myapplication;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.opendatakit.sync.service.IOdkSyncServiceInterface;
import org.opendatakit.sync.service.SyncAttachmentState;
import org.opendatakit.sync.service.SyncStatus;
import org.opendatakit.utilities.ODKFileUtils;

public class SyncTask extends AsyncTask {

    private Context context;

    public SyncTask(Context context){
        this.context = context;
    }

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

    @Override
    protected Object doInBackground(Object[] objects) {
            String appName = ODKFileUtils.getOdkDefaultAppName();
            intent_sync.setComponent(new ComponentName("org.opendatakit.services", "org.opendatakit.services.sync.service.OdkSyncService"));
            intent_sync.setAction(IOdkSyncServiceInterface.class.getName());
            context.bindService(intent_sync, mConnection, Context.BIND_AUTO_CREATE);
            //wait until bindService completes
            while (true) if (isBound) break;
            // run synchronization and wait until sync status changes from SYNCING to something else
            try {
                boolean isSynced = sService.synchronizeWithServer(appName, SyncAttachmentState.REDUCED_SYNC);
                Log.i("TAG IS_SYNCED", String.valueOf(isSynced));
                while(true) if(sService.getSyncStatus(appName)!= SyncStatus.SYNCING) break;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            //unbind and stop service
            context.unbindService(mConnection);
            boolean hasStopped = context.stopService(intent_sync);
            // wait until service has stopped
            while(true) {
                if(hasStopped) {
                    Log.i("TAG", "Service unbounded.");
                    isBound = false;
                    break;
                }
            }
        return null;
    }
}