package com.internalpositioning.find3.find3app;

import java.util.Date;

public interface SendListener{
    void Sent(Date date, int wifiLength);
    void Acked(Date date);
    void ScanStarted(Date date);
    void TaskListReceived(boolean hasNewTasks);
}