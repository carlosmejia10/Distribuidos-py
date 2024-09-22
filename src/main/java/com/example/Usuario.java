package com.example;

import org.zeromq.ZMQ;
import org.zeromq.ZContext;
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
        this.socket = context.createSocket(ZMQ.REQ);
        this.socket.connect("tcp://192.168.10.24:5555");
        this.gson = new Gson();
    }

    @Override
    public void run() {
        try {
            Thread.sleep(tiempoEspera * 1000);
            socket.send(gson.toJson(new Mensaje("solicitud", usuarioId, posicion)));
            String respuesta = socket.recvStr();
            Respuesta resp = gson.fromJson(respuesta, Respuesta.class);
            if (resp.status.equals("ok")) {
                System.out.println("Usuario " + usuarioId + " asignado al taxi " + resp.taxiId);
            } else {
                System.out.println("Usuario " + usuarioId + " no pudo ser asignado a un taxi");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            socket.close();
            context.close();
        }
    }

    public static void main(String[] args) {
        int[] posicion = {0, 0};
        Usuario usuario = new Usuario(1, posicion, 5);
        usuario.start();
    }
}