package com.example;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;

public class Usuario extends Thread {
    private int usuarioId;
    private int[] posicion;
    private int tiempoEspera;
    private ZContext context;
    private ZMQ.Socket socket;
    private Gson gson;

    public Usuario(int usuarioId, int[] posicion, int tiempoEspera) {
        this.usuarioId = usuarioId;
        this.posicion = posicion;
        this.tiempoEspera = tiempoEspera;
        this.context = new ZContext();
        this.socket = context.createSocket(ZMQ.DEALER);
        this.socket.setIdentity(String.valueOf(usuarioId).getBytes(ZMQ.CHARSET));
        this.socket.connect("tcp://servidor-central:5555");
        this.gson = new Gson();
    }

    @Override
    public void run() {
        try {
            // Esperar un tiempo antes de enviar la solicitud
            Thread.sleep(tiempoEspera * 1000);

            Mensaje solicitud = new Mensaje("solicitud", usuarioId, posicion);
            String mensajeJson = gson.toJson(solicitud);
            socket.send(mensajeJson);
            System.out.println("Usuario ID=" + usuarioId + " enviando solicitud: " + mensajeJson);

            byte[] reply = socket.recv(0);
            String respuesta = new String(reply, ZMQ.CHARSET);
            System.out.println("Usuario ID=" + usuarioId + " recibió respuesta: " + respuesta);

            Respuesta resp = gson.fromJson(respuesta, Respuesta.class);
            if (resp.status.equals("ok")) {
                System.out.println("Usuario " + usuarioId + " asignado al taxi " + resp.taxiId);
            } else {
                System.out.println("Usuario " + usuarioId + " no pudo ser asignado a un taxi");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("Error en Usuario ID=" + usuarioId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            socket.close();
            context.close();
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Uso: Usuario <usuarioId> <posX> <posY> <tiempoEspera>");
            return;
        }
        int usuarioId = Integer.parseInt(args[0]);
        int[] posicion = {Integer.parseInt(args[1]), Integer.parseInt(args[2])};
        int tiempoEspera = Integer.parseInt(args[3]);

        Usuario usuario = new Usuario(usuarioId, posicion, tiempoEspera);
        usuario.start();
    }
}