package com.radio.models;

import java.time.Duration;

public class Cancion {
    private String titulo;
    private String artista;
    private String rutaArchivo;
    private Duration duracion;
    private Genero genero;

    public Cancion() {}

    public Cancion(String titulo, String artista, String rutaArchivo, Duration duracion, Genero genero) {
        this.titulo = titulo;
        this.artista = artista;
        this.rutaArchivo = rutaArchivo;
        this.duracion = duracion;
        this.genero = genero;
    }

    // Getters y Setters
    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getArtista() {
        return artista;
    }

    public void setArtista(String artista) {
        this.artista = artista;
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

    public Genero getGenero() {
        return genero;
    }

    public void setGenero(Genero genero) {
        this.genero = genero;
    }

    @Override
    public String toString() {
        return String.format("%s - %s (%s)", artista, titulo, genero.getNombre());
    }
}