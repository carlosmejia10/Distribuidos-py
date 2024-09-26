package com.example;

public class TaxiInfo {
    private int[] posicion;
    private boolean disponible;
    private String identity;

    public TaxiInfo(int[] posicion, boolean disponible, String identity) {
        this.posicion = posicion;
        this.disponible = disponible;
        this.identity = identity;
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

    public String getIdentity() {
        return identity;
    }
}