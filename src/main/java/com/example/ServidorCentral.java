package com.example;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class ServidorCentral {

    private Map<Integer, TaxiInfo> taxis = new HashMap<>();
    private ZContext context;
    private ZMQ.Socket socket;
    private Gson gson;

    public ServidorCentral() {
        this.context = new ZContext();
        this.socket = context.createSocket(ZMQ.ROUTER);
        boolean bound = this.socket.bind("tcp://0.0.0.0:5555");
        System.out.println("Socket bound: " + bound);
        this.gson = new Gson();
    }

    public void run() {
        System.out.println("Servidor Central iniciado en 0.0.0.0:5555. Esperando conexiones...");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                System.out.println("Esperando mensaje...");
                String identity = socket.recvStr();
                System.out.println("Identidad recibida: " + identity);
                
                String mensaje = socket.recvStr();
                System.out.println("Mensaje recibido de " + identity + ": " + mensaje);

                if (mensaje == null || mensaje.isEmpty()) {
                    System.out.println("Mensaje vacío recibido de " + identity + ". Ignorando.");
                    continue;
                }

                try {
                    Mensaje msg = gson.fromJson(mensaje, Mensaje.class);
                    if (msg == null) {
                        System.out.println("No se pudo parsear el mensaje JSON de " + identity + ". Mensaje: " + mensaje);
                    } else {
                        procesarMensaje(identity, msg);
                    }
                } catch (JsonSyntaxException e) {
                    System.out.println("Error al procesar el mensaje JSON de " + identity + ": " + e.getMessage());
                    System.out.println("Mensaje recibido (no JSON): " + mensaje);
                }
            } catch (Exception e) {
                System.out.println("Error inesperado en el Servidor Central: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void procesarMensaje(String identity, Mensaje msg) {
        if (msg == null || msg.tipo == null) {
            System.out.println("Mensaje nulo o sin tipo recibido de " + identity);
            return;
        }
        System.out.println("Procesando mensaje de tipo: " + msg.tipo + " de " + identity);
        switch (msg.tipo) {
            case "registro":
                taxis.put(msg.id, new TaxiInfo(msg.posicion, true, identity));
                System.out.println("Nuevo taxi registrado - ID=" + msg.id + " en la posición " + msg.posicion[0] + "," + msg.posicion[1]);
                enviarRespuestaOk(identity);
                break;
            case "solicitud":
                System.out.println("Nueva solicitud de servicio - Usuario ID=" + msg.id + " en la posición " + msg.posicion[0] + "," + msg.posicion[1]);
                Respuesta respuesta = asignarTaxi(msg);
                if (respuesta.status.equals("ok")) {
                    TaxiInfo taxi = taxis.get(respuesta.taxiId);
                    enviarAsignacion(taxi.getIdentity(), msg, identity);
                } else {
                    System.out.println("No se pudo asignar un taxi al Usuario ID=" + msg.id);
                }
                break;
            case "actualizacion":
                actualizarPosicionTaxi(msg);
                break;
            default:
                System.out.println("Mensaje de tipo desconocido: " + msg.tipo);
                break;
        }
    }

    private void enviarRespuestaOk(String identity) {
        socket.sendMore(identity);
        socket.send("ok");
        System.out.println("Respuesta 'ok' enviada a " + identity);
    }

    private Respuesta asignarTaxi(Mensaje msg) {
        int usuarioX = msg.posicion[0];
        int usuarioY = msg.posicion[1];
        int taxiIdAsignado = -1;
        double distanciaMinima = Double.MAX_VALUE;
        for (Map.Entry<Integer, TaxiInfo> entry : taxis.entrySet()) {
            TaxiInfo taxi = entry.getValue();
            if (taxi.isDisponible()) {
                int taxiX = taxi.getPosicion()[0];
                int taxiY = taxi.getPosicion()[1];
                double distancia = Math.sqrt(Math.pow(taxiX - usuarioX, 2) + Math.pow(taxiY - usuarioY, 2));
                if (distancia < distanciaMinima || (distancia == distanciaMinima && entry.getKey() < taxiIdAsignado)) {
                    distanciaMinima = distancia;
                    taxiIdAsignado = entry.getKey();
                }
            }
        }
        if (taxiIdAsignado != -1) {
            taxis.get(taxiIdAsignado).setDisponible(false);
            String info = "Servicio asignado: Taxi ID=" + taxiIdAsignado + " a Usuario ID=" + msg.id;
            registrarInformacion(info);
            System.out.println(info);
            return new Respuesta("ok", taxiIdAsignado);
        } else {
            String info = "Servicio no disponible para Usuario ID=" + msg.id;
            registrarInformacion(info);
            System.out.println(info);
            return new Respuesta("no disponible", -1);
        }
    }

    private void enviarAsignacion(String taxiIdentity, Mensaje msg, String usuarioIdentity) {
        Mensaje asignacion = new Mensaje("servicio", msg.id, msg.posicion);
        String mensajeJson = gson.toJson(asignacion);

        // Enviar asignación al taxi
        socket.sendMore(taxiIdentity);
        socket.send(mensajeJson);
        System.out.println("Asignación enviada al taxi " + taxiIdentity + ": " + mensajeJson);

        // Enviar asignación al usuario
        Respuesta respuesta = new Respuesta("ok", msg.id);
        String respuestaJson = gson.toJson(respuesta);
        socket.sendMore(usuarioIdentity);
        socket.send(respuestaJson);
        System.out.println("Asignación enviada al usuario " + usuarioIdentity + ": " + taxiIdentity);
    }

    private void actualizarPosicionTaxi(Mensaje msg) {
        TaxiInfo taxi = taxis.get(msg.id);
        if (taxi != null) {
            taxi.setPosicion(msg.posicion);
            taxi.setDisponible(true);
            String info = "Actualización de posición: Taxi ID=" + msg.id + " a posición " + msg.posicion[0] + "," + msg.posicion[1];
            registrarInformacion(info);
            System.out.println(info);
        }
    }

    private void registrarInformacion(String info) {
        try (FileWriter writer = new FileWriter("registro_taxis.txt", true)) {
            writer.write(info + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ServidorCentral servidor = new ServidorCentral();
        servidor.run();
    }
}