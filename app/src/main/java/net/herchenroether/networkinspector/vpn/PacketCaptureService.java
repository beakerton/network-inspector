package net.herchenroether.networkinspector.vpn;

import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import net.herchenroether.networkinspector.utils.Logger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Captures all packets on the system.
 *
 * Created by Adam Herchenroether on 9/7/2016.
 */
public class PacketCaptureService extends VpnService {
    private static String VPN_INTENT_EXTRA = "VpnExtra";
    private static String VPN_START_SERVICE = "Start";
    private static String VPN_STOP_SERVICE = "Stop";

    private ExecutorService mExecutor;
    private ParcelFileDescriptor mInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String command = intent.getStringExtra(VPN_INTENT_EXTRA);
        if (command.equals(VPN_START_SERVICE)) {
            Logger.info("starting VPN service");
            initializeVPN();
            mExecutor = Executors.newFixedThreadPool(1);
            mExecutor.submit(new VpnRunnable());
        } else if (command.equals(VPN_STOP_SERVICE)) {
            Logger.info("stopping VPN service");
            closeVPN();
        }

        return START_STICKY;
    }

    private void initializeVPN() {
        if (mInterface == null) {
            mInterface = new Builder().addAddress("10.0.0.2", 32)
                    .addRoute("0.0.0.0", 0)
                    .setSession("NetworkInspector")
                    .establish();
            if (mInterface == null) {
                Logger.error("Failed to establish VPN connection");
            }
        }
    }

    @Override
    public void onDestroy() {
        closeVPN();
    }

    private void closeVPN() {
        mExecutor.shutdownNow();
        try {
            if (mInterface != null) {
                mInterface.close();
                mInterface = null;
                Logger.info("Closed VPN file descriptor");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        stopSelf();
    }

    /**
     * Sends an intent to start the VPN service
     *
     * @param context
     */
    public static void startVpnService(Context context) {
        final Intent intent = new Intent(context, PacketCaptureService.class);
        intent.putExtra(VPN_INTENT_EXTRA, VPN_START_SERVICE);
        context.startService(intent);
    }

    /**
     * Sends an intent to stop the VPN service
     *
     * @param context
     */
    public static void stopVpnService(Context context) {
        final Intent intent = new Intent(context, PacketCaptureService.class);
        intent.putExtra(VPN_INTENT_EXTRA, VPN_STOP_SERVICE);
        context.startService(intent);
    }

    private class VpnRunnable implements Runnable {

        @Override
        public void run() {
            Logger.info("starting VPN runnable");
            try {
                while (!Thread.interrupted()) {
                    // Packets to be sent are queued in this input stream.
                    //FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());
                    // Packets received need to be written to this output stream.
                    //FileOutputStream out = new FileOutputStream(mInterface.getFileDescriptor());

                    // Allocate the buffer for a single packet.
                    //ByteBuffer packet = ByteBuffer.allocate(32767);

                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Logger.info("Interrupted");
            /*} catch (IOException e) {
                Logger.error("IOException");
                e.printStackTrace();*/
            } finally {
                closeVPN();
            }
        }
    }
}
