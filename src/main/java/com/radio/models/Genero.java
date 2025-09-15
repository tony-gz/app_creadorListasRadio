package com.radio.models;

public enum Genero {
    INFANTILES("Infantiles"),
    CUMBIAS("Cumbias"),
    BALADAS("Baladas"),
    ROCK("Rock"),
    POP("Pop"),
    SALSA("Salsa"),
    REGIONAL("Regional"),
    CLASICA("Clásica"),
    JAZZ("Jazz"),
    ELECTRONICA("Electrónica"),
    RANCHERA("Ranchera"),
    BOLEROS("Boleros"),
    VARIADO("Variado");

    private final String nombre;

    Genero(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}