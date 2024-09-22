package com.example;

import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

public class ServidorCentral {
    private int n, m;
    private Map<Integer, TaxiInfo> taxis;
    private ZContext context;
    private ZMQ.Socket socket;
    private Gson gson;

    public ServidorCentral(int n, int m) {
        this.n = n;
        this.m = m;
        this.taxis = new HashMap<>();
        this.context = new ZContext();
        this.socket = context.createSocket(ZMQ.REP);
        this.socket.bind("tcp://*:5555");
        this.gson = new Gson();
    }

    private void registrarTaxi(int taxiId, int[] posicion) {
        taxis.put(taxiId, new TaxiInfo(posicion, true));
        System.out.println("Taxi registrado: ID=" + taxiId + ", Posición=" + posicion[0] + "," + posicion[1]);
    }

    private void actualizarPosicionTaxi(int taxiId, int[] nuevaPosicion) {
        if (taxis.containsKey(taxiId)) {
            taxis.get(taxiId).setPosicion(nuevaPosicion);
            System.out.println("Posición actualizada: Taxi ID=" + taxiId + ", Nueva Posición=" + nuevaPosicion[0] + "," + nuevaPosicion[1]);
        }
    }

    private int asignarTaxi(int[] posicionUsuario) {
        int taxiMasCercano = -1;
        int distanciaMinima = Integer.MAX_VALUE;
        for (Map.Entry<Integer, TaxiInfo> entry : taxis.entrySet()) {
            if (entry.getValue().isDisponible()) {
                int distancia = Math.abs(entry.getValue().getPosicion()[0] - posicionUsuario[0]) +
                                Math.abs(entry.getValue().getPosicion()[1] - posicionUsuario[1]);
                if (distancia < distanciaMinima) {
                    distanciaMinima = distancia;
                    taxiMasCercano = entry.getKey();
                }
            }
        }
        return taxiMasCercano;
    }

    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String mensaje = socket.recvStr();
                Mensaje msg = gson.fromJson(mensaje, Mensaje.class);
                switch (msg.tipo) {
                    case "registro":
                        registrarTaxi(msg.id, msg.posicion);
                        break;
                    case "actualizacion":
                        actualizarPosicionTaxi(msg.id, msg.posicion);
                        break;
                    case "solicitud":
                        int taxiId = asignarTaxi(msg.posicion);
                        if (taxiId != -1) {
                            taxis.get(taxiId).setDisponible(false);
                            System.out.println("Taxi asignado: Usuario Posición=" + msg.posicion[0] + "," + msg.posicion[1] + " -> Taxi ID=" + taxiId);
                            socket.send(gson.toJson(new Respuesta("ok", taxiId)));
                        } else {
                            System.out.println("No hay taxis disponibles para la posición del usuario: " + msg.posicion[0] + "," + msg.posicion[1]);
                            socket.send(gson.toJson(new Respuesta("no disponible", -1)));
                        }
                        break;
                }
            }
        } finally {
            socket.close();
            context.close();
        }
    }

    public static void main(String[] args) {
        ServidorCentral servidor = new ServidorCentral(1000, 1000);
        servidor.run();
    }
}

class TaxiInfo {
    private int[] posicion;
    private boolean disponible;

    public TaxiInfo(int[] posicion, boolean disponible) {
        this.posicion = posicion;
        this.disponible = disponible;
    }

    public int[] getPosicion() {
        return posicion;
    }

    public void setPosicion(int[] posicion) {
        this.posicion = posicion;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }
}