package com.example;

import java.util.Random;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;

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
        this.socket = context.createSocket(ZMQ.DEALER);
        this.socket.setIdentity(String.valueOf(taxiId).getBytes(ZMQ.CHARSET));
        boolean connected = this.socket.connect("tcp://192.168.0.10:5555");
        System.out.println("Socket connected: " + connected);
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
                    Mensaje mensajeActualizacion = new Mensaje("actualizacion", taxiId, posicion);
                    String mensajeJson = gson.toJson(mensajeActualizacion);
                    socket.send(mensajeJson);
                    System.out.println("Taxi ID=" + taxiId + " se movió a la posición " + posicion[0] + "," + posicion[1]);
                    System.out.println("Mensaje enviado: " + mensajeJson);
                    String respuesta = socket.recvStr();
                    System.out.println("Respuesta recibida: " + respuesta);
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
            System.out.println("Taxi ID=" + taxiId + " intentando conectar a 192.168.0.6:5555");
            Mensaje mensajeRegistro = new Mensaje("registro", taxiId, posicion);
            String mensajeJson = gson.toJson(mensajeRegistro);
            socket.send(mensajeJson);
            System.out.println("Taxi ID=" + taxiId + " enviando mensaje de registro desde la posición " + posicion[0] + "," + posicion[1]);
            System.out.println("Mensaje de registro enviado: " + mensajeJson);

            // Esperar respuesta
            byte[] reply = socket.recv(0);  // 0 significa que esperará indefinidamente
            String respuesta = new String(reply, ZMQ.CHARSET);
            
            System.out.println("Respuesta recibida: " + respuesta);
            if (respuesta.equals("ok")) {
                System.out.println("Taxi ID=" + taxiId + " registrado exitosamente en la posición " + posicion[0] + "," + posicion[1]);
                mover();
            } else {
                System.out.println("Error al registrar el Taxi ID=" + taxiId + ": " + respuesta);
            }
        } catch (Exception e) {
            System.out.println("Error en Taxi ID=" + taxiId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            socket.close();
            context.close();
        }
    }

    public static void main(String[] args) {
        if (args.length < 7) {
            System.out.println("Uso: Taxi <taxiId> <n> <m> <posX> <posY> <velocidad> <serviciosDiarios>");
            return;
        }
        int taxiId = Integer.parseInt(args[0]);
        int n = Integer.parseInt(args[1]);
        int m = Integer.parseInt(args[2]);
        int[] posicionInicial = {Integer.parseInt(args[3]), Integer.parseInt(args[4])};
        int velocidad = Integer.parseInt(args[5]);
        int serviciosDiarios = Integer.parseInt(args[6]);

        Taxi taxi = new Taxi(taxiId, n, m, posicionInicial, velocidad, serviciosDiarios);
        taxi.run();
    }
}
