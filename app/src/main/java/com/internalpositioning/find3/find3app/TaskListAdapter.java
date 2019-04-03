package com.internalpositioning.find3.find3app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class TaskListAdapter extends ArrayAdapter<TasksAPI.Task> {
    private final Context context;
    private List<TasksAPI.Task> tasks;

    public List<TasksAPI.Task> getTasks(){
        return this.tasks;
    }
    public boolean setTasks(List<TasksAPI.Task> tasks){
        this.tasks.clear();
        this.tasks.addAll(tasks);
        return true;
    }

    public TaskListAdapter(Context context, List<TasksAPI.Task> tasks) {
        super(context, -1, tasks);
        this.context = context;
        this.tasks = tasks;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.row, parent, false);
        final TasksAPI.Task task = tasks.get(position);

        TextView nameView = (TextView) rowView.findViewById(R.id.nameText);
        TextView descriptionView = (TextView) rowView.findViewById(R.id.descriptionText);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        final Button doneButton = (Button)rowView.findViewById(R.id.doneButton);

        nameView.setText(task.getName());
        descriptionView.setText(task.getDescription());
        imageView.setImageResource(R.drawable.omnicelllogo);
        doneButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                String workflowApi = preferences.getString("workflowApi", null);
                String token = preferences.getString("token", null);

                DeleteTaskAPI api = new DeleteTaskAPI();
                api.execute(workflowApi, token, task.getId().toString());

                doneButton.setEnabled(false);
            }
        });

        return rowView;
    }
}