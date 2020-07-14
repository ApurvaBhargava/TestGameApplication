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

import org.json.JSONArray;
import org.json.JSONException;
import org.opendatakit.database.data.BaseTable;
import org.opendatakit.database.data.Row;
import org.opendatakit.database.service.IDbInterface;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.InternalUserDbInterfaceAidlWrapperImpl;
import org.opendatakit.database.service.UserDbInterfaceImpl;
import org.opendatakit.exception.ActionNotAuthorizedException;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.utilities.LocalizationUtils;

import java.util.Calendar;

public class TableActivity extends AppCompatActivity {

    IDbInterface mService = null;
    UserDbInterfaceImpl nService = null;
    InternalUserDbInterfaceAidlWrapperImpl oService;
    private boolean isBound;
    String appName = "default";
    Intent intent = new Intent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table);

        final ServiceConnection mConnection = new ServiceConnection() {
            // Called when the connection with the service is established
            public void onServiceConnected(ComponentName className, IBinder service) {
                // Following the example above for an AIDL interface,
                // this gets an instance of the IRemoteInterface, which we can use to call on the service
                mService = IDbInterface.Stub.asInterface(service);
                Log.i("TAG", "Service connected");
            }

            // Called when the connection with the service disconnects unexpectedly
            public void onServiceDisconnected(ComponentName className) {
                Log.i("TAG", "Service disconnected due to unexplained causes.");
                mService = null;
            }
        };

        Button bindButton = findViewById(R.id.bind_table);
        bindButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                intent.setComponent(new ComponentName("org.opendatakit.services", "org.opendatakit.services.database.service.OdkDatabaseService"));
                intent.setAction(IDbInterface.class.getName());
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                isBound = true;
            }
        });

        Button unbindButton = findViewById(R.id.unbind_table);
        unbindButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                if(isBound) {
                    unbindService(mConnection);
                    stopService(intent);
                    Log.i("TAG", "Service unbounded.");
                    isBound = false;
                }
                else {
                    Log.i("TAG", "Service isn't bound in the first place.");
                }
            }
        });

        Button displayButton = findViewById(R.id.display_row);
        displayButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                try {
                    DbHandle db = mService.openDatabase(appName);
                    String currentTableId = "game";
                    oService = new InternalUserDbInterfaceAidlWrapperImpl(mService);
                    nService = new UserDbInterfaceImpl(oService);
                    String userName = oService.getActiveUser(appName);
                    // read row from table
                    BaseTable table = nService.simpleQuery(appName, db, currentTableId, null, null, null, null,
                            null, null, null, null);
                    //Log.i("NUMBER OF ROWS", String.valueOf(table.getNumberOfRows()));
                    int last_row_index = table.getNumberOfRows()-1;
                    JSONArray userListObject = new JSONArray(oService.getUsersList(appName));
                    String[] usersList = new String[userListObject.length()];
                    String otherUser = "";
                    for(int i=0; i<userListObject.length(); i++) {
                        usersList[i] = userListObject.getJSONObject(i).getString("user_id");
                        if(!usersList[i].equals(userName)) {
                            if(usersList[i]=="username:apurvab" || usersList[i]=="username:amoon")
                                otherUser = usersList[i];
                        }
                    }
                    for(int i=last_row_index; i>=0; i--)  {
                        Row tr = table.getRowAtIndex(i);
                        if("username:apurvab".equals(String.valueOf(tr.getRawStringByIndex(table.getColumnIndexOfElementKey("name"))))) {
                            Log.e("IMAGE OPTION", i+" "+String.valueOf(tr.getRawStringByIndex(table.getColumnIndexOfElementKey("option"))));
                        }
                    }
                    //Row tr = table.getRowAtIndex(table.getNumberOfRows()-1);
                    //Log.i("ROW STRING name", String.valueOf(tr.getRawStringByIndex(table.getColumnIndexOfElementKey("name"))));
                    //Log.i("ROW STRING option", String.valueOf(tr.getRawStringByIndex(table.getColumnIndexOfElementKey("option"))));
                    //Log.i("ROW STRING date", String.valueOf(tr.getRawStringByIndex(table.getColumnIndexOfElementKey("date"))));
                    //Log.i("ROW STRING time", String.valueOf(tr.getRawStringByIndex(table.getColumnIndexOfElementKey("time"))));
                } catch (RemoteException | ServicesAvailabilityException | JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Button addButton = findViewById(R.id.add_row);
        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                try {
                    DbHandle db = mService.openDatabase(appName);
                    String currentTableId = "game";
                    oService = new InternalUserDbInterfaceAidlWrapperImpl(mService);
                    nService = new UserDbInterfaceImpl(oService);
                    // Insert into table locally
                    Calendar currentTime = Calendar.getInstance();
                    String date = currentTime.get(Calendar.DAY_OF_MONTH) + "/" + (currentTime.get(Calendar.MONTH)+1) + "/" + currentTime.get(Calendar.YEAR);
                    String time = currentTime.get(Calendar.HOUR_OF_DAY) + ":" + currentTime.get(Calendar.MINUTE) + ":" + currentTime.get(Calendar.SECOND);
                    String userName = oService.getActiveUser(appName);
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("name", userName);
                    contentValues.put("option", "a3");
                    contentValues.put("date", date);
                    contentValues.put("time", time);
                    //oService.insertLocalOnlyRow(appName, db, currentTableId, contentValues);
                    oService.insertRowWithId(appName, db, currentTableId, contentValues, LocalizationUtils.genUUID());
                } catch (RemoteException | ServicesAvailabilityException | ActionNotAuthorizedException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}