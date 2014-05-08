package se.lundakarnevalen.extern.map;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.RectF;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import se.lundakarnevalen.extern.util.Logf;

/**
 * Created by Filip on 2014-05-07.
 */
public class GPSTracker extends Service implements LocationListener {
    private static final String LOG_TAG = GPSTracker.class.getSimpleName();
    public static final int UPDATE_PERIOD = 10000;

    private final LocationManager mLocationManager;
    private final Context mContext;
    private final Timer mTimer;

    public void invalidateMe(GPSListener listener) {
        listener.onNewLocation(latitude, longitude);
    }

    public interface GPSListener {
        public void onNewLocation(double lat, double lng);
    }

    boolean canGetLocation = false;

    List<GPSListener> mListeners;
    Location location;
    double latitude;
    double longitude;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 2 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 8000; // 1 sec

    public GPSTracker(Context context) {
        this.mContext = context;
        this.mLocationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
        this.mListeners = new ArrayList<GPSListener>(2);
        this.mTimer = new Timer();
        this.mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                new Handler(mContext.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        updateLocation();
                    }
                });
            }
        }, 2000, UPDATE_PERIOD);
    }

    public Location updateLocation() {
        try {
            // if GPS Enabled get lat/long using GPS Services
            if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                Log.d(LOG_TAG, "LocationProvider: GPS Enabled, requesting location");
                this.canGetLocation = true;
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            } else if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

                Log.d(LOG_TAG, "LocationProvider: Mobile Enabled, requesting location");
                this.canGetLocation = true;
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            } else {
                Criteria c = new Criteria();
                final String bestProvider = mLocationManager.getBestProvider(c, false);

                Logf.d(LOG_TAG, "LocationProvider: %s, requesting location", bestProvider);
                this.canGetLocation = true;
                mLocationManager.requestLocationUpdates(bestProvider, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                location = mLocationManager.getLastKnownLocation(bestProvider);

            }

            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                Logf.d(LOG_TAG, "New position: %f, %f", latitude, longitude);
                onLocationChanged(location);
            }

        } catch (Exception e) {
            Log.wtf(LOG_TAG, "Failed to acquire location", e);
            e.printStackTrace();
        }
        return location;
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     */
    public void stopUsingGPS() {
        mTimer.cancel();
        if(mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }
    }

    public void addListener(GPSListener listener) {
        this.mListeners.add(listener);
    }

    public void removeListener(GPSListener listener) {
        this.mListeners.remove(listener);
    }

    private static final RectF LUNDAGARD = new RectF(55.70900f, 13.1874f, 55.70301f, 13.1991f);

    @Override
    public void onLocationChanged(Location location) {
        final double lat = location.getLatitude();
        final double lng = location.getLongitude();
        if (LUNDAGARD.contains((float) lat, (float) lng)) {
            // We only care if inside LUNDAGARD
            Logf.d(LOG_TAG, "(%s) Posting location: %f, %f", location.getProvider(), lat, lng);
            this.location = location;
        } else {
            Logf.d(LOG_TAG, "(%s) Ignoring location: %f, %f", location.getProvider(), lat, lng);
        }

        for (GPSListener l : mListeners) {
            Logf.d(LOG_TAG, "Delivering location to %s: %f, %f", l, lat, lng);
            l.onNewLocation(lat, lng);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.wtf(LOG_TAG, "Current Provider is disabled: "+provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.wtf(LOG_TAG, "Current Provider is enabled: "+provider);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Logf.wtf(LOG_TAG, "Current Provider Status: %s, %d, %s",provider, status, extras);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setTitle("GPS settings");
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");
        //alertDialog.setIcon(R.drawable.delete);
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }
}