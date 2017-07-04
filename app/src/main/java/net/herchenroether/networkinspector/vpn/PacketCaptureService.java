package net.herchenroether.networkinspector.vpn;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.RequiresApi;
import android.util.Log;

import net.herchenroether.networkinspector.utils.Logger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eu.faircode.netguard.Allowed;
import eu.faircode.netguard.Packet;
import eu.faircode.netguard.ResourceRecord;
import eu.faircode.netguard.Usage;

/**
 * Captures all packets on the system.
 *
 * Created by Adam Herchenroether on 9/7/2016.
 */
public class PacketCaptureService extends VpnService {
    // Used to load the 'networkreader' library on service startup.
    static {
        System.loadLibrary("networkreader");
    }

    private static String VPN_INTENT_EXTRA = "VpnExtra";
    private static String VPN_START_SERVICE = "Start";
    private static String VPN_STOP_SERVICE = "Stop";

    private ParcelFileDescriptor mInterface;

    private native void jni_init();

    private native void jni_start(int tun, boolean fwd53, int rcode, int loglevel);

    private native void jni_stop(int tun, boolean clr);

    private native int jni_get_mtu();

    private native void jni_socks5(String addr, int port, String username, String password);

    private native void jni_done();

    @Override
    public void onCreate() {
        jni_init();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String command = intent.getStringExtra(VPN_INTENT_EXTRA);
        if (command.equals(VPN_START_SERVICE)) {
            Logger.info("starting VPN service");
            initializeVPN();
        } else if (command.equals(VPN_STOP_SERVICE)) {
            Logger.info("stopping VPN service");
            closeVPN();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        closeVPN();
        jni_done();
        super.onDestroy();
    }

    @Override
    public void onRevoke() {
        closeVPN();
        jni_done();
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initializeVPN() {
        if (mInterface == null) {
            try {
                mInterface = new Builder().addAddress("10.0.0.2", 32)
                        .addRoute("0.0.0.0", 0)
                        .setSession("NetworkInspector")
                        .addAllowedApplication("com.amazon.avod.thirdpartyclient")
                        .establish();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (mInterface == null) {
                Logger.error("Failed to establish VPN connection");
            }

            jni_socks5("", 0, "", "");
            jni_start(mInterface.getFd(), true, 3, Log.ERROR);
        }
    }

    private void closeVPN() {
        try {
            if (mInterface != null) {
                jni_stop(mInterface.getFd(), true);
                mInterface.close();
                mInterface = null;
                Logger.info("Closed VPN file descriptor");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable ex) {
            // File descriptor might be closed
            Logger.error("file descriptor is closed");
            jni_stop(-1, true);
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

    // Called from JNI layer
    private void nativeExit(String reason) {
        Logger.warn("Native exit reason = " + reason);
    }

    // Called from JNI layer
    private void nativeError(int error, String message) {
        Logger.error(String.format(Locale.getDefault(), "Native error %d, message = ", error, message));
    }

    // Called from JNI layer
    private void logPacket(Packet packet) {
        Logger.info(packet.toString() + packet.data);
        // no-op
    }

    // Called from JNI layer
    private void dnsResolved(ResourceRecord rr) {
        // no-op
    }

    // Called from JNI layer
    private boolean isDomainBlocked(String name) {
        return false;
    }

    // Called from JNI layer
    private Allowed isAddressAllowed(Packet packet) {
        return new Allowed();
    }

    // Called from JNI layer
    private void accountUsage(Usage usage) {
        // no-op
    }
}
