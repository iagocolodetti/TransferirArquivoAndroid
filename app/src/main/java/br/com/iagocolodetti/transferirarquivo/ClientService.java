package br.com.iagocolodetti.transferirarquivo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import java.util.ArrayList;

import br.com.iagocolodetti.transferirarquivo.exception.ClientConnectException;
import br.com.iagocolodetti.transferirarquivo.exception.SendFileException;

public class ClientService extends Service {

    IBinder mBinder = new LocalBinder();

    private final int CHANNEL_ID = 98765;

    private ClientServiceCallbacks clientServiceCallbacks = null;

    private Client client = null;
    private String ip, dir;
    private int port;

    private NotificationManager notificationManager = null;
    private Notification.Builder notificationBuilder = null;

    public void connect() throws ClientConnectException {
        client.connect(ip, port, dir);
    }

    public void disconnect() {
        client.disconnect();
    }

    public void sendFiles(ArrayList<MyFile> myFiles) throws SendFileException {
        client.sendFiles(myFiles);
    }

    public boolean sendingFiles() {
        return client.sendingFiles();
    }

    private Notification.Builder getNotificationBuilder(String channelId) {
        Notification.Builder builder;
        notificationManager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT < 26) {
            builder = new Notification.Builder(this)
                    .setAutoCancel(false)
                    .setContentIntent(null)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_waiting_connection))
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.mipmap.ic_launcher_round);
        } else {
            NotificationChannel channel = new NotificationChannel(channelId, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
            builder = new Notification.Builder(this, channelId)
                    .setAutoCancel(false)
                    .setContentIntent(null)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_waiting_connection))
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.mipmap.ic_launcher_round);
        }
        return builder;
    }

    public void setNotificationContentText(String text) {
        notificationBuilder.setContentText(text);
        notificationManager.notify(CHANNEL_ID, notificationBuilder.build());
    }

    public void showMessage(final String message) {
        new Handler(getMainLooper()).post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    // <editor-fold defaultstate="collapsed" desc="ClientServiceCallbacks">
    public void setMainActivityCallbacks(ClientServiceCallbacks clientServiceCallbacks) {
        this.clientServiceCallbacks = clientServiceCallbacks;
    }

    public void addAreaLog(String message) {
        clientServiceCallbacks.addAreaLog(message);
    }

    public void connecting() {
        setNotificationContentText(getString(R.string.notification_connecting, ip, port));
        clientServiceCallbacks.connecting();
    }

    public void connected() {
        setNotificationContentText(getString(R.string.notification_connected_to, ip, port));
        clientServiceCallbacks.connected();
    }

    public void disconnected() {
        setNotificationContentText(getString(R.string.notification_waiting_connection));
        clientServiceCallbacks.disconnected();
    }

    public void setStatus(final String status) {
        clientServiceCallbacks.setStatus(status);
    }

    public void setProgressStatus(int value) {
        clientServiceCallbacks.setProgressStatus(value);
    }
    // </editor-fold>

    public class LocalBinder extends Binder {
        public ClientService getServerInstance() {
            return ClientService.this;
        }
    }

    public void onCreate() {
        super.onCreate();
        client = new Client(this);
        notificationBuilder = getNotificationBuilder(String.valueOf(CHANNEL_ID));
        startForeground(CHANNEL_ID, notificationBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ip = intent.getStringExtra("ip");
        port = intent.getIntExtra("port", -1);
        dir = intent.getStringExtra("dir");
        return START_NOT_STICKY;
    }

    public void onDestroy() {
        super.onDestroy();
        notificationManager.cancel(CHANNEL_ID);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
