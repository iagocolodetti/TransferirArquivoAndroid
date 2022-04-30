/*
 * Copyright (C) 2019 Iago Colodetti
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package br.com.iagocolodetti.transferirarquivo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.regex.Pattern;

import br.com.iagocolodetti.transferirarquivo.exception.ClientConnectException;
import br.com.iagocolodetti.transferirarquivo.exception.SendFileException;

/**
 *
 * @author iagocolodetti
 */
public class Client {

    private final int MIN_PORT = 0;
    private final int MAX_PORT = 65535;

    private ClientService clientService;

    private Socket socket = null;

    private volatile boolean connected = false;

    private boolean receivingFile = false;
    private Thread sendingFile = null;

    private final int CONNECT_TIMEOUT = 5000;
    private final int MAX_RECONNECT_TRIES = 5;
    private final int WAIT_TO_RECONNECT = 500;
    private final int WAIT_AFTER_SEND = 2000;
    private final int WAIT_AFTER_SEND_LOOP = 10;

    public Client(ClientService clientService) {
        this.clientService = clientService;
    }

    public void connect(String ip, int port, String dir) throws ClientConnectException {
        if (ip.isEmpty()) {
            throw new ClientConnectException(clientService.getString(R.string.ip_not_defined_error));
        }
        if (!Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$").matcher(ip).matches()) {
            throw new ClientConnectException(clientService.getString(R.string.invalid_ip_error));
        }
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new ClientConnectException(clientService.getString(R.string.port_limit_error, MIN_PORT, MAX_PORT));
        }
        if (!new File(dir).exists() && !new File(dir).mkdir()) {
            throw new ClientConnectException(clientService.getString(R.string.dir_error));
        }
        clientService.connecting();
        Thread t = new Thread(new ClientConnect(ip, port, dir));
        t.start();
    }

    public void disconnect() {
        try {
            connected = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            socket = null;
            clientService.disconnected();
        }
    }

    public void sendFiles(ArrayList<MyFile> myFiles) throws SendFileException {
        if (!isSocketConnected()) {
            throw new SendFileException(clientService.getString(R.string.not_connected_to_send_error));
        }
        if (myFiles == null || myFiles.isEmpty()) {
            throw new SendFileException(clientService.getString(R.string.file_not_found_send_error));
        }
        if (receivingFile || sendingFiles()) {
            throw new SendFileException(clientService.getString(R.string.already_sending_error));
        }
        sendingFile = new Thread(new SendFiles(myFiles));
        sendingFile.start();
    }

    public boolean sendingFiles() {
        return (sendingFile != null && sendingFile.isAlive());
    }

    private boolean isSocketConnected() {
        return (socket != null && !socket.isClosed() && socket.isConnected());
    }

    private void threadSleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class ClientConnect implements Runnable {

        private final String ip;
        private final int port;
        private final String dir;

        public ClientConnect(String ip, int port, String dir) {
            connected = true;
            this.ip = ip;
            this.port = port;
            this.dir = dir;
        }

        @Override
        public void run() {
            while (connected) {
                receivingFile = false;
                if (!isSocketConnected()) {
                    threadSleep(socket == null ? 0 : WAIT_TO_RECONNECT);
                    int tries = 0;
                    while (connected && tries < MAX_RECONNECT_TRIES) {
                        try {
                            socket = null;
                            socket = new Socket();
                            socket.connect(new InetSocketAddress(ip, port), CONNECT_TIMEOUT);
                            clientService.connected();
                            break;
                        } catch (IllegalArgumentException ex) {
                            clientService.showMessage(clientService.getString(R.string.incorrect_ip_port_error));
                            disconnect();
                            ex.printStackTrace();
                            break;
                        } catch (IOException ex) {
                            tries++;
                            ex.printStackTrace();
                        }
                    }
                    if (tries == MAX_RECONNECT_TRIES) {
                        clientService.showMessage(clientService.getString(R.string.not_possible_connect_error));
                        disconnect();
                    }
                }
                if (connected) {
                    InputStream is = null;
                    DataInputStream dis = null;
                    OutputStream out = null;
                    try {
                        is = socket.getInputStream();
                        dis = new DataInputStream(is);
                        String[] data = dis.readUTF().split(Pattern.quote("|"));
                        receivingFile = true;
                        threadSleep(1000);
                        String newFileName = data[0];
                        for (int i = 1; new File(dir + newFileName).exists(); i++) {
                            newFileName = data[0].substring(0, data[0].lastIndexOf(".")) + "(" + i + ")" + data[0].substring(data[0].lastIndexOf("."));
                        }
                        long size = Long.parseLong(data[1]);
                        long sizekb = (data[1].equals("0") ? 1 : size / 1024);
                        sizekb = (sizekb == 0 ? 1 : sizekb);
                        is = socket.getInputStream();
                        out = new FileOutputStream(dir + newFileName);
                        clientService.setNotificationContentText(clientService.getString(R.string.notification_receiving_file, newFileName));
                        clientService.setStatus(clientService.getString(R.string.status_receiving_file));
                        clientService.addAreaLog(clientService.getString(R.string.receiving_file, newFileName));
                        byte[] buffer = new byte[1024];
                        long receivedBytes = 0;
                        int count;
                        long kb = 0;
                        int progress;
                        while ((count = is.read(buffer)) > 0) {
                            out.write(buffer, 0, count);
                            receivedBytes += count;
                            progress = (int) (++kb * 100 / sizekb);
                            clientService.setProgressStatus(progress < 100 ? progress : 100);
                        }
                        if (receivedBytes == size) {
                            clientService.addAreaLog(clientService.getString(R.string.file_received, newFileName));
                        } else {
                            clientService.addAreaLog(clientService.getString(R.string.received_file_corrupted, newFileName));
                            clientService.showMessage(clientService.getString(R.string.lost_connection_error));
                            disconnect();
                        }
                    } catch (IOException ex) {
                        if (connected && !sendingFiles()) {
                            clientService.showMessage(clientService.getString(R.string.lost_connection_error));
                            disconnect();
                            ex.printStackTrace();
                        }
                    } finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                            if (is != null) {
                                is.close();
                            }
                            if (dis != null) {
                                dis.close();
                            }
                            if (socket != null) {
                                socket.close();
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } finally {
                            clientService.setProgressStatus(0);
                            clientService.setStatus("");
                        }
                    }
                }
            }
        }
    }

    private class SendFiles implements Runnable {

        private final ArrayList<MyFile> myFiles;

        public SendFiles(ArrayList<MyFile> myFiles) {
            this.myFiles = myFiles;
        }

        @Override
        public void run() {
            for (MyFile myFile : myFiles) {
                int loop = 0;
                while (!isSocketConnected() && loop < WAIT_AFTER_SEND_LOOP) {
                    loop++;
                    threadSleep(WAIT_AFTER_SEND);
                }
                if (loop == WAIT_AFTER_SEND_LOOP) {
                    clientService.showMessage(clientService.getString(R.string.not_possible_send_error));
                    break;
                }
                InputStream is = null;
                DataOutputStream dos = null;
                OutputStream out = null;
                try {
                    is = myFile.getInputStream();
                    dos = new DataOutputStream(socket.getOutputStream());
                    dos.writeUTF(myFile.getName() + "|" + myFile.getSize());
                    threadSleep(1000);
                    out = socket.getOutputStream();
                    clientService.setNotificationContentText(clientService.getString(R.string.notification_sending_file, myFile.getName()));
                    clientService.setStatus(clientService.getString(R.string.status_sending_file));
                    clientService.addAreaLog(clientService.getString(R.string.sending_file, myFile.getName()));
                    byte[] buffer = new byte[1024];
                    long sendedBytes = 0;
                    int count;
                    long sizekb = (myFile.getSize() / 1024);
                    sizekb = (sizekb == 0 ? 1 : sizekb);
                    long kb = 0;
                    int progress;
                    while ((count = is.read(buffer)) > 0) {
                        out.write(buffer, 0, count);
                        sendedBytes += count;
                        progress = (int) (++kb * 100 / sizekb);
                        clientService.setProgressStatus(progress < 100 ? progress : 100);
                    }
                    if (sendedBytes == myFile.getSize()) {
                        clientService.addAreaLog(clientService.getString(R.string.file_sended, myFile.getName()));
                    } else {
                        clientService.addAreaLog(clientService.getString(R.string.sended_file_corrupted, myFile.getName()));
                    }
                } catch (FileNotFoundException ex) {
                    clientService.addAreaLog(clientService.getString(R.string.file_not_found, myFile.getName()));
                } catch (IOException ex) {
                    if (connected) {
                        clientService.showMessage(clientService.getString(R.string.lost_connection_error));
                        disconnect();
                        ex.printStackTrace();
                    }
                } finally {
                    try {
                        if (is != null) {
                            if (out != null) {
                                out.close();
                            }
                            is.close();
                            if (dos != null) {
                                dos.close();
                            }
                            if (socket != null) {
                                socket.close();
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } finally {
                        clientService.setStatus("");
                        clientService.setProgressStatus(0);
                    }
                }
                threadSleep(WAIT_AFTER_SEND);
            }
        }
    }
}
