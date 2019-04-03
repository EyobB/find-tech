package com.internalpositioning.find3.find3app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements TasksAPI.Response {

    // logging
    private final String TAG = "MainActivity";

    // background manager
    private PendingIntent recurringLl24 = null;
    private Intent ll24 = null;
    AlarmManager alarms = null;
    WebSocketClient mWebSocketClient = null;
    Timer timer = null;
    private RemindTask oneSecondTimer = null;

    private String[] autocompleteLocations = new String[] {"Central-Pharmacy","Omni-south"};

    EditText find3ApiEdit;
    EditText workflowApiEdit;
    EditText tokenEdit;
    EditText intervalEditText;
    TextView currentLocationTextView;
    LinearLayout advancedOptionsContainer;
    ToggleButton toggleScanType;
    TextView textTasks;
    EditText familyNameEdit;
    EditText deviceNameEdit;

    SendListener sendListenerService;
    String tasksApiUrl;
    TaskListAdapter taskListAdapter;

    MainActivity thisInstance;

    @Override
    protected void onDestroy() {
        Log.d(TAG, "MainActivity onDestroy()");
        if (alarms != null) alarms.cancel(recurringLl24);
        if (timer != null) timer.cancel();
        if (mWebSocketClient != null) {
            mWebSocketClient.close();
        }
        android.app.NotificationManager mNotificationManager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0);
        Intent scanService = new Intent(this, ScanService.class);
        stopService(scanService);
        super.onDestroy();
    }

    @Override
    public void processFinished(TasksAPI.Task[] tasks) {
        Toast.makeText(getApplicationContext(),"Tasks updated.", Toast.LENGTH_SHORT).show();
        List<TasksAPI.Task> tasksList = Arrays.asList(tasks);
        boolean isTaskModified = hasDataChanged(tasksList);
        if(isTaskModified) {
            taskListAdapter.setTasks(tasksList);
            taskListAdapter.notifyDataSetChanged();
            try {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //deprecated in API 26
                    v.vibrate(500);
                }


                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class RemindTask extends TimerTask {
        private Integer counter = 0;

        public void resetCounter() {
            counter = 0;
        }
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    counter++;
                    if (mWebSocketClient != null) {
                        if (mWebSocketClient.isClosed()) {
                            connectWebSocket();
                        }
                    }
                    TextView rssi_msg = (TextView) findViewById(R.id.textOutput);
                    String currentText = rssi_msg.getText().toString();
                    if (currentText.contains("ago: ")) {
                        String[] currentTexts = currentText.split("ago: ");
                        currentText = currentTexts[1];
                    }
                    rssi_msg.setText(counter + " seconds ago: " + currentText);
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check permissions
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_WIFI_STATE}, 1);
        }

        intervalEditText = (EditText)findViewById(R.id.intervalEditText);
        currentLocationTextView =  (TextView)findViewById(R.id.currentLocation);
        familyNameEdit = (EditText) findViewById(R.id.familyName);
        deviceNameEdit = (EditText) findViewById(R.id.deviceName);
        find3ApiEdit = (EditText) findViewById(R.id.serverAddress);
        workflowApiEdit = (EditText) findViewById(R.id.workflowApi);
        tokenEdit = (EditText) findViewById(R.id.token);
        advancedOptionsContainer = (LinearLayout)findViewById(R.id.advancedOptionsContainer);
        toggleScanType = (ToggleButton)findViewById(R.id.toggleScanType);
        textTasks = (TextView)findViewById(R.id.textTasks);

        sendListenerService = new SendListener() {
            @Override
            public void Sent(Date date, int wifiLength) {
                SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm:ss");//dd/MM/yyyy
                Date now = new Date();
                String nowString = sdfDate.format(now);
                ((TextView)findViewById(R.id.textTasks)).append(nowString + ": Data sent: "+ wifiLength +" \n");
                Log.i(TAG, nowString + ": Data sent: "+ wifiLength);

            }

            @Override
            public void Acked(Date date) {
                SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm:ss");//dd/MM/yyyy
                Date now = new Date();
                String nowString = sdfDate.format(now);
                ((TextView)findViewById(R.id.textTasks)).append(nowString + ": Data acknowledged"+" \n");
                Log.i(TAG, nowString + ": Data acknowledged");
            }

            @Override
            public void ScanStarted(Date date) {
                SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm:ss");//dd/MM/yyyy
                Date now = new Date();
                String nowString = sdfDate.format(now);
                ((TextView)findViewById(R.id.textTasks)).append(nowString + ": Scan started"+" \n");
                Log.i(TAG, nowString + ": Scan started");

            }
        };
        Broadcaster broadcaster = new Broadcaster();
        broadcaster.addListener(sendListenerService);

        TextView rssi_msg = (TextView) findViewById(R.id.textOutput);
        rssi_msg.setText("not running");

        loadPreferences();
        setupTaskList();

        AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.locationName);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, autocompleteLocations);
        textView.setAdapter(adapter);

        thisInstance = this;

        ToggleButton toggleButtonTracking = (ToggleButton) findViewById(R.id.toggleScanType);
        toggleButtonTracking.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TextView rssi_msg = (TextView) findViewById(R.id.textOutput);
                rssi_msg.setText("not running");
                Log.d(TAG, "toggle set to false");
                if (alarms != null) alarms.cancel(recurringLl24);
                android.app.NotificationManager mNotificationManager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.cancel(0);
                if (timer != null) timer.cancel();

                CompoundButton scanButton = (CompoundButton) findViewById(R.id.toggleButton);
                scanButton.setChecked(false);
            }
        });

        findViewById(R.id.logoImageView).setOnClickListener(new DoubleClickListener() {
            @Override
            public void onSingleClick(View v) {

            }

            @Override
            public void onDoubleClick(View v) {
                if(advancedOptionsContainer.getVisibility() == View.VISIBLE) {
                    advancedOptionsContainer.setVisibility(View.GONE);
                    toggleScanType.setVisibility(View.GONE);
                }
                else{
                    advancedOptionsContainer.setVisibility(View.VISIBLE);
                    toggleScanType.setVisibility(View.VISIBLE);
                }
            }
        });

        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    TextView rssi_msg = (TextView) findViewById(R.id.textOutput);
                    String familyName = ((EditText) findViewById(R.id.familyName)).getText().toString().toLowerCase();
                    if (familyName.equals("")) {
                        rssi_msg.setText("family name cannot be empty");
                        buttonView.toggle();
                        return;
                    }

                    String find3Api = find3ApiEdit.getText().toString();
                    if (find3Api.equals("")) {
                        rssi_msg.setText("Find3 address cannot be empty");
                        buttonView.toggle();
                        return;
                    }
                    if (find3Api.contains("http")!=true) {
                        rssi_msg.setText("Find3 address must include http or https in server name");
                        buttonView.toggle();
                        return;
                    }

                    final String workflowApi = workflowApiEdit.getText().toString();
                    if (workflowApi.equals("")) {
                        rssi_msg.setText("Workflow address cannot be empty");
                        buttonView.toggle();
                        return;
                    }
                    if (workflowApi.contains("http")!=true) {
                        rssi_msg.setText("Workflow address must include http or https in server name");
                        buttonView.toggle();
                        return;
                    }

                    int interval = getInteger(intervalEditText.getText().toString(), -1);
                    if (interval < 1) {
                        rssi_msg.setText("interval must be a positive integer.");
                        return;
                    }

                    final String token = tokenEdit.getText().toString();
                    if (token.equals("")) {
                        rssi_msg.setText("Token cannot be empty");
                        return;
                    }

                    String deviceName = ((EditText) findViewById(R.id.deviceName)).getText().toString().toLowerCase();
                    if (deviceName.equals("")) {
                        rssi_msg.setText("device name cannot be empty");
                        buttonView.toggle();
                        return;
                    }

                    boolean allowGPS = ((CheckBox) findViewById(R.id.allowGPS)).isChecked();
                    Log.d(TAG,"allowGPS is checked: "+allowGPS);
                    String locationName = ((EditText) findViewById(R.id.locationName)).getText().toString().toLowerCase();

                    CompoundButton trackingButton = (CompoundButton) findViewById(R.id.toggleScanType);
                    if (trackingButton.isChecked() == false) {
                        locationName = "";
                    } else {
                        if (locationName.equals("")) {
                            rssi_msg.setText("location name cannot be empty when learning");
                            buttonView.toggle();
                            return;
                        }
                    }

                    updatePreferences(familyName, find3Api, workflowApi, token, interval, deviceName, allowGPS, locationName);
                    tasksApiUrl = workflowApi + (workflowApi.endsWith("/")?"":"/") + deviceName;

                    startTaskMonitoring(interval, token);

                    rssi_msg.setText("running");
                    // 24/7 alarm
                    ll24 = new Intent(MainActivity.this, AlarmReceiverLife.class);
                    Log.d(TAG, "setting familyName to [" + familyName + "]");
                    ll24.putExtra("familyName", familyName);
                    ll24.putExtra("deviceName", deviceName);
                    ll24.putExtra("serverAddress", find3Api);
                    ll24.putExtra("locationName", locationName);
                    ll24.putExtra("allowGPS",allowGPS);

                    recurringLl24 = PendingIntent.getBroadcast(MainActivity.this, 0, ll24, PendingIntent.FLAG_CANCEL_CURRENT);
                    alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    alarms.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.currentThreadTimeMillis(), 10000, recurringLl24);
                    Timer timer = new Timer();
                    oneSecondTimer = new RemindTask();
                    timer.scheduleAtFixedRate(oneSecondTimer, 1000, 1000);
                    connectWebSocket();

                    String scanningMessage = "Scanning for " + familyName + "/" + deviceName;
                    if (locationName.equals("") == false) {
                        scanningMessage += " at " + locationName;
                    }
                    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MainActivity.this)
                            .setSmallIcon(R.drawable.ic_stat_name)
                            .setContentTitle(scanningMessage)
                            .setContentIntent(recurringLl24);
                    //specifying an action and its category to be triggered once clicked on the notification
                    Intent resultIntent = new Intent(MainActivity.this, MainActivity.class);
                    resultIntent.setAction("android.intent.action.MAIN");
                    resultIntent.addCategory("android.intent.category.LAUNCHER");
                    PendingIntent resultPendingIntent = PendingIntent.getActivity(MainActivity.this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    notificationBuilder.setContentIntent(resultPendingIntent);

                    android.app.NotificationManager notificationManager =
                            (android.app.NotificationManager) MainActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());

                    final TextView myClickableUrl = (TextView) findViewById(R.id.textInstructions);
                    myClickableUrl.setText("See your results in realtime: " + find3Api + "/view/location/" + familyName + "/" + deviceName);
                    Linkify.addLinks(myClickableUrl, Linkify.WEB_URLS);
                } else {
                    TextView rssi_msg = (TextView) findViewById(R.id.textOutput);
                    rssi_msg.setText("not running");
                    Log.d(TAG, "toggle set to false");
                    alarms.cancel(recurringLl24);
                    android.app.NotificationManager mNotificationManager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.cancel(0);
                    timer.cancel();
                }
            }
        });
    }

    private void setupTaskList() {

        List<TasksAPI.Task> list = new ArrayList<TasksAPI.Task>();
        taskListAdapter = new TaskListAdapter(getBaseContext(), list);
        final ListView listview = (ListView) findViewById(R.id.listview);
        listview.setAdapter(taskListAdapter);
    }

    private void startTaskMonitoring(int interval, final String token) {
        final Handler handler = new Handler();
        Timer timer = new Timer(false);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        TasksAPI tasksApi =  new TasksAPI();
                        tasksApi.delegate = thisInstance;
                        tasksApi.execute(tasksApiUrl, token);
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(timerTask, interval * 3000, interval * 1000);
    }

    private void reloadTasks(List<TasksAPI.Task> tasks) {
        if(hasDataChanged(tasks)) {
            taskListAdapter.setTasks(tasks);
            taskListAdapter.notifyDataSetChanged();
        }
    }

    private boolean hasDataChanged(List<TasksAPI.Task> tasks) {
        List<TasksAPI.Task> oldTasks = taskListAdapter.getTasks();
        final HashMap<UUID, TasksAPI.Task> oldTasksHashSet = new HashMap<>();
        for (TasksAPI.Task t : oldTasks) {
            oldTasksHashSet.put(t.getId(), t);
        }

        if (oldTasks.size() != tasks.size())
            return true;

        for (TasksAPI.Task task : tasks) {
            TasksAPI.Task oldTask = oldTasksHashSet.get(task.getId());
            if (oldTask == null)
                return true;
        }
        return false;
    }

    private void loadPreferences() {
        // check to see if there are preferences
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        familyNameEdit.setText(sharedPref.getString("familyName", "truefalses"));
        deviceNameEdit.setText(sharedPref.getString("deviceName", ""));
        find3ApiEdit.setText(sharedPref.getString("find3Api","http://13.59.114.154:8003"));
        workflowApiEdit.setText(sharedPref.getString("workflowApi","https://teg7wev413.execute-api.us-west-2.amazonaws.com/Prod/v1/tasks/tech"));
        tokenEdit.setText(sharedPref.getString("token","eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiIyMGQyNmZkYy1kODE1LTRiYzItOTExMy1mYjk4YzBkNTExYjciLCJydGkiOiIyMGQyNmZkYy1kODE1LTRiYzItOTExMy1mYjk4YzBkNTExYjciLCJzdWIiOiI2MWZhYTkyYi1iNDY5LTQwMmItOGE4My05NzFlMTg4YzQ5YjIiLCJnaXZlbl9uYW1lIjoiRXlvYiIsImZhbWlseV9uYW1lIjoiU3RyYXRlZ2lzdCIsInVuYW1lIjoiZXlvYnMiLCJyb2xlIjoiU3RyYXRlZ2lzdCIsImNzIjoiW1wiS1VNRURcIixcIkNIU1wiLFwiR0hTXCIsXCJDMDAyXCIsXCJDMDAxXCJdIiwiY2NpIjoiIiwidXNlIjoiUmVmcmVzaCIsIm5iZiI6MTU1NDI0NjIyMSwiZXhwIjoxNTU0MjYwNjIxLCJpYXQiOjE1NTQyNDYyMjEsImlzcyI6Imh0dHA6Ly9wYy5vbW5pY2VsbC5jb20iLCJhdWQiOiJodHRwOi8vcGMub21uaWNlbGwuY29tIn0.fHSEQOgN5dFqXTetzFG_CMoqmj-bvvo6Z6lginiD-dOm0Fe8RGdee0wJGzGXsD88iTjpX5VuwBlf0F4DrecRXQ"));
        intervalEditText.setText(String.valueOf(sharedPref.getInt("interval",5)));
        CheckBox checkBoxAllowGPS = (CheckBox) findViewById(R.id.allowGPS);
        checkBoxAllowGPS.setChecked(sharedPref.getBoolean("allowGPS",false));
    }

    private void updatePreferences(String familyName, String find3Api, String workflowApi, String token, int interval, String deviceName, boolean allowGPS, String locationName) {
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("familyName", familyName);
        editor.putString("deviceName", deviceName);
        editor.putString("find3Api", find3Api);
        editor.putString("workflowApi", workflowApi);
        editor.putString("token", token);
        editor.putInt("interval", interval);
        editor.putString("locationName", locationName);
        editor.putBoolean("allowGPS",allowGPS);
        editor.commit();
    }

    public static int getInteger(String number, int defaultValue){
        int value = defaultValue;
        try{
            value = Integer.parseInt(number);
        }catch(Exception e ){
        }
        return value;
    }

    private void connectWebSocket() {
        URI uri;
        try {
            String serverAddress = find3ApiEdit.getText().toString();
            String familyName = ((EditText) findViewById(R.id.familyName)).getText().toString();
            String deviceName = ((EditText) findViewById(R.id.deviceName)).getText().toString();
            serverAddress = serverAddress.replace("http", "ws");
            uri = new URI(serverAddress + "/ws?family=" + familyName + "&device=" + deviceName);
            Log.d("Websocket", "connect to websocket at " + uri.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                mWebSocketClient.send("Hello");
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Websocket", "message: " + message);
                        JSONObject json = null;
                        JSONObject fingerprint = null;
                        JSONObject sensors = null;
                        JSONArray guesses = null;
                        JSONObject bluetooth = null;
                        JSONObject wifi = null;
                        String deviceName = "";
                        String locationName = "";
                        String familyName = "";
                        try {
                            json = new JSONObject(message);
                        } catch (Exception e) {
                            Log.d("Websocket", "json error: " + e.toString());
                            return;
                        }
                        try {
                            fingerprint = new JSONObject(json.get("sensors").toString());
                            Log.d("Websocket", "fingerprint: " + fingerprint);
                        } catch (Exception e) {
                            Log.d("Websocket", "json error: " + e.toString());
                        }

                        try {
                            guesses = (JSONArray) json.get("guesses");
                            JSONObject topGuess = new JSONObject(guesses.get(0).toString());

                            String currentLocation = topGuess.get("location").toString();
                            double locationProbability = Double.parseDouble(topGuess.get("probability").toString());

                            String location = "You are in: " + currentLocation + " ("+Math.round(locationProbability*100)+"%)";
                            currentLocationTextView.setText(location);

                            Log.d("Websocket", "guesses: " + guesses);
                        } catch (Exception e) {
                            Log.d("Websocket", "json error: " + e.toString());
                        }

                        try {
                            sensors = new JSONObject(fingerprint.get("s").toString());
                            deviceName = fingerprint.get("d").toString();
                            familyName = fingerprint.get("f").toString();
                            locationName = fingerprint.get("l").toString();
                            Log.d("Websocket", "sensors: " + sensors);
                        } catch (Exception e) {
                            Log.d("Websocket", "json error: " + e.toString());
                        }
                        try {
                            wifi = new JSONObject(sensors.get("wifi").toString());
                            Log.d("Websocket", "wifi: " + wifi);
                        } catch (Exception e) {
                            Log.d("Websocket", "json error: " + e.toString());
                        }
                        try {
                            bluetooth = new JSONObject(sensors.get("bluetooth").toString());
                            Log.d("Websocket", "bluetooth: " + bluetooth);
                        } catch (Exception e) {
                            Log.d("Websocket", "json error: " + e.toString());
                        }
                        Log.d("Websocket", bluetooth.toString());
                        Integer bluetoothPoints = bluetooth.length();
                        Integer wifiPoints = wifi.length();
                        Long secondsAgo = null;
                        try {
                            secondsAgo = fingerprint.getLong("t");
                        } catch (Exception e) {
                            Log.w("Websocket", e);
                        }

                        if ((System.currentTimeMillis() - secondsAgo)/1000 > 3) {
                            return;
                        }
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd HH:mm:ss");
                        Date resultdate = new Date(secondsAgo);
//                        String message = sdf.format(resultdate) + ": " + bluetoothPoints.toString() + " bluetooth and " + wifiPoints.toString() + " wifi points inserted for " + familyName + "/" + deviceName;
                        String message = "1 second ago: added " + bluetoothPoints.toString() + " bluetooth and " + wifiPoints.toString() + " wifi points for " + familyName + "/" + deviceName;
                        oneSecondTimer.resetCounter();
                        if (locationName.equals("") == false) {
                            message += " at " + locationName;
                        }
                        TextView rssi_msg = (TextView) findViewById(R.id.textOutput);
                        Log.d("Websocket", message);
                        rssi_msg.setText(message);

                    }
                });
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView rssi_msg = (TextView) findViewById(R.id.textOutput);
                        rssi_msg.setText("cannot connect to server, fingerprints will not be uploaded");
                    }
                });
            }
        };
        mWebSocketClient.connect();
    }
}