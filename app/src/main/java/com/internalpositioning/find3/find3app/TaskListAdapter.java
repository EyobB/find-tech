package com.internalpositioning.find3.find3app;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        TasksAPI.Task task = tasks.get(position);

        TextView nameView = (TextView) rowView.findViewById(R.id.nameText);
        TextView descriptionView = (TextView) rowView.findViewById(R.id.descriptionText);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);

        nameView.setText(task.getName());
        descriptionView.setText(task.getDescription());

        imageView.setImageResource(R.drawable.omnicelllogo);

        return rowView;
    }
}