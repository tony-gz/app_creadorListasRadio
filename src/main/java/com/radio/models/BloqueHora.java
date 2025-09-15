package com.radio.models;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BloqueHora {
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Genero genero;
    private String rutaCarpeta;
    private List<Cancion> canciones;
    private List<InsercionEspecial> inserciones;
    private int toleranciaMinutos; // Tolerancia de ±5 minutos

    public BloqueHora() {
        this.canciones = new ArrayList<>();
        this.inserciones = new ArrayList<>();
        this.toleranciaMinutos = 5; // Por defecto 5 minutos de tolerancia
    }

    public BloqueHora(LocalTime horaInicio, LocalTime horaFin, Genero genero, String rutaCarpeta) {
        this();
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.genero = genero;
        this.rutaCarpeta = rutaCarpeta;
    }

    // Getters y Setters
    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public Genero getGenero() {
        return genero;
    }

    public void setGenero(Genero genero) {
        this.genero = genero;
    }

    public String getRutaCarpeta() {
        return rutaCarpeta;
    }

    public void setRutaCarpeta(String rutaCarpeta) {
        this.rutaCarpeta = rutaCarpeta;
    }

    public List<Cancion> getCanciones() {
        return canciones;
    }

    public void setCanciones(List<Cancion> canciones) {
        this.canciones = canciones;
    }

    public void agregarCancion(Cancion cancion) {
        this.canciones.add(cancion);
    }

    public List<InsercionEspecial> getInserciones() {
        return inserciones;
    }

    public void setInserciones(List<InsercionEspecial> inserciones) {
        this.inserciones = inserciones;
    }

    public void agregarInsercion(InsercionEspecial insercion) {
        this.inserciones.add(insercion);
    }

    public int getToleranciaMinutos() {
        return toleranciaMinutos;
    }

    public void setToleranciaMinutos(int toleranciaMinutos) {
        this.toleranciaMinutos = toleranciaMinutos;
    }

    @Override
    public String toString() {
        return String.format("%s - %s: %s (%d canciones)",
                horaInicio, horaFin, genero.getNombre(), canciones.size());
    }
}