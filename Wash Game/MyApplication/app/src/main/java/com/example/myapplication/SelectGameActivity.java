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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.apache.commons.lang3.ArrayUtils;
import org.opendatakit.database.data.BaseTable;
import org.opendatakit.database.data.Row;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.IDbInterface;
import org.opendatakit.database.service.InternalUserDbInterfaceAidlWrapperImpl;
import org.opendatakit.database.service.UserDbInterfaceImpl;
import org.opendatakit.exception.ServicesAvailabilityException;

import java.util.ArrayList;

public class SelectGameActivity extends AppCompatActivity {

    String playerName, playerID, gameID;
    int nplayers, nrounds, paramA, paramB, paramC;

    boolean serviceRegFlag = false;

    IDbInterface mService = null;
    UserDbInterfaceImpl nService = null;
    InternalUserDbInterfaceAidlWrapperImpl oService;
    private boolean isBound;
    String appName = "default";
    Intent intent = new Intent();
    ServiceConnection mConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_game);

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

        intent.setComponent(new ComponentName("org.opendatakit.services", "org.opendatakit.services.database.service.OdkDatabaseService"));
        intent.setAction(IDbInterface.class.getName());
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        Button selectButton = findViewById(R.id.button_play_game);
        selectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                serviceRegFlag = true;
                EditText edit =  (EditText) findViewById(R.id.EditPlayerName);
                playerName = edit.getText().toString();
                edit =  (EditText) findViewById(R.id.EditPlayerID);
                playerID = edit.getText().toString();
                edit =  (EditText) findViewById(R.id.EditChooseGame);
                gameID = edit.getText().toString();

                try {
                    DbHandle db = mService.openDatabase(appName);
                    String currentTableId = "games";
                    oService = new InternalUserDbInterfaceAidlWrapperImpl(mService);
                    nService = new UserDbInterfaceImpl(oService);
                    // Read table
                    BaseTable table = nService.simpleQuery(appName, db, currentTableId, "gameid="+gameID, null, null, null,
                            null, null, null, null);
                    //int numRows = table.getNumberOfRows();
                    Row tr = table.getRowAtIndex(0);
                    //String gid = String.valueOf(tr.getRawStringByIndex(table.getColumnIndexOfElementKey("gameid")));
                    nplayers = (int) Integer.parseInt(String.valueOf(tr.getRawStringByIndex(table.getColumnIndexOfElementKey("nplayers"))));
                    nrounds = (int) Integer.parseInt(String.valueOf(tr.getRawStringByIndex(table.getColumnIndexOfElementKey("nrounds"))));
                    paramA = (int) Integer.parseInt(String.valueOf(tr.getRawStringByIndex(table.getColumnIndexOfElementKey("parama"))));
                    paramB = (int) Integer.parseInt(String.valueOf(tr.getRawStringByIndex(table.getColumnIndexOfElementKey("paramb"))));
                    paramC = (int) Integer.parseInt(String.valueOf(tr.getRawStringByIndex(table.getColumnIndexOfElementKey("paramc"))));

                } catch (RemoteException | ServicesAvailabilityException e) {
                    e.printStackTrace();
                }

                Intent intentNext = new Intent(SelectGameActivity.this, GameActivity.class);
                intentNext.putExtra("playerName", playerName);
                intentNext.putExtra("playerID", playerID);
                intentNext.putExtra("gameID", gameID);
                intentNext.putExtra("nplayers", nplayers);
                intentNext.putExtra("nrounds", nrounds);
                intentNext.putExtra("paramA", paramA);
                intentNext.putExtra("paramB", paramB);
                intentNext.putExtra("paramC", paramC);
                if(isBound) {
                    unbindService(mConnection);
                    stopService(intent);
                }
                SelectGameActivity.this.startActivity(intentNext);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!serviceRegFlag) {
            //unbind and stop service
            if(isBound) {
                unbindService(mConnection);
                stopService(intent);
            }
        }
    }
}
