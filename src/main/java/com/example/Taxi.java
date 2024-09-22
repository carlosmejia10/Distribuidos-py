package com.example;

import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import com.google.gson.Gson;
import java.util.Random;

public class Taxi {
    private int taxiId;
    private int n, m;
    private int[] posicion;
    private int velocidad;
    private int serviciosDiarios;
    private ZContext context;
    private ZMQ.Socket socket;
    private Gson gson;

    public Taxi(int taxiId, int n, int m, int[] posicionInicial, int velocidad, int serviciosDiarios) {
        this.taxiId = taxiId;
        this.n = n;
        this.m = m;
        this.posicion = posicionInicial;
        this.velocidad = velocidad;
        this.serviciosDiarios = serviciosDiarios;
        this.context = new ZContext();
        this.socket = context.createSocket(ZMQ.REQ);
        this.socket.connect("tcp://192.168.10.24:5555");
        this.gson = new Gson();
    }

    private void mover() {
        Random random = new Random();
        try {
            while (serviciosDiarios > 0 && !Thread.currentThread().isInterrupted()) {
                if (velocidad > 0) {
                    String direccion = random.nextBoolean() ? "N" : "S";
                    if (direccion.equals("N") && posicion[1] > 0) {
                        posicion[1]--;
                    } else if (direccion.equals("S") && posicion[1] < m - 1) {
                        posicion[1]++;
                    } else if (direccion.equals("E") && posicion[0] < n - 1) {
                        posicion[0]++;
                    } else if (direccion.equals("O") && posicion[0] > 0) {
                        posicion[0]--;
                    }
                    socket.send(gson.toJson(new Mensaje("actualizacion", taxiId, posicion)));
                    socket.recvStr();
                    System.out.println("Taxi ID=" + taxiId + " se movió a la posición " + posicion[0] + "," + posicion[1]);
                }
                Thread.sleep(30000 / velocidad);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            socket.close();
            context.close();
        }
    }

    public void run() {
        try {
            socket.send(gson.toJson(new Mensaje("registro", taxiId, posicion)));
            socket.recvStr();
            System.out.println("Taxi ID=" + taxiId + " registrado en la posición " + posicion[0] + "," + posicion[1]);
            mover();
        } finally {
            socket.close();
            context.close();
        }
    }

    public static void main(String[] args) {
        int[] posicionInicial = {0, 0};
        Taxi taxi = new Taxi(1, 1000, 1000, posicionInicial, 4, 3);
        taxi.run();
    }
}