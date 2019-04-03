package com.internalpositioning.find3.find3app;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.UUID;

public class DeleteTaskAPI extends AsyncTask<String, Void, UUID> {
    private static final String TAG = "DeleteTaskAPI";

    @Override
    protected UUID doInBackground(String... args) {
        String url = args[0];
        final String token = args[1];
        final UUID id = UUID.fromString(args[2]);

        url += (url.endsWith("/")?"":"/") + id;
        // Create URL
        URL tasksEndpoint = null;
        try {
            tasksEndpoint = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Create connection
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) tasksEndpoint.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", token);

        try {
            conn.setRequestMethod("DELETE");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
        Log.w(TAG, url);
        Log.w(TAG, token);
        try {

            int code = conn.getResponseCode();
            Log.i(TAG, "TasksApi response status: " + code);

            if (code != 200) {
                Log.e(TAG, "TasksApi call failed. Url: " + url + " Status code: " + code);
                return null;
            }

            return id;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}