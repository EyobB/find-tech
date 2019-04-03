package com.internalpositioning.find3.find3app;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Broadcaster {
    private static List<SendListener> listeners = new ArrayList<SendListener>();

    public void addListener(SendListener toAdd) {
        listeners.add(toAdd);
    }

    public void Sent(int wifiLength) {
        // Notify everybody that may be interested.
        Date date = new Date();
        for (SendListener hl : listeners)
            hl.Sent(date, wifiLength);
    }

    public void Acked(){
        Date date = new Date();
        for (SendListener hl : listeners)
            hl.Acked(date);
    }

    public void ScanStarted(){
        Date date = new Date();
        for (SendListener hl : listeners)
            hl.ScanStarted(date);
    }
}