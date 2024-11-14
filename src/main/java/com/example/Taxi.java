package com.example;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;

public class Taxi extends Thread {
    private int taxiId;
    private int n;
    private int m;
    private int[] posicion;
    private int[] posicionInicial;
    private int velocidad;
    private int serviciosDiarios;
    private int serviciosRealizados;
    private ZContext context;
    private ZMQ.Socket socket;
    private Gson gson;

    public Taxi(int taxiId, int n, int m, int[] posicionInicial, int velocidad, int serviciosDiarios) {
        this.taxiId = taxiId;
        this.n = n;
        this.m = m;
        this.posicion = posicionInicial.clone();
        this.posicionInicial = posicionInicial.clone();
        this.velocidad = velocidad;
        this.serviciosDiarios = serviciosDiarios;
        this.serviciosRealizados = 0;
        this.context = new ZContext();
        this.socket = context.createSocket(ZMQ.DEALER);
        this.socket.setIdentity(String.valueOf(taxiId).getBytes(ZMQ.CHARSET));
        this.socket.connect("tcp://servidor-central:5555");
        this.gson = new Gson();
    }

    @Override
    public void run() {
        try {
            // Registrar el taxi en el servidor central
            Mensaje registro = new Mensaje("registro", taxiId, posicion);
            String mensajeJson = gson.toJson(registro);
            socket.send(mensajeJson);
            System.out.println("Taxi ID=" + taxiId + " enviando registro: " + mensajeJson);

            // Esperar respuesta de registro
            byte[] reply = socket.recv(0);
            String respuesta = new String(reply, ZMQ.CHARSET);
            System.out.println("Respuesta recibida: " + respuesta);

            if (respuesta.equals("ok")) {
                System.out.println("Taxi ID=" + taxiId + " registrado exitosamente en la posición " + posicion[0] + "," + posicion[1]);

                // Bucle principal para esperar mensajes del servidor y enviar la posición periódicamente
                while (serviciosRealizados < serviciosDiarios) {
                    // Enviar la posición actual al servidor
                    actualizarPosicion();

                    // Esperar mensaje del servidor con un timeout
                    String mensaje = socket.recvStr(ZMQ.DONTWAIT);
                    if (mensaje != null) {
                        System.out.println("Mensaje recibido: " + mensaje);
                        Mensaje msg = gson.fromJson(mensaje, Mensaje.class);
                        if (msg.tipo.equals("servicio")) {
                            realizarServicio();
                        }
                    }

                    // Esperar un tiempo antes de enviar la próxima actualización de posición
                    Thread.sleep(velocidad * 1000);
                }
                System.out.println("Taxi ID=" + taxiId + " ha completado todos los servicios del día.");
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

    private void actualizarPosicion() {
        // Actualizar la posición del taxi (ejemplo simple: mover en una dirección aleatoria)
        posicion[0] = (posicion[0] + 1) % n;
        posicion[1] = (posicion[1] + 1) % m;

        // Enviar la nueva posición al servidor central
        Mensaje actualizacion = new Mensaje("actualizacion", taxiId, posicion);
        String mensajeJson = gson.toJson(actualizacion);
        socket.send(mensajeJson, 0);  // Enviar sin esperar respuesta
        System.out.println("Taxi ID=" + taxiId + " nueva posición: " + mensajeJson);
    }

    public void realizarServicio() {
        try {
            // Desaparecer de la matriz por 30 segundos
            System.out.println("Taxi ID=" + taxiId + " realizando servicio. Desapareciendo por 30 segundos.");
            Thread.sleep(30000);

            // Incrementar el contador de servicios realizados
            serviciosRealizados++;

            if (serviciosRealizados < serviciosDiarios) {
                // Volver a la posición inicial
                posicion = posicionInicial.clone();
                Mensaje actualizacion = new Mensaje("actualizacion", taxiId, posicion);
                String mensajeJson = gson.toJson(actualizacion);
                socket.send(mensajeJson, 0);  // Enviar sin esperar respuesta
                System.out.println("Taxi ID=" + taxiId + " volvió a la posición inicial: " + mensajeJson);
            } else {
                System.out.println("Taxi ID=" + taxiId + " ha completado todos los servicios del día.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("Error en Taxi ID=" + taxiId + " durante el servicio: " + e.getMessage());
            e.printStackTrace();
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
        taxi.start();
    }
}