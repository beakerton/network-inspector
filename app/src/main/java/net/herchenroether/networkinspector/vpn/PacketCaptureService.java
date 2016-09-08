package net.herchenroether.networkinspector.vpn;

import android.content.Intent;
import android.net.VpnService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Captures all packets on the system.
 *
 * Created by Adam Herchenroether on 9/7/2016.
 */
public class PacketCaptureService extends VpnService {
    private ExecutorService mExecutor;

    @Override
    public void onCreate() {
        mExecutor = Executors.newFixedThreadPool(1);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mExecutor.shutdownNow();
    }
}
