package com.example.myapplication;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.opendatakit.database.data.BaseTable;
import org.opendatakit.database.data.Row;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.IDbInterface;
import org.opendatakit.database.service.InternalUserDbInterfaceAidlWrapperImpl;
import org.opendatakit.database.service.UserDbInterfaceImpl;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.sync.service.IOdkSyncServiceInterface;
import org.opendatakit.sync.service.SyncAttachmentState;
import org.opendatakit.sync.service.SyncStatus;
import org.opendatakit.utilities.ODKFileUtils;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class UpdateIntentService extends IntentService {
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_UPDATE = "com.example.myapplication.action.update";
    private static final String ACTION_SOMETHING_ELSE = "com.example.myapplication.action.somethingElse";

    private int imgNum;

    private IDbInterface mService = null;
    private UserDbInterfaceImpl nService = null;
    InternalUserDbInterfaceAidlWrapperImpl oService;
    private boolean isBound;
    private String appName = "default";
    private Intent intent = new Intent();

    final ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Following the example above for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            mService = IDbInterface.Stub.asInterface(service);
            Log.i("TAG", "Db Service connected");
            isBound = true;
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.i("TAG", "Db Service disconnected due to unexplained causes.");
            mService = null;
        }
    };

    public UpdateIntentService() {
        super("SyncIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPDATE.equals(action)) {
                while (true) {
                    /*try {
                        intent.setComponent(new ComponentName("org.opendatakit.services", "org.opendatakit.services.database.service.OdkDatabaseService"));
                        intent.setAction(IDbInterface.class.getName());
                        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                        while(true) {
                            Log.i("here", "stuck");
                            if(isBound) break;
                        }
                        DbHandle db = mService.openDatabase(appName);
                        String currentTableId = "game";
                        oService = new InternalUserDbInterfaceAidlWrapperImpl(mService);
                        nService = new UserDbInterfaceImpl(oService);
                        String userName = oService.getActiveUser(appName);
                        // read row from table
                        BaseTable table = nService.simpleQuery(appName, db, currentTableId, null, null, null, null,
                                null, null, null, null);
                        int last_row_index = table.getNumberOfRows()-1;
                        JSONArray userListObject = new JSONArray(oService.getUsersList(appName));
                        String[] usersList = new String[userListObject.length()];
                        String otherUser = "";
                        for(int i=0; i<userListObject.length(); i++) {
                            usersList[i] = userListObject.getJSONObject(i).getString("user_id");
                            if(!usersList[i].equals(userName)) {
                                if(usersList[i].equals("username:apurvab") || usersList[i].equals("username:amoon"))
                                    otherUser = usersList[i];
                            }
                        }
                        for(int i=last_row_index; i>=0; i--)  {
                            Row tr = table.getRowAtIndex(i);
                            if(otherUser.equals(String.valueOf(tr.getRawStringByIndex(table.getColumnIndexOfElementKey("name"))))) {
                                imgNum = (int) String.valueOf(tr.getRawStringByIndex(table.getColumnIndexOfElementKey("option"))).charAt(1)-1;
                                break;
                            }
                        }
                        unbindService(mConnection);
                        stopService(intent);
                        Log.i("TAG", "Service unbounded.");
                        isBound = false;
                    } catch (RemoteException | ServicesAvailabilityException | JSONException e) {
                        e.printStackTrace();
                    }*/
                    imgNum = 0 + (int)(Math.random() * 5);
                    intent.putExtra("imgOption", imgNum);
                    sendBroadcast(intent);
                }
            } else if (ACTION_SOMETHING_ELSE.equals(action)) {
                Log.e("IMPLEMENTATION NOTE:", "Not implemented yet");
            }
        }
    }
}
