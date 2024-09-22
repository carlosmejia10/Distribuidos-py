package com.example;


public class Mensaje {
    String tipo;
    int id;
    int[] posicion;

    public Mensaje() {
        // Constructor por defecto
    }

    public Mensaje(String tipo, int id, int[] posicion) {
        this.tipo = tipo;
        this.id = id;
        this.posicion = posicion;
    }

    // Getters y setters si es necesario
}