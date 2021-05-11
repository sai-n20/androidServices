package com.example.project5client;

import android.os.RemoteException;
import android.view.View;

import java.io.IOException;

public interface RVClickListener {

    public void onClick(View view, int position) throws IOException, InterruptedException, RemoteException;
}
