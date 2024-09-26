package com.example;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class HealthCheck extends Thread {
    private String primaryServer;
    private String backupServer;
    private ZContext context;
    private ZMQ.Socket socket;

    public HealthCheck(String primaryServer, String backupServer) {
        this.primaryServer = primaryServer;
        this.backupServer = backupServer;
        this.context = new ZContext();
        this.socket = context.createSocket(ZMQ.REQ);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                socket.connect(primaryServer);
                socket.send("health_check");
                String response = socket.recvStr(ZMQ.DONTWAIT);
                if (response == null) {
                    // Primary server is down, switch to backup
                    System.out.println("Primary server down, switching to backup");
                    socket.disconnect(primaryServer);
                    socket.connect(backupServer);
                }
                Thread.sleep(5000); // Check every 5 seconds
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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