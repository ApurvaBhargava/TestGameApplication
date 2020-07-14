package com.example.myapplication;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.viewpager.widget.PagerAdapter;

import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.IDbInterface;
import org.opendatakit.database.service.InternalUserDbInterfaceAidlWrapperImpl;
import org.opendatakit.database.service.UserDbInterfaceImpl;
import org.opendatakit.exception.ActionNotAuthorizedException;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.utilities.LocalizationUtils;

import java.util.Calendar;

public class ImageAdapter extends PagerAdapter {

    IDbInterface mService = null;
    UserDbInterfaceImpl nService = null;
    InternalUserDbInterfaceAidlWrapperImpl oService;
    private boolean isBound;
    String appName = "default";
    Intent intent = new Intent();

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

    private Context mContext;
    private int[] mImageIds = new int[]{R.drawable.cat, R.drawable.dog, R.drawable.horse, R.drawable.dolphin, R.drawable.phoenix, R.drawable.dragon};
    ImageAdapter(Context context) {
        mContext = context;
    }
    @Override
    public int getCount() {
        return mImageIds.length;
    }
    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        final ImageView imageView = new ImageView(mContext);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(mImageIds[position]);

        intent.setComponent(new ComponentName("org.opendatakit.services", "org.opendatakit.services.database.service.OdkDatabaseService"));
        intent.setAction(IDbInterface.class.getName());
        mContext.getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        isBound = true;

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.e("CLICK_TEST", "N"+String.valueOf(position));
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
                    contentValues.put("option", "a"+String.valueOf(position+1));
                    contentValues.put("date", date);
                    contentValues.put("time", time);
                    //oService.insertLocalOnlyRow(appName, db, currentTableId, contentValues);
                    oService.insertRowWithId(appName, db, currentTableId, contentValues, LocalizationUtils.genUUID());
                } catch (RemoteException | ServicesAvailabilityException | ActionNotAuthorizedException e) {
                    e.printStackTrace();
                }
            }
        });
        container.addView(imageView, 0);
        return imageView;
    }
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((ImageView) object);
    }
}