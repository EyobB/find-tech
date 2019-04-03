package com.internalpositioning.find3.find3app;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.UUID;

import org.json.*;

import com.loopj.android.http.*;

import cz.msebera.android.httpclient.Header;

public class TasksAPI extends AsyncTask<String, Void, TasksAPI.Task[]> {
    private static final String TAG = "TasksAPI";
    public Response delegate = null;

    @Override
    protected TasksAPI.Task[] doInBackground(String... args) {
        final String url = args[0];
        final String token = args[1];

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
            conn.setRequestMethod("GET");
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

            InputStream in = new BufferedInputStream(conn.getInputStream());
            String response = convertStreamToString(in);
            Log.i(TAG, "TasksApi response: " + response);

            JSONArray responseJsonArray = new JSONArray(response);
            TasksAPI.Task[] tasks = new TasksAPI.Task[responseJsonArray.length()];
            for (int i = 0; i < responseJsonArray.length(); i++) {
                JSONObject jsonobject = null;
                try {
                    jsonobject = responseJsonArray.getJSONObject(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Gson gson = new Gson();
                TasksAPI.Task task = gson.fromJson(jsonobject.toString(), TasksAPI.Task.class);
                tasks[i] = task;
            }

            return tasks;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        Handler mainHandler = new Handler(Looper.getMainLooper());
//        Runnable myRunnable = new Runnable() {
//            @Override
//            public void run() {
//                HttpUtils.get(url, token, new JsonHttpResponseHandler() {
//                    @Override
//                    public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject j) {
//                        Log.e(TAG, "Status: " + statusCode + " EX: " + t.toString());
//                    }
//
//                    @Override
//                    public void onSuccess(int statusCode, Header[] headers, JSONArray jsonarray) {
//                        Log.e(TAG, "Complete: " + statusCode);
////                        if (statusCode != 200) {
////                            return;
////                        }
////
////                        Task[] tasks = new Task[jsonarray.length()];
////                        for (int i = 0; i < jsonarray.length(); i++) {
////                            JSONObject jsonobject = null;
////                            try {
////                                jsonobject = jsonarray.getJSONObject(i);
////                            } catch (JSONException e) {
////                                e.printStackTrace();
////                            }
////                            Gson gson = new Gson();
////                            Task task = gson.fromJson(jsonobject.toString(), TasksAPI.Task.class);
////                            tasks[i] = task;
////                        }
////
////                        delegate.processFinished(tasks);
//                    }
//                });
//            }
//        };
//        mainHandler.post(myRunnable);
//
        return new TasksAPI.Task[0];
    }

    protected void onPostExecute(TasksAPI.Task[] tasks) {
        delegate.processFinished(tasks);
    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }

    public static class Task {
        private UUID id;
        private String omni;
        private String name;
        private String description;
        private String assignedTo;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getOmni() {
            return omni;
        }

        public void setOmni(String omni) {
            this.omni = omni;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAssignedTo() {
            return assignedTo;
        }

        public void setAssignedTo(String assignedTo) {
            this.assignedTo = assignedTo;
        }
    }

    public interface Response {
        void processFinished(Task[] tasks);
    }
}

