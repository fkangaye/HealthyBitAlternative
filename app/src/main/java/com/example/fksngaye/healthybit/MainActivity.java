package com.example.fksngaye.healthybit;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.mbientlab.metawear.AsyncOperation.CompletionHandler;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.RouteManager.MessageHandler;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.Bmi160Accelerometer;
import com.mbientlab.metawear.module.Bmi160Gyro;
import com.mbientlab.metawear.module.Bmi160Gyro.*;
import com.mbientlab.metawear.AsyncOperation;


import static com.mbientlab.metawear.MetaWearBoard.ConnectionStateHandler;


public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private static final String TAG = "METAWEAR";
    public final static String EXTRA_BT_DEVICE= "com.example.fksngaye.healthybit.MainActivity.EXTRA_BT_DEVICE";
    private Switch sample_control;
    private Accelerometer accelModule;
    private static final float ACC_RANGE = 8.f, ACC_FREQ = 50.f;
    private static final String STREAM_KEY = "accel_stream";

    //Metawear objects
    private MetaWearBleService.LocalBinder serviceBinder;
    private MetaWearBoard metaWearBoard;
    private Led ledModule;

    private static final String LOG_KEY = "accel_log";
    private Logging loggingModule;


    private final ConnectionStateHandler stateHandler = new ConnectionStateHandler() {
        @Override
        public void connected() {
            Log.i(TAG, "Connected");
            try {
                accelModule = metaWearBoard.getModule(Accelerometer.class);
                loggingModule = metaWearBoard.getModule(Logging.class);
            } catch (UnsupportedModuleException e) {
                e.printStackTrace();
            }
            sample_control = (Switch) findViewById(R.id.sample_control);
                sample_control.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Log.i("Switch State=", "" + isChecked);
                        if (isChecked) {
                            accelModule.setOutputDataRate(ACC_FREQ);
                            accelModule.setAxisSamplingRange(ACC_RANGE);
                            accelModule.routeData()
                                    .fromAxes().stream(LOG_KEY)
                                    .commit().onComplete(new CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
//                                    result.subscribe(STREAM_KEY, new MessageHandler() {
//                                        @Override
//                                        public void process(com.mbientlab.metawear.Message
// message) {
//                                            CartesianFloat axes = message.getData
// (CartesianFloat.class);
//                                            Log.i(TAG, axes.toString());
//                                        }
//                                    });

                                    result.setLogMessageHandler(LOG_KEY, new MessageHandler() {
                                        @Override
                                        public void process(Message message) {
                                            final CartesianFloat axisData = message.getData
                                                    (CartesianFloat.class);
                                            Log.i(TAG, String.format("Log: %s", axisData.toString
                                                    ()));
                                        }
                                    });
                                }

                                @Override
                                public void failure(Throwable error) {
                                    Log.e(TAG, "Error committing route", error);
                                }
                            });
                            loggingModule.startLogging();
                            accelModule.enableAxisSampling();
                            accelModule.start();
                        } else {
                            loggingModule.stopLogging();
                            accelModule.disableAxisSampling();
                            accelModule.stop();
                            loggingModule.downloadLog(0.05f, new Logging.DownloadHandler() {
                                @Override
                                public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                                    Log.i(TAG, String.format("Progress= %d / %d", nEntriesLeft,
                                            totalEntries));
                                }
                            });
                            Log.i(TAG, "Log size: " + loggingModule.getLogCapacity());
                        }
                    }
                });
           }

//        @Override
//        public void disconnected() {
//            super.disconnected();
//            Log.i(TAG, "Connection Lost");
//        }
//
//        @Override
//        public void failure(int status, Throwable error) {
//            super.failure(status, error);
//            Log.i(TAG, "Error Connection", error);
//        }
    };
//    public void retrieveBoard() {
//
//        // Create a MetaWear board object for the Bluetooth Device
//        //mwBoard= serviceBinder.getMetaWearBoard(remoteDevice);
//        metaWearBoard.setConnectionStateHandler(stateHandler);
//
//
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getApplicationContext().unbindService(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                this, Context.BIND_AUTO_CREATE);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        serviceBinder = (MetaWearBleService.LocalBinder) service;
        metaWearBoard.setConnectionStateHandler(stateHandler);

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }
}
