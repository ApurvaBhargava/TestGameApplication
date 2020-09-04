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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.opendatakit.database.data.BaseTable;
import org.opendatakit.database.data.Row;
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

public class GameActivity extends AppCompatActivity {

    int round_counter = 1, minSelect, roundScore, totalScore=0;
    int[] costList = {0, 10, 20, 30, 40};
    String selection;

    String playerName, playerID, gameID;
    int nplayers, nrounds, paramA, paramB, paramC;

    IDbInterface mService = null;
    UserDbInterfaceImpl nService = null;
    InternalUserDbInterfaceAidlWrapperImpl oService;
    private boolean isBound;
    String appName = "default";
    Intent intent = new Intent();
    IOdkSyncServiceInterface sService = null;
    private boolean isBoundSync;
    Intent intent_sync = new Intent();
    ServiceConnection sConnection;
    ServiceConnection mConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        Intent intentPrev = getIntent();
        playerName = intentPrev.getStringExtra("playerName");
        playerID = intentPrev.getStringExtra("playerID");
        gameID = intentPrev.getStringExtra("gameID");
        nplayers = intentPrev.getIntExtra("nplayers", -1);
        nrounds = intentPrev.getIntExtra("nrounds", -1);
        paramA = intentPrev.getIntExtra("paramA", -1);
        paramB = intentPrev.getIntExtra("paramB", -1);
        paramC = intentPrev.getIntExtra("paramC", -1);

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

        TextView gameIDHeader = findViewById(R.id.GameIDHeader);
        gameIDHeader.setText("Game ID: " + gameID);

        Button submitButton = findViewById(R.id.button_submit);
        submitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //boolean flagLoop = true;
                round_counter += 1;
                if(round_counter>nrounds) {
                    Intent intentNext = new Intent(GameActivity.this, ResultActivity.class);
                    intentNext.putExtra("totalScore", totalScore);
                    GameActivity.this.startActivity(intentNext);
                }

                RadioGroup radioGroup = findViewById(R.id.RadioGroup01);
                int selectedId = radioGroup.getCheckedRadioButtonId();
                RadioButton radioButton = findViewById(selectedId);
                selection = String.valueOf(radioButton.getTag());

                try {
                    DbHandle db = mService.openDatabase(appName);
                    String currentTableId = "wash";
                    oService = new InternalUserDbInterfaceAidlWrapperImpl(mService);
                    nService = new UserDbInterfaceImpl(oService);
                    // Insert into table locally
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("gameid", gameID);
                    contentValues.put("roundnum", round_counter-1);
                    contentValues.put("playerid", playerID);
                    contentValues.put("playername", playerName);
                    contentValues.put("selection", selection);
                    oService.insertRowWithId(appName, db, currentTableId, contentValues, LocalizationUtils.genUUID());
                } catch (RemoteException | ServicesAvailabilityException | ActionNotAuthorizedException e) {
                    e.printStackTrace();
                }

                while(true) {
                    // run synchronization and wait until sync status changes from SYNCING to something else
                    boolean isSynced;
                    try {
                        isSynced = sService.synchronizeWithServer(appName, SyncAttachmentState.REDUCED_SYNC);
                        Log.i("TAG IS SYNCED", String.valueOf(isSynced));
                        while(true) if(sService.getSyncStatus(appName)!= SyncStatus.SYNCING) break;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    // wait until sync is completed
                    while(true) {
                        try {
                            if(sService.getSyncStatus(appName)!= SyncStatus.NONE) break;
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        DbHandle db = mService.openDatabase(appName);
                        String currentTableId = "wash";
                        oService = new InternalUserDbInterfaceAidlWrapperImpl(mService);
                        nService = new UserDbInterfaceImpl(oService);
                        // Read table
                        BaseTable table = nService.simpleQuery(appName, db, currentTableId,
                                "gameid=" + gameID + " AND roundnum=" + String.valueOf(round_counter - 1), null, null, null,
                                null, null, null, null);
                        if (table.getNumberOfRows() >= nplayers){
                            Log.i("CHECKOUTTTT", String.valueOf(table.getNumberOfRows()));
                            break;
                        }
                        sService.clearAppSynchronizer(appName);
                    }
                    catch (RemoteException | ServicesAvailabilityException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    DbHandle db = mService.openDatabase(appName);
                    String currentTableId = "wash";
                    oService = new InternalUserDbInterfaceAidlWrapperImpl(mService);
                    nService = new UserDbInterfaceImpl(oService);
                    // Read table
                    BaseTable table = nService.simpleQuery(appName, db, currentTableId,
                            "gameid="+gameID+" AND roundnum="+String.valueOf(round_counter-1), null, null, null,
                            null, null, null, null);
                    int numRows = table.getNumberOfRows();
                    minSelect = 99;
                    int temp;
                    for(int i=0; i<numRows; i++) {
                        Row tr = table.getRowAtIndex(i);
                        String opt = String.valueOf(tr.getRawStringByIndex(table.getColumnIndexOfElementKey("selection")));
                        temp = Integer.parseInt(opt.substring(1));
                        if(temp < minSelect)
                            minSelect = temp;
                    }
                } catch (RemoteException | ServicesAvailabilityException e) {
                    e.printStackTrace();
                }
                roundScore = paramA*costList[minSelect-1] - paramB*costList[Integer.parseInt(selection.substring(1))-1] + paramC;
                totalScore += roundScore;
                TextView roundHeader = findViewById(R.id.RoundHeader);
                roundHeader.setText("Round: " + round_counter);
                TextView grpMinVal = findViewById(R.id.GroupMinimumValue);
                grpMinVal.setText(Integer.toString(costList[minSelect-1]));
                TextView roundScoreVal = findViewById(R.id.RoundScoreValue);
                roundScoreVal.setText(Integer.toString(roundScore));
                TextView totScoreVal = findViewById(R.id.TotalScoreValue);
                totScoreVal.setText(Integer.toString(totalScore));
                try {
                    sService.clearAppSynchronizer(appName);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        if(isBound) {
            unbindService(mConnection);
            stopService(intent);
        }
    }
}
