package com.example;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class HealthCheck extends Thread {
    private String primaryServer;
    private String backupServer;
    private ZContext context;
    private ZMQ.Socket socket;
    private volatile boolean useBackup;

    public HealthCheck(String primaryServer, String backupServer) {
        this.primaryServer = primaryServer;
        this.backupServer = backupServer;
        this.context = new ZContext();
        this.socket = context.createSocket(ZMQ.REQ);
        this.useBackup = false;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String server = useBackup ? backupServer : primaryServer;
                socket.connect(server);
                socket.send("health_check");
                String response = socket.recvStr(ZMQ.DONTWAIT);
                if (response == null) {
                    // Primary server is down, switch to backup
                    System.out.println("Primary server down, switching to backup");
                    useBackup = true;
                    socket.disconnect(primaryServer);
                    socket.connect(backupServer);
                } else {
                    // Primary server is up
                    System.out.println("Primary server is up");
                    useBackup = false;
                }
                Thread.sleep(5000); // Check every 5 seconds
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isUsingBackup() {
        return useBackup;
    }

    public String getPrimaryServer() {
        return primaryServer;
    }

    public void setPrimaryServer(String primaryServer) {
        this.primaryServer = primaryServer;
    }

    public String getBackupServer() {
        return backupServer;
    }

    public void setBackupServer(String backupServer) {
        this.backupServer = backupServer;
    }

    public void close() {
        socket.close();
        context.close();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: HealthCheck <primaryServer> <backupServer>");
            return;
        }
        String primaryServer = args[0];
        String backupServer = args[1];

        HealthCheck healthCheck = new HealthCheck(primaryServer, backupServer);
        healthCheck.start();
    }
}