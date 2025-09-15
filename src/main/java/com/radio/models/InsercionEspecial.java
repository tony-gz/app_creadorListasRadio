package com.radio.models;

import java.time.Duration;

public class InsercionEspecial {

    public enum TipoInsercion {
        HIMNO_NACIONAL("Himno Nacional"),
        HIMNO_GUERRERO("Himno de Guerrero"),
        POEMA("Poema"),
        LOCUCION_HORA("Locución de Hora"),
        IDENTIFICACION("Identificación"),
        PROMO("Promo");

        private final String nombre;

        TipoInsercion(String nombre) {
            this.nombre = nombre;
        }

        public String getNombre() {
            return nombre;
        }
    }

    private String nombre;
    private String rutaArchivo;
    private Duration duracion;
    private TipoInsercion tipo;

    public InsercionEspecial() {}

    public InsercionEspecial(String nombre, String rutaArchivo, Duration duracion, TipoInsercion tipo) {
        this.nombre = nombre;
        this.rutaArchivo = rutaArchivo;
        this.duracion = duracion;
        this.tipo = tipo;
    }

    // Getters y Setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public Duration getDuracion() {
        return duracion;
    }

    public void setDuracion(Duration duracion) {
        this.duracion = duracion;
    }

    public TipoInsercion getTipo() {
        return tipo;
    }

    public void setTipo(TipoInsercion tipo) {
        this.tipo = tipo;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", nombre, tipo.getNombre());
    }
}